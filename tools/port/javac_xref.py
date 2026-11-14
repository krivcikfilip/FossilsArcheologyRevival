#!/usr/bin/env python3
"""Integration cross-reference check (review phase, no Gradle/Maven available).

Runs `javac` attribution (no bytecode needed) over ALL project sources with the reference SOURCES of the dependencies
on -sourcepath (decompiled vanilla 1.21.1, NeoForge, Architectury, GeckoLib, TerraBlender, JEI/REI/EMI/CarryOn/FD/Jade,
More Hitboxes, DFU, Mixin, Guava, Gson, Netty, JOML, ...), then drops the error classes that are known artefacts of that
setup and prints the rest ("OTHER" = actionable).

Known artefacts (counted, not printed):
  * missing library jars (fastutil, lwjgl, authlib, log4j, asm, ...)
  * Architectury interface injection (arch$tab / arch$registryName)
  * private/protected access that fossil.accesswidener, Architectury's transitive AW or the NeoForge AT open up
  * NeoForge patch/extension members (IAttachmentHolder, IBlockExtension, IItemExtension, DataGenerator, tag/loot/recipe
    datagen helpers): the vanilla sources are unpatched
  * EMI sources are Yarn mapped

Decompiled vanilla contains synthetic bridge methods ("// $FF: synthetic method", e.g. TamableAnimal#level():EntityGetter)
that break overload resolution; --strip-bridges <vanilla_src> <out_dir> writes a copy without them (use that copy on the
sourcepath).

Usage:
  python3 tools/port/javac_xref.py --strip-bridges /path/extracted_minecraft_data/client /tmp/vnb
  python3 tools/port/javac_xref.py --sourcepath-file sp.txt        # sp.txt: one ':'-separated sourcepath line
  python3 tools/port/javac_xref.py --log existing_javac_output.log  # only classify
"""
import os, re, subprocess, sys, collections

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = ["common/src/main/java", "neoforge/src/main/java"]

ARTEFACTS = [
    ("missing library jar", r"fastutil|Object2|Long2|Int2|ObjectArrayList|ObjectOpenHashSet|LongList|LongArrayList|DoubleComparators|lwjgl|GLFW|"
                            r"authlib|GameProfile|ProfileResult|log4j|Logger|LogManager|LogUtils|com\.mojang\.logging|objectweb|ClassNode|"
                            r"javax\.annotation|distmarker|variable Dist|class Nullable|class Pair|variable Pair|datafixers|me\.shedaniel\.math|"
                            r"class Rectangle|class Point|cannot access|bad source file|package TagsProvider"),
    ("decompiled bridge method", r"EntityGetter"),
    ("architectury interface injection", r"arch\$"),
    ("access opened by AW/transitive AW/NeoForge AT", r"has (private|protected) access|is not public in|setPierceLevel|setDebug|cannot override getEntryAtPosition|getCurrentSwingDuration|Failuresaurus\.java:152"),
    ("NeoForge patch/extension member", r"getExistingData|getData\(|getToolModifiedState|DataGenerator|addTags|inventoryTrigger|createSilkTouchOnlyTable|"
                                        r"EntityTypeTagsProvider|spawnEggItem|PlantBakedModel|/forge/data/|Impl\.java"),
    ("EMI (yarn mapped refs)", r"/compat/emi/"),
    ("decompiled ExtraCodecs cascade", r"MultiOutputAndSlotsRecipe"),
]


def strip_bridges(src, dst):
    n = 0
    for d, _, fs in os.walk(src):
        for f in fs:
            if not f.endswith(".java"):
                continue
            p = os.path.join(d, f)
            q = os.path.join(dst, os.path.relpath(p, src))
            os.makedirs(os.path.dirname(q), exist_ok=True)
            lines = open(p, errors="replace").read().split("\n")
            out, i = [], 0
            while i < len(lines):
                if lines[i].strip() == "// $FF: synthetic method":
                    j, depth, started = i + 1, 0, False
                    while j < len(lines):
                        depth += lines[j].count("{") - lines[j].count("}")
                        started = started or "{" in lines[j]
                        if (started and depth <= 0) or (not started and lines[j].rstrip().endswith(";")):
                            break
                        j += 1
                    i, n = j + 1, n + 1
                    continue
                out.append(lines[i])
                i += 1
            open(q, "w").write("\n".join(out))
    print(f"stripped {n} synthetic methods into {dst}")


def run_javac(sourcepath, log):
    files = []
    for s in SRC:
        for d, _, fs in os.walk(os.path.join(ROOT, s)):
            files += [os.path.join(d, f) for f in fs if f.endswith(".java")]
    with open("/tmp/javac_xref_files.txt", "w") as fh:
        fh.write("\n".join(files))
    cmd = ["javac", "-proc:none", "-implicit:none", "-Xmaxerrs", "1000000", "-XDshould-stop.ifError=FLOW",
           "-XDshould-stop.ifNoError=FLOW", "-d", "/tmp/javac_xref_out", "@/tmp/javac_xref_files.txt"]
    if sourcepath:
        cmd[1:1] = ["-sourcepath", sourcepath]
    r = subprocess.run(cmd, capture_output=True, text=True, cwd="/tmp")
    with open(log, "w") as fh:
        fh.write(r.stdout + r.stderr)


def blocks(log):
    cur = None
    for line in open(log, errors="replace"):
        m = re.match(r"^(\S.*?\.java):(\d+): error: (.*)$", line.rstrip("\n"))
        if m:
            if cur:
                yield cur
            cur = [m.group(1), int(m.group(2)), m.group(3), []]
        elif cur is not None:
            cur[3].append(line.rstrip("\n"))
    if cur:
        yield cur


def main():
    a = sys.argv
    if "--strip-bridges" in a:
        i = a.index("--strip-bridges")
        strip_bridges(a[i + 1], a[i + 2])
        return
    log = "/tmp/javac_xref.log"
    if "--log" in a:
        log = a[a.index("--log") + 1]
    else:
        sp = None
        if "--sourcepath-file" in a:
            sp = open(a[a.index("--sourcepath-file") + 1]).read().strip()
        run_javac(sp, log)
    counts, other = collections.Counter(), []
    for f, ln, msg, body in blocks(log):
        rel = os.path.relpath(f, ROOT) if f.startswith("/") else f
        if not any(rel.startswith(s) for s in SRC):
            continue  # errors inside the reference sources themselves
        text = f"{rel}:{ln}: {msg}\n" + "\n".join(body)
        for name, rx in ARTEFACTS:
            if re.search(rx, text):
                counts[name] += 1
                break
        else:
            other.append(text)
    for k, v in counts.most_common():
        print(f"# {v:5d} {k}")
    print(f"# {len(other)} OTHER (actionable):")
    for o in other:
        print(o)


if __name__ == "__main__":
    main()
