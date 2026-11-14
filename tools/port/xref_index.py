#!/usr/bin/env python3
"""Rough symbol index + integration cross-reference checks for the 1.21.1 port (review phase).

Regex based (no real Java parser). Checks:
  1. imports of project classes that do not exist (removed/renamed classes)
  2. @ExpectPlatform methods in common  <->  static methods in neoforge `<pkg>.forge.<Class>Impl` (name, parameter types, static)
  3. mixin json entries <-> mixin classes; @Shadow/@Accessor/@Invoker/@Inject method names against the vanilla 1.21.1 source tree
     (pass --vanilla <dir> = extracted_minecraft_data client sources)
  4. Class.member(...) calls on project classes whose simple name is unique: method name + arity must exist in that class or a
     project superclass (only reported when the whole superclass chain is project code, otherwise it is "maybe inherited")
Usage: python3 tools/port/xref_index.py [--vanilla /path/to/client] [--json index.json]
"""
import os, re, sys, json, collections

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = {"common": "common/src/main/java", "neoforge": "neoforge/src/main/java"}
PKG = "com.github.teamfossilsarcheology.fossil"

def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), s, flags=re.S)
    s = re.sub(r"//[^\n]*", "", s)
    return s

def strip_strings(s):
    return re.sub(r'"(?:\\.|[^"\\])*"', '""', s)

def split_params(p):
    p = p.strip()
    if not p:
        return []
    out, depth, cur = [], 0, ""
    for ch in p:
        if ch in "<(": depth += 1
        if ch in ">)": depth -= 1
        if ch == "," and depth == 0:
            out.append(cur); cur = ""
        else:
            cur += ch
    out.append(cur)
    return [x.strip() for x in out]

def param_type(p):
    p = re.sub(r"@\w+(\.\w+)*(\([^)]*\))?\s*", "", p)  # annotations
    p = re.sub(r"\bfinal\s+", "", p).strip()
    t = p.rsplit(None, 1)[0] if " " in p else p
    t = re.sub(r"\s+", "", t)
    # erasure-ish: drop generics and qualifiers (Item.Properties -> Properties); good enough for descriptor matching
    while "<" in t:
        t2 = re.sub(r"<[^<>]*>", "", t)
        if t2 == t:
            break
        t = t2
    t = t.split(".")[-1] if "..." not in t else t.replace("...", "[]").split(".")[-1]
    return t

METHOD_RE = re.compile(r"(?P<mods>(?:(?:public|protected|private|static|final|abstract|synchronized|native|default)\s+)*)"
                       r"(?:<[^>]+>\s+)?(?P<ret>[\w.$<>\[\]?,\s]+?)\s+(?P<name>\w+)\s*\((?P<params>[^()]*(?:\([^()]*\)[^()]*)*)\)\s*(?:throws[^{;]+)?[{;]")
CLASS_RE = re.compile(r"\b(class|interface|enum|record)\s+(\w+)(\s*<[^{]*?>)?(?:\s*\([^)]*\))?\s*(?:extends\s+([\w.<>,?\s]+?))?\s*(?:implements\s+([\w.<>,?\s]+?))?\s*\{")
KEYWORDS = {"if", "for", "while", "switch", "catch", "return", "new", "else", "synchronized", "try", "throw", "super", "this"}

class Cls:
    def __init__(self, fq, path, module):
        self.fq, self.path, self.module = fq, path, module
        self.simple = fq.rsplit(".", 1)[-1]
        self.methods = collections.defaultdict(list)  # name -> [(arity, static, [types], annotations, line)]
        self.fields = set()
        self.extends = None
        self.implements = []
        self.imports = {}

def index():
    classes = {}
    for module, rel in SRC.items():
        base = os.path.join(ROOT, rel)
        for d, _, fs in os.walk(base):
            for f in fs:
                if not f.endswith(".java"):
                    continue
                path = os.path.join(d, f)
                raw = open(path, errors="replace").read()
                src = strip_strings(strip_comments(raw))
                pkg = re.search(r"^\s*package\s+([\w.]+)\s*;", src, re.M)
                pkg = pkg.group(1) if pkg else ""
                imports = {}
                for m in re.finditer(r"^\s*import\s+(static\s+)?([\w.]+)(\.\*)?\s*;", src, re.M):
                    if not m.group(1) and not m.group(3):
                        imports[m.group(2).rsplit(".", 1)[-1]] = m.group(2)
                top = f[:-5]
                c = Cls(pkg + "." + top, path, module)
                c.imports = imports
                cm = CLASS_RE.search(src)
                if cm:
                    c.extends = cm.group(4).strip() if cm.group(4) else None
                    c.implements = [x.strip() for x in split_params(cm.group(5))] if cm.group(5) else []
                    if cm.group(1) == "record" and cm.group(2) == top:
                        rp = re.search(r"record\s+\w+(?:<[^>]*>)?\s*\(([^)]*)\)", src)
                        if rp:
                            for p in split_params(rp.group(1)):
                                n = p.split()[-1] if p.split() else ""
                                c.methods[n].append((0, False, [], "", 0))
                                c.fields.add(n)
                lines = src.split("\n")
                for m in METHOD_RE.finditer(src):
                    name = m.group("name")
                    if name in KEYWORDS or m.group("ret").strip() in KEYWORDS | {"else"}:
                        continue
                    ret = m.group("ret").strip()
                    if not ret.split() or ret.split()[-1] in KEYWORDS:
                        continue
                    params = split_params(m.group("params"))
                    if any("=" in p or not re.search(r"[\w>\]]\s+\w", p.replace("...", " ")) for p in params):
                        continue
                    line = src.count("\n", 0, m.start()) + 1
                    pre = src[max(0, m.start("name") - 400):m.start("name")]
                    ann = " ".join(re.findall(r"@\w+", re.split(r"[;{}]", pre)[-1]))
                    c.methods[name].append((len(params), "static" in (m.group("mods") + " " + ret).split(), [param_type(p) for p in params], ann, line))
                for m in re.finditer(r"(?:public|protected|private|static|final)\s+(?:static\s+|final\s+)*[\w.<>,?\[\]\s]+?\s+(\w+)\s*(?:=|;)", src):
                    c.fields.add(m.group(1))
                classes[c.fq] = c
    return classes

def check_imports(classes):
    issues = []
    known = set(classes)
    pkgs = {c.fq.rsplit(".", 1)[0] for c in classes.values()}
    for c in classes.values():
        for simple, fq in c.imports.items():
            if not fq.startswith(PKG):
                continue
            if fq in known:
                continue
            # nested class import: Outer.Inner
            outer = fq.rsplit(".", 1)[0]
            if outer in known:
                continue
            issues.append(f"{os.path.relpath(c.path, ROOT)}: import of missing project class {fq}")
    return issues

def check_expect_platform(classes):
    issues = []
    n = 0
    for c in classes.values():
        if c.module != "common":
            continue
        for name, ovs in c.methods.items():
            for (ar, st, types, ann, line) in ovs:
                if "@ExpectPlatform" not in ann:
                    continue
                n += 1
                pkg, simple = c.fq.rsplit(".", 1)
                impl_fq = f"{pkg}.forge.{simple}Impl"
                impl = classes.get(impl_fq)
                where = f"{os.path.relpath(c.path, ROOT)}:{line} {simple}.{name}({', '.join(types)})"
                if not impl:
                    issues.append(f"{where}: no Impl class {impl_fq}")
                    continue
                cands = impl.methods.get(name, [])
                if not any(t == types and s for (a, s, t, _, _) in cands):
                    issues.append(f"{where}: Impl {impl_fq} has {[(t, 'static' if s else 'instance') for (a, s, t, _, _) in cands]}")
    # Impl classes without @ExpectPlatform counterpart
    for c in classes.values():
        if c.module == "neoforge" and c.simple.endswith("Impl") and ".forge." in c.fq:
            base = c.fq.replace(".forge.", ".", 1)[:-4]
            # strip only the last ".forge" segment
            parts = c.fq.rsplit(".forge.", 1)
            base = parts[0] + "." + parts[1][:-4]
            if base not in classes:
                issues.append(f"{os.path.relpath(c.path, ROOT)}: Impl without common class {base}")
                continue
            for name, ovs in c.methods.items():
                for (ar, st, types, ann, line) in ovs:
                    if not st:
                        continue
                    cm = classes[base].methods.get(name, [])
                    if not any("@ExpectPlatform" in a2 and t2 == types for (_, _, t2, a2, _) in cm):
                        if any(t2 == types for (_, _, t2, _, _) in cm):
                            continue
                        issues.append(f"{os.path.relpath(c.path, ROOT)}:{line}: static {name}({', '.join(types)}) has no @ExpectPlatform counterpart in {base} (helper?)")
    issues.insert(0, f"(checked {n} @ExpectPlatform methods)")
    return issues

ENUM_IMPLICIT = {"values": 0, "valueOf": 1, "ordinal": 0, "name": 0}

def check_mixins(classes, vanilla):
    issues = []
    for js in [os.path.join(ROOT, "common/src/main/resources/fossil_common.mixins.json"),
               os.path.join(ROOT, "neoforge/src/main/resources/fossil_forge.mixins.json")]:
        if not os.path.exists(js):
            issues.append(f"missing mixin config {js}")
            continue
        cfg = json.load(open(js))
        pkg = cfg.get("package")
        names = set()
        for k in ("mixins", "client", "server"):
            for n in cfg.get(k, []) or []:
                names.add(n)
                if f"{pkg}.{n}" not in classes:
                    issues.append(f"{os.path.relpath(js, ROOT)}: entry {k}/{n} -> class {pkg}.{n} not found")
        mixdir_classes = [c for c in classes.values() if c.fq.startswith(pkg + ".")]
        for c in mixdir_classes:
            raw = open(c.path, errors="replace").read()
            if "@Mixin" in raw:
                rel = c.fq[len(pkg) + 1:]
                if rel not in names:
                    issues.append(f"{os.path.relpath(c.path, ROOT)}: @Mixin class not listed in {os.path.basename(js)}")
        if cfg.get("plugin") and cfg["plugin"] not in classes:
            issues.append(f"{os.path.basename(js)}: plugin {cfg['plugin']} not found")
    # @Shadow / @Accessor / @Invoker against vanilla
    if not vanilla:
        return issues
    for c in classes.values():
        raw = open(c.path, errors="replace").read()
        m = re.search(r"@Mixin\s*\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class", raw)
        if not m:
            continue
        tgt_simple = m.group(1).split(".")[-1]
        tgt_fq = c.imports.get(m.group(1).split(".")[0])
        if not tgt_fq or not tgt_fq.startswith("net.minecraft"):
            continue
        outer = tgt_fq
        inner = m.group(1).split(".")[1:]
        vpath = os.path.join(vanilla, outer.replace(".", "/") + ".java")
        if not os.path.exists(vpath):
            issues.append(f"{os.path.relpath(c.path, ROOT)}: mixin target {tgt_fq} not in vanilla 1.21.1 sources")
            continue
        vsrc = open(vpath, errors="replace").read()
        for sm in re.finditer(r"@Shadow[^;{]*?\s(\w+)\s*(\(|;|=)", raw):
            nm = sm.group(1)
            if not re.search(r"\b%s\b" % re.escape(nm), vsrc):
                issues.append(f"{os.path.relpath(c.path, ROOT)}: @Shadow {nm} not found in {tgt_fq}")
        for am in re.finditer(r"@(Accessor|Invoker)\s*(?:\(\s*(?:value\s*=\s*)?\"(\w+)\"[^)]*\))?\s*(?:@\w+\s*)*[\w.<>\[\]?, ]+?\s(\w+)\s*\(", raw):
            kind, explicit, meth = am.groups()
            target = explicit
            if not target:
                mm = re.match(r"(get|set|is|call|invoke)(\w+)", meth)
                target = mm.group(2)[0].lower() + mm.group(2)[1:] if mm else meth
            if not re.search(r"\b%s\b" % re.escape(target), vsrc):
                issues.append(f"{os.path.relpath(c.path, ROOT)}: @{kind} {meth} -> '{target}' not found in {tgt_fq}")
        for im in re.finditer(r"method\s*=\s*\{?\s*\"([\w$<>]+)", raw):
            nm = im.group(1)
            if nm in ("<init>", "<clinit>"):
                continue
            if not re.search(r"\b%s\b" % re.escape(nm), vsrc):
                issues.append(f"{os.path.relpath(c.path, ROOT)}: injector method '{nm}' not found in {tgt_fq}")
    return issues

def resolve_super(c, classes):
    if not c.extends:
        return None
    n = re.sub(r"<.*", "", c.extends).strip()
    fq = c.imports.get(n.split(".")[0])
    if not fq:
        same = c.fq.rsplit(".", 1)[0] + "." + n
        if same in classes:
            return classes[same]
        return "external"
    return classes.get(fq, "external")

def has_member(c, name, arity, classes, depth=0):
    """True / False / None(unknown, external supertype)"""
    if depth > 15:
        return None
    for (a, s, t, _, _) in c.methods.get(name, []):
        if a == arity or (t and t[-1].endswith("...")):
            return True
    sup = resolve_super(c, classes)
    if sup is None:
        unknown = any(not classes.get(c.imports.get(re.sub(r"<.*", "", i).strip(), "")) for i in c.implements)
        return None if (unknown or c.implements) else False
    if sup == "external":
        return None
    return has_member(sup, name, arity, classes, depth + 1)

def check_static_calls(classes):
    by_simple = collections.defaultdict(list)
    for c in classes.values():
        by_simple[c.simple].append(c)
    issues = []
    call = re.compile(r"\b([A-Z]\w+)\.([a-z]\w*)\s*\(")
    for c in classes.values():
        src = strip_strings(strip_comments(open(c.path, errors="replace").read()))
        for m in call.finditer(src):
            cls, meth = m.groups()
            fq = c.imports.get(cls) or (c.fq.rsplit(".", 1)[0] + "." + cls)
            tgt = classes.get(fq)
            if not tgt:
                continue
            # arity
            i, depth, args, cur = m.end(), 1, 0, ""
            nonempty = False
            while i < len(src) and depth:
                ch = src[i]
                if ch in "([{": depth += 1
                elif ch in ")]}": depth -= 1
                elif ch == "," and depth == 1: args += 1
                if depth and not ch.isspace(): nonempty = True
                i += 1
            arity = args + 1 if nonempty else 0
            if ENUM_IMPLICIT.get(meth) == arity and re.search(r"\benum\s+" + tgt.simple + r"\b", open(tgt.path, errors="replace").read()):
                continue
            r = has_member(tgt, meth, arity, classes)
            if r is False:
                if meth in tgt.fields:
                    continue
                if any(meth in cc.methods for cc in classes.values() if cc.fq.startswith(tgt.fq + ".")):
                    continue
                line = src.count("\n", 0, m.start()) + 1
                have = sorted({(a) for (a, *_r) in tgt.methods.get(meth, [])})
                issues.append(f"{os.path.relpath(c.path, ROOT)}:{line}: {cls}.{meth}/{arity} not declared in {tgt.fq}" + (f" (arities {have})" if have else ""))
    return issues

def main():
    vanilla = None
    if "--vanilla" in sys.argv:
        vanilla = sys.argv[sys.argv.index("--vanilla") + 1]
    classes = index()
    print(f"# indexed {len(classes)} classes, {sum(len(v) for c in classes.values() for v in c.methods.values())} method decls")
    for title, issues in [("imports of missing project classes", check_imports(classes)),
                          ("@ExpectPlatform <-> Impl", check_expect_platform(classes)),
                          ("mixins", check_mixins(classes, vanilla)),
                          ("Class.method(...) calls on project classes", check_static_calls(classes))]:
        print(f"\n## {title}: {len(issues)}")
        for i in issues:
            print("  " + i)
    if "--json" in sys.argv:
        out = sys.argv[sys.argv.index("--json") + 1]
        json.dump({fq: {"module": c.module, "extends": c.extends,
                        "methods": {n: [(a, s, t) for (a, s, t, _, _) in v] for n, v in c.methods.items()}}
                   for fq, c in classes.items()}, open(out, "w"), indent=1)

if __name__ == "__main__":
    main()
