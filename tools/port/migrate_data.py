#!/usr/bin/env python3
"""
Data/resource pack migration for the Fossils & Archeology Revival port
Minecraft 1.20.1 (Forge/Fabric) -> 1.21.1 (NeoForge).

Re-runnable and idempotent: running it on already converted data changes nothing.

What it does (see PORT_NOTES/data.md for the full write-up):
  1. Renames datapack folders to the 1.21 singular names (recipes -> recipe, tags/items -> tag/item, ...),
     and forge/biome_modifier -> neoforge/biome_modifier. Folders are merged if both exist.
  2. Renames tag files whose conventional name changed (forge:* / old fabric c:* -> NeoForge 1.21 c:*).
  3. Converts JSON content:
       recipes       result {"item"} / "result": "id" -> {"id", "count"}, stonecutting top-level count,
                     forge "conditions" -> "neoforge:conditions", fabric:load_conditions dropped,
                     legacy ingredient "data" dropped, custom fossil recipe outputs, create:crushing 1.21 format
       loot tables   loot_table entry "name" -> "value", looting_enchant -> enchanted_count_increase,
                     random_chance_with_looting -> random_chance_with_enchanted_bonus, set_nbt -> set_custom_data,
                     copy_nbt -> copy_custom_data, item predicates (enchantments/tag/nbt) -> 1.20.5 sub-predicates,
                     untyped {min,max} number providers get "type": "minecraft:uniform"
       advancements  display.icon {"item","nbt"} -> {"id","count"}, item predicates as above, entity predicates
       tags          renamed tag references
       worldgen      int/float provider {"type","value":{...}} wrappers flattened (1.20.5 MapCodec dispatch)
       biome mods    forge:add_features/add_spawns/... -> neoforge:*
       everywhere    renamed vanilla ids (grass -> short_grass, scute -> turtle_scute), renamed tag references
  4. Validates: every JSON under the given resource roots parses; reports counts per change type and
     lists everything that could not be converted automatically ("MANUAL").

Usage:
  python3 tools/port/migrate_data.py [--repo PATH] [--dry-run] [--forge-to-neoforge] [--mcmeta PATH]

  --forge-to-neoforge  also copy+convert forge/src/main/resources/data -> neoforge/src/main/resources/data
                       (only when neoforge/ exists; never deletes the forge/ originals)
  --mcmeta PATH        directory containing misode/mcmeta checkouts (mcmeta-registries for 1.21.1) used to
                       validate item/block/entity ids referenced by recipes, loot tables, tags (optional)
"""
import argparse
import collections
import copy
import json
import os
import re
import shutil
import sys

# --------------------------------------------------------------------------------------------------------------------
# Tables
# --------------------------------------------------------------------------------------------------------------------

# data/<namespace>/<old> -> data/<namespace>/<new>   (1.21 "depluralized" registry folders, see 1.21 primer)
DIR_RENAMES = [
    ("tags/blocks", "tags/block"),
    ("tags/items", "tags/item"),
    ("tags/entity_types", "tags/entity_type"),
    ("tags/fluids", "tags/fluid"),
    ("tags/game_events", "tags/game_event"),
    ("tags/functions", "tags/function"),
    ("recipes", "recipe"),
    ("loot_tables", "loot_table"),
    ("advancements", "advancement"),
    ("structures", "structure"),
    ("item_modifiers", "item_modifier"),
    ("predicates", "predicate"),
    ("functions", "function"),
    # NeoForge registries live under the neoforge namespace folder
    ("forge/biome_modifier", "neoforge/biome_modifier"),
    ("forge/structure_modifier", "neoforge/structure_modifier"),
]

# Tag ids (without '#') whose name changed. Applied to tag references and tag file locations.
# NeoForge 1.21.1 names verified against NeoForge/src/main/java/net/neoforged/neoforge/common/Tags.java
TAG_RENAMES = {
    "forge:eggs": "c:eggs",
    "forge:glass": "c:glass_blocks",
    "forge:glass_panes": "c:glass_panes",
    "forge:slimeballs": "c:slime_balls",
    "forge:slime_balls": "c:slime_balls",
    "forge:raw_fishes": "c:foods/raw_fish",
    "forge:cooked_fishes": "c:foods/cooked_fish",
    "forge:bones": "c:bones",
    "forge:string": "c:strings",
    "forge:leather": "c:leathers",
    "forge:feathers": "c:feathers",
    "forge:gunpowder": "c:gunpowders",
    "forge:ender_pearls": "c:ender_pearls",
    "forge:shears": "c:tools/shear",
    "forge:chests": "c:chests",
    "forge:chests/wooden": "c:chests/wooden",
    "forge:cobblestone": "c:cobblestones",
    "forge:stone": "c:stones",
    "forge:sand": "c:sands",
    "forge:gravel": "c:gravels",
    "forge:obsidian": "c:obsidians",
    "forge:mushrooms": "c:mushrooms",
    "forge:fences": "c:fences",
    "forge:fence_gates": "c:fence_gates",
    "forge:rods/wooden": "c:rods/wooden",
    "forge:rods/blaze": "c:rods/blaze",
    "forge:heads": "minecraft:skulls",
    # old Fabric conventional (v1) name; our generated tag only holds entity (fish) buckets
    "c:water_buckets": "c:buckets/entity_water",
}
# forge:<prefix>/<x> -> c:<prefix>/<x>
TAG_PREFIX_RENAMES = ["ingots", "nuggets", "gems", "dusts", "ores", "storage_blocks", "raw_materials", "dyes",
                      "crops", "seeds", "rods"]

# Vanilla ids renamed between 1.20.1 and 1.21.1 (data only; assets textures/models are separate)
ID_RENAMES = {
    "minecraft:grass": "minecraft:short_grass",      # 1.20.3
    "minecraft:scute": "minecraft:turtle_scute",     # 1.20.5
}

# Forge condition types -> NeoForge condition types (net.neoforged.neoforge.common.NeoForgeMod#CONDITION_CODECS)
CONDITION_RENAMES = {
    "forge:mod_loaded": "neoforge:mod_loaded",
    "forge:not": "neoforge:not",
    "forge:and": "neoforge:and",
    "forge:or": "neoforge:or",
    "forge:item_exists": "neoforge:item_exists",
    "forge:tag_empty": "neoforge:tag_empty",
    "forge:true": "neoforge:true",
    "forge:false": "neoforge:false",
}

BIOME_MODIFIER_RENAMES = {
    "forge:add_features": "neoforge:add_features",
    "forge:remove_features": "neoforge:remove_features",
    "forge:add_spawns": "neoforge:add_spawns",
    "forge:remove_spawns": "neoforge:remove_spawns",
    "forge:add_spawn_costs": "neoforge:add_spawn_costs",
    "forge:remove_spawn_costs": "neoforge:remove_spawn_costs",
    "forge:add_carvers": "neoforge:add_carvers",
    "forge:remove_carvers": "neoforge:remove_carvers",
    "forge:none": "neoforge:none",
}

CRAFTING_LIKE = {"minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:stonecutting",
                 "minecraft:smithing_transform"}
COOKING = {"minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking"}
FOSSIL_WITH_FUEL = {"fossil:culture_vat", "fossil:worktable"}
FOSSIL_MULTI_OUTPUT = {"fossil:analyzer", "fossil:sifter"}

# keys that hold a dispatch-provider whose payload was wrapped in "value" before 1.20.5
PROVIDER_PAYLOAD_KEYS = {"min_inclusive", "max_inclusive", "source", "mean", "deviation", "distribution",
                         "plateau", "inner"}

# --------------------------------------------------------------------------------------------------------------------
# Bookkeeping
# --------------------------------------------------------------------------------------------------------------------

STATS = collections.Counter()
MANUAL = []  # (file, message)
_current_file = [None]


def stat(name, n=1):
    STATS[name] += n


def manual(msg):
    MANUAL.append((_current_file[0], msg))


# --------------------------------------------------------------------------------------------------------------------
# Generic helpers
# --------------------------------------------------------------------------------------------------------------------

def rename_tag_id(tag_id):
    if tag_id in TAG_RENAMES:
        return TAG_RENAMES[tag_id]
    if tag_id.startswith("forge:"):
        path = tag_id[len("forge:"):]
        if path.split("/")[0] in TAG_PREFIX_RENAMES and "/" in path:
            return "c:" + path
    return tag_id


def rename_string(s):
    """Rename '#tag' references and renamed vanilla ids in a plain string value."""
    if s.startswith("#"):
        new = "#" + rename_tag_id(s[1:])
        if new != s:
            stat("tag reference renamed")
            return new
        return s
    if s in ID_RENAMES:
        stat("vanilla id renamed (%s)" % s)
        return ID_RENAMES[s]
    return s


def walk_generic(o):
    """Apply id/tag renames to all string values and to {"tag": x} values in any JSON."""
    if isinstance(o, dict):
        for k, v in list(o.items()):
            if isinstance(v, str):
                if k == "tag":
                    nv = rename_tag_id(v)
                    if nv != v:
                        stat("tag reference renamed")
                        o[k] = nv
                else:
                    o[k] = rename_string(v)
            else:
                walk_generic(v)
    elif isinstance(o, list):
        for i, v in enumerate(o):
            if isinstance(v, str):
                o[i] = rename_string(v)
            else:
                walk_generic(v)


def find_leftovers(o, path=""):
    """Report any remaining forge:/fabric: references (they need a human)."""
    if isinstance(o, dict):
        for k, v in o.items():
            if k.startswith("forge:") or k.startswith("fabric:"):
                manual("leftover key '%s' at %s" % (k, path))
            find_leftovers(v, path + "/" + k)
    elif isinstance(o, list):
        for i, v in enumerate(o):
            find_leftovers(v, "%s[%d]" % (path, i))
    elif isinstance(o, str):
        if re.match(r"^#?(forge|fabric):", o):
            manual("leftover value '%s' at %s" % (o, path))


def convert_conditions(obj):
    """Forge/Fabric load conditions at top level -> neoforge:conditions."""
    changed = False
    if "fabric:load_conditions" in obj:
        del obj["fabric:load_conditions"]
        stat("fabric:load_conditions removed")
        changed = True
    if "conditions" in obj and isinstance(obj["conditions"], list) and obj["conditions"] and \
            isinstance(obj["conditions"][0], dict) and "type" in obj["conditions"][0]:
        conds = obj.pop("conditions")

        def conv(c):
            if isinstance(c, dict):
                t = c.get("type")
                if t in CONDITION_RENAMES:
                    c["type"] = CONDITION_RENAMES[t]
                elif t and not t.startswith("neoforge:"):
                    manual("unknown load condition type %s" % t)
                # neoforge:not uses "value"; and/or use "values" (same as forge)
                for k in ("value",):
                    if k in c:
                        conv(c[k])
                for k in ("values",):
                    if k in c and isinstance(c[k], list):
                        for x in c[k]:
                            conv(x)
                # put "type" first for readability
                d = {"type": c.pop("type")} if "type" in c else {}
                d.update(c)
                c.clear()
                c.update(d)
            return c

        for c in conds:
            conv(c)
        new = {"neoforge:conditions": conds}
        new.update(obj)
        obj.clear()
        obj.update(new)
        stat("forge conditions -> neoforge:conditions")
        changed = True
    return changed


# --------------------------------------------------------------------------------------------------------------------
# Item stacks / predicates
# --------------------------------------------------------------------------------------------------------------------

def nbt_to_components(nbt):
    """Very small SNBT -> components translation for the cases we actually have. Returns (components, ok)."""
    if nbt is None:
        return {}, True
    s = nbt.strip() if isinstance(nbt, str) else None
    if s in ("{Damage:0}", "{Damage:0b}", "{}"):
        return {}, True  # default damage; nothing to encode
    return {"minecraft:custom_data": nbt}, False


def item_result(r, with_count=True):
    """1.20.1 result ("id" string or {"item","count","nbt"}) -> 1.21 ItemStack codec {"count","id","components"}."""
    if isinstance(r, str):
        out = {"count": 1, "id": r} if with_count else {"id": r}
        stat("recipe result string -> {id}")
        return out
    if isinstance(r, dict) and "item" in r and "id" not in r:
        out = {}
        if with_count or "count" in r:
            out["count"] = r.get("count", 1)
        out["id"] = r["item"]
        if "nbt" in r:
            comps, ok = nbt_to_components(r["nbt"])
            if comps:
                out["components"] = comps
            if not ok:
                manual("result nbt %s converted to custom_data, check component mapping" % r["nbt"])
        for k, v in r.items():
            if k not in ("item", "count", "nbt"):
                out[k] = v
        stat("recipe result {item} -> {id}")
        return out
    return r


def convert_ingredient(ing):
    if isinstance(ing, list):
        return [convert_ingredient(x) for x in ing]
    if isinstance(ing, dict):
        if "data" in ing and "item" in ing:
            del ing["data"]
            stat("legacy ingredient 'data' removed")
        t = ing.get("type")
        if t:
            if t in ("forge:nbt", "forge:partial_nbt", "forge:intersection", "forge:difference", "forge:compound"):
                manual("forge custom ingredient %s needs manual conversion to neoforge:* ingredient" % t)
            elif not t.startswith("neoforge:"):
                manual("custom ingredient type %s" % t)
    return ing


def convert_item_predicate(p):
    """1.20.1 ItemPredicate -> 1.20.5+ ItemPredicate (items HolderSet + sub predicates)."""
    if not isinstance(p, dict):
        return p
    changed = False
    if "tag" in p:
        tag = p.pop("tag")
        if "items" in p:
            manual("item predicate has both 'tag' and 'items'; kept items, dropped tag #%s" % tag)
        else:
            p["items"] = "#" + rename_tag_id(tag)
        changed = True
    # NOTE: "items": [..] lists are still valid HolderSets in 1.21.1, so they are left as lists.
    preds = p.get("predicates", {})
    if "enchantments" in p:
        preds["minecraft:enchantments"] = [convert_enchantment_predicate(e) for e in p.pop("enchantments")]
        changed = True
    if "stored_enchantments" in p:
        preds["minecraft:stored_enchantments"] = [convert_enchantment_predicate(e) for e in
                                                 p.pop("stored_enchantments")]
        changed = True
    if "potion" in p:
        preds["minecraft:potion_contents"] = p.pop("potion")
        changed = True
    if "durability" in p:
        preds["minecraft:damage"] = {"durability": p.pop("durability")}
        changed = True
    if "nbt" in p:
        nbt = p.pop("nbt")
        preds["minecraft:custom_data"] = nbt
        manual("item predicate nbt %s -> minecraft:custom_data; vanilla data now lives in components, verify" % nbt)
        changed = True
    if preds:
        p["predicates"] = preds
    if changed:
        stat("item predicate converted")
    return p


def convert_enchantment_predicate(e):
    if isinstance(e, dict) and "enchantment" in e:
        e = dict(e)
        e["enchantments"] = e.pop("enchantment")
        # keep key order: enchantments, levels
        out = {"enchantments": e.pop("enchantments")}
        out.update(e)
        return out
    return e


def convert_location_predicate(p):
    if not isinstance(p, dict):
        return p
    if "biome" in p:
        p["biomes"] = p.pop("biome")
        stat("location predicate biome -> biomes")
    if "structure" in p:
        p["structures"] = p.pop("structure")
        stat("location predicate structure -> structures")
    if "dimension" in p:
        pass
    if "block" in p and isinstance(p["block"], dict):
        b = p["block"]
        if "tag" in b:
            b["blocks"] = "#" + b.pop("tag")
        if "nbt" in b:
            manual("block predicate nbt kept as-is (still valid in 1.21.1)")
    return p


def convert_entity_predicate(p):
    if not isinstance(p, dict):
        return p
    if "equipment" in p and isinstance(p["equipment"], dict):
        for slot, ip in p["equipment"].items():
            p["equipment"][slot] = convert_item_predicate(ip)
    for k in ("location", "stepping_on"):
        if k in p:
            convert_location_predicate(p[k])
    for k in ("vehicle", "passenger", "targeted_entity"):
        if k in p:
            convert_entity_predicate(p[k])
    if "type_specific" in p and isinstance(p["type_specific"], dict):
        ts = p["type_specific"]
        if ts.get("type") == "player" or ts.get("type") == "minecraft:player":
            if "looking_at" in ts:
                convert_entity_predicate(ts["looking_at"])
    return p


def convert_loot_condition(c):
    if isinstance(c, list):
        for x in c:
            convert_loot_condition(x)
        return
    if not isinstance(c, dict):
        return
    t = c.get("condition")
    if t in ("minecraft:alternative",):
        c["condition"] = "minecraft:any_of"
    if t == "minecraft:match_tool" and "predicate" in c:
        convert_item_predicate(c["predicate"])
    elif t == "minecraft:entity_properties" and "predicate" in c:
        convert_entity_predicate(c["predicate"])
    elif t == "minecraft:location_check" and "predicate" in c:
        convert_location_predicate(c["predicate"])
    elif t == "minecraft:random_chance_with_looting":
        chance = c.pop("chance")
        mult = c.pop("looting_multiplier")
        c.clear()
        c.update({"condition": "minecraft:random_chance_with_enchanted_bonus",
                  "enchanted_chance": {"type": "minecraft:linear", "base": chance + mult,
                                       "per_level_above_first": mult},
                  "enchantment": "minecraft:looting",
                  "unenchanted_chance": chance})
        stat("random_chance_with_looting -> random_chance_with_enchanted_bonus")
    for k in ("term",):
        if k in c:
            convert_loot_condition(c[k])
    for k in ("terms",):
        if k in c:
            convert_loot_condition(c[k])


def fix_number_provider(n):
    if isinstance(n, dict) and "type" not in n and set(n.keys()) <= {"min", "max"} and n:
        new = {"type": "minecraft:uniform"}
        new.update(n)
        stat("untyped number provider -> minecraft:uniform")
        return new
    return n


def convert_loot_function(f):
    if not isinstance(f, dict):
        return
    fn = f.get("function")
    if fn == "minecraft:looting_enchant":
        new = {}
        for k, v in f.items():
            if k == "function":
                continue
            new[k] = v
        new["enchantment"] = "minecraft:looting"
        new["function"] = "minecraft:enchanted_count_increase"
        f.clear()
        f.update(new)
        stat("looting_enchant -> enchanted_count_increase")
    elif fn == "minecraft:set_nbt":
        f["function"] = "minecraft:set_custom_data"
        stat("set_nbt -> set_custom_data")
        manual("set_nbt -> set_custom_data: vanilla NBT keys (Damage, Enchantments, display...) must become set_components")
    elif fn == "minecraft:copy_nbt":
        f["function"] = "minecraft:copy_custom_data"
        stat("copy_nbt -> copy_custom_data")
        manual("copy_nbt -> copy_custom_data: verify source/targets")
    elif fn in ("minecraft:enchant_randomly",):
        if "enchantments" in f:
            f["options"] = f.pop("enchantments")
            stat("enchant_randomly enchantments -> options")
    elif fn in ("minecraft:enchant_with_levels",):
        if "treasure" in f:
            treasure = f.pop("treasure")
            if treasure:
                manual("enchant_with_levels treasure=true: set options to a tag incl. treasure enchantments")
            stat("enchant_with_levels treasure removed")
    elif fn == "minecraft:set_count":
        if "count" in f:
            f["count"] = fix_number_provider(f["count"])
    elif fn == "minecraft:set_name" or fn == "minecraft:set_lore":
        pass
    elif fn and fn.startswith("minecraft:"):
        pass
    if "conditions" in f:
        convert_loot_condition(f["conditions"])


def convert_loot_entry(e):
    if not isinstance(e, dict):
        return
    t = e.get("type")
    if t == "minecraft:loot_table" and "name" in e and "value" not in e:
        new = {}
        for k, v in e.items():
            new["value" if k == "name" else k] = v
        e.clear()
        e.update(new)
        stat("loot_table entry name -> value")
    for c in e.get("children", []):
        convert_loot_entry(c)
    for f in e.get("functions", []):
        convert_loot_function(f)
    if "conditions" in e:
        convert_loot_condition(e["conditions"])


def convert_loot_table(d):
    for pool in d.get("pools", []):
        for k in ("rolls", "bonus_rolls"):
            if k in pool:
                pool[k] = fix_number_provider(pool[k])
        for e in pool.get("entries", []):
            convert_loot_entry(e)
        for f in pool.get("functions", []):
            convert_loot_function(f)
        if "conditions" in pool:
            convert_loot_condition(pool["conditions"])
    for f in d.get("functions", []):
        convert_loot_function(f)


# --------------------------------------------------------------------------------------------------------------------
# Per category converters
# --------------------------------------------------------------------------------------------------------------------

def convert_recipe(d):
    convert_conditions(d)
    t = d.get("type")
    if t in CRAFTING_LIKE:
        if t == "minecraft:stonecutting" and isinstance(d.get("result"), str):
            count = d.pop("count", 1)
            d["result"] = {"count": count, "id": d["result"]}
            stat("stonecutting result+count -> {count,id}")
        elif "result" in d:
            d["result"] = item_result(d["result"], with_count=True)
        for k in ("ingredient", "base", "addition", "template"):
            if k in d:
                d[k] = convert_ingredient(d[k])
        if "ingredients" in d:
            d["ingredients"] = [convert_ingredient(x) for x in d["ingredients"]]
        if "key" in d:
            for k in d["key"]:
                d["key"][k] = convert_ingredient(d["key"][k])
    elif t in COOKING:
        if "result" in d:
            d["result"] = item_result(d["result"], with_count=False)
        if "ingredient" in d:
            d["ingredient"] = convert_ingredient(d["ingredient"])
    elif t in FOSSIL_WITH_FUEL:
        # culture_vat / worktable: input, fuel = Ingredient; result = ItemStack {"id", "count"?, "components"?}
        if "result" in d:
            d["result"] = item_result(d["result"], with_count=False)
        for k in ("input", "fuel"):
            if k in d:
                d[k] = convert_ingredient(d[k])
    elif t in FOSSIL_MULTI_OUTPUT:
        # analyzer / sifter: input = Ingredient; outputs = [{"id", "count"?, "components"?, "weight"}]; total?
        if "input" in d:
            d["input"] = convert_ingredient(d["input"])
        outs = []
        for o in d.get("outputs", []):
            if isinstance(o, dict) and "item" in o:
                n = {"id": o["item"]}
                if "count" in o:
                    n["count"] = o["count"]
                if "nbt" in o:
                    comps, ok = nbt_to_components(o["nbt"])
                    if comps:
                        n["components"] = comps
                    if not ok:
                        manual("output nbt %s converted to custom_data, check" % o["nbt"])
                for k, v in o.items():
                    if k not in ("item", "count", "nbt"):
                        n[k] = v
                outs.append(n)
                stat("fossil weighted output {item} -> {id}")
            else:
                outs.append(o)
        if "outputs" in d:
            d["outputs"] = outs
    elif t == "create:crushing" or (t or "").startswith("create:"):
        # Create 6 (1.21.1): processing_time, results [{"id", "count"?, "chance"?}]
        if "processingTime" in d:
            new = {}
            for k, v in d.items():
                new["processing_time" if k == "processingTime" else k] = v
            d.clear()
            d.update(new)
            stat("create processingTime -> processing_time")
        res = []
        for r in d.get("results", []):
            if isinstance(r, dict) and "item" in r:
                n = {}
                if "count" in r:
                    n["count"] = r["count"]
                if "chance" in r:
                    n["chance"] = r["chance"]
                n["id"] = r["item"]
                for k, v in r.items():
                    if k not in ("item", "count", "chance"):
                        n[k] = v
                res.append(n)
                stat("create result {item} -> {id}")
            else:
                res.append(r)
        if "results" in d:
            d["results"] = res
        if "ingredients" in d:
            d["ingredients"] = [convert_ingredient(x) for x in d["ingredients"]]
    elif t:
        manual("recipe type %s not handled by script" % t)


def convert_advancement(d):
    convert_conditions(d)
    disp = d.get("display")
    if isinstance(disp, dict) and isinstance(disp.get("icon"), dict):
        icon = disp["icon"]
        if "item" in icon:
            new = {"count": icon.get("count", 1), "id": icon["item"]}
            if "nbt" in icon:
                comps, ok = nbt_to_components(icon["nbt"])
                if comps:
                    new["components"] = comps
                if not ok:
                    manual("advancement icon nbt %s -> custom_data, check" % icon["nbt"])
            disp["icon"] = new
            stat("advancement icon {item} -> {id}")
    for name, crit in d.get("criteria", {}).items():
        cond = crit.get("conditions")
        if not isinstance(cond, dict):
            continue
        for k, v in list(cond.items()):
            if k in ("item", "tool"):
                convert_item_predicate(v)
            elif k in ("items", "ingredients") and isinstance(v, list):
                for ip in v:
                    convert_item_predicate(ip)
            elif k in ("entity", "player", "parent", "partner", "child", "victim", "villager", "zombie",
                       "projectile", "shooter", "source_entity", "lightning", "bystander"):
                if isinstance(v, list):
                    convert_loot_condition(v)
                elif isinstance(v, dict):
                    convert_entity_predicate(v)
            elif k == "location" or k == "entered" or k == "exited":
                if isinstance(v, list):
                    convert_loot_condition(v)
                elif isinstance(v, dict):
                    convert_location_predicate(v)


def flatten_providers(o):
    """{"type": T, "value": {min_inclusive...}} -> {"type": T, min_inclusive...} (1.20.5 MapCodec dispatch)."""
    if isinstance(o, dict):
        if set(o.keys()) == {"type", "value"} and isinstance(o["value"], dict) and \
                set(o["value"].keys()) & PROVIDER_PAYLOAD_KEYS:
            inner = o.pop("value")
            for k, v in inner.items():
                o[k] = v
            stat("int/float provider 'value' wrapper flattened")
        for v in o.values():
            flatten_providers(v)
    elif isinstance(o, list):
        for v in o:
            flatten_providers(v)


def convert_biome_modifier(d):
    t = d.get("type")
    if t in BIOME_MODIFIER_RENAMES:
        d["type"] = BIOME_MODIFIER_RENAMES[t]
        stat("biome modifier type forge -> neoforge")
    elif t and t.startswith("forge:"):
        manual("unknown forge biome modifier %s" % t)


def classify(rel):
    """rel = path relative to data/ (namespace/...). Returns category."""
    parts = rel.split("/")
    sub = "/".join(parts[1:])
    for old, new in DIR_RENAMES:  # also classify not-yet-renamed folders (dry run)
        if sub.startswith(old + "/"):
            sub = new + sub[len(old):]
            break
    for prefix, cat in (("recipe/", "recipe"), ("loot_table/", "loot_table"), ("advancement/", "advancement"),
                        ("tags/", "tag"), ("worldgen/", "worldgen"), ("neoforge/biome_modifier/", "biome_modifier"),
                        ("dimension/", "worldgen"), ("dimension_type/", "worldgen"),
                        ("item_modifier/", "loot_function"), ("predicate/", "predicate")):
        if sub.startswith(prefix):
            return cat
    return "other"


def convert_json(obj, cat):
    if cat == "recipe":
        convert_recipe(obj)
    elif cat == "loot_table":
        convert_loot_table(obj)
    elif cat == "advancement":
        convert_advancement(obj)
    elif cat == "worldgen":
        flatten_providers(obj)
    elif cat == "biome_modifier":
        convert_biome_modifier(obj)
    elif cat == "loot_function":
        for f in (obj if isinstance(obj, list) else [obj]):
            convert_loot_function(f)
    elif cat == "predicate":
        convert_loot_condition(obj)
    else:
        convert_conditions(obj) if isinstance(obj, dict) else None
    walk_generic(obj)
    find_leftovers(obj)
    return obj


# --------------------------------------------------------------------------------------------------------------------
# File system
# --------------------------------------------------------------------------------------------------------------------

def detect_style(text):
    m = re.search(r"\n([ \t]+)\S", text)
    indent = m.group(1) if m else "  "
    if indent.startswith("\t"):
        indent = "\t"
    return indent, text.endswith("\n")


def dump(obj, indent, trailing_nl):
    s = json.dumps(obj, indent=indent, ensure_ascii=False)
    return s + ("\n" if trailing_nl else "")


def merge_move(src, dst, dry):
    """Move directory src to dst, merging into dst if it already exists."""
    if not os.path.exists(src):
        return 0
    moved = 0
    for root, dirs, files in os.walk(src):
        for fn in files:
            s = os.path.join(root, fn)
            d = os.path.join(dst, os.path.relpath(s, src))
            if os.path.exists(d):
                MANUAL.append((s, "rename target exists, kept target %s; source left in place" % d))
                continue
            if not dry:
                os.makedirs(os.path.dirname(d), exist_ok=True)
                shutil.move(s, d)
            moved += 1
    if not dry:
        # remove empty dirs left behind
        for root, dirs, files in os.walk(src, topdown=False):
            if not os.listdir(root):
                os.rmdir(root)
    return moved


def rename_dirs(data_root, dry):
    if not os.path.isdir(data_root):
        return
    for ns in sorted(os.listdir(data_root)):
        nsdir = os.path.join(data_root, ns)
        if not os.path.isdir(nsdir):
            continue
        for old, new in DIR_RENAMES:
            src = os.path.join(nsdir, old)
            if os.path.isdir(src):
                n = merge_move(src, os.path.join(nsdir, new), dry)
                stat("files moved %s -> %s" % (old, new), n)
        # forge/ empty parent left behind
        fdir = os.path.join(nsdir, "forge")
        if not dry and os.path.isdir(fdir) and not os.listdir(fdir):
            os.rmdir(fdir)


def rename_tag_files(data_root, dry):
    """Move tag files whose id changed, e.g. data/forge/tags/item/eggs.json -> data/c/tags/item/eggs.json."""
    if not os.path.isdir(data_root):
        return
    moves = []
    for ns in os.listdir(data_root):
        tags = os.path.join(data_root, ns, "tags")
        if not os.path.isdir(tags):
            continue
        for root, dirs, files in os.walk(tags):
            for fn in files:
                if not fn.endswith(".json"):
                    continue
                p = os.path.join(root, fn)
                rel = os.path.relpath(p, tags)  # <registry>/<path>.json  (registry may contain '/': worldgen/biome)
                reg_parts = rel.split("/")
                # registry folder is 1 segment except worldgen/<x>
                n = 2 if reg_parts[0] == "worldgen" else 1
                reg = "/".join(reg_parts[:n])
                path = "/".join(reg_parts[n:])[:-5]
                old_id = "%s:%s" % (ns, path)
                new_id = rename_tag_id(old_id)
                if new_id != old_id:
                    nns, npath = new_id.split(":", 1)
                    moves.append((p, os.path.join(data_root, nns, "tags", reg, npath + ".json"), old_id, new_id))
    for src, dst, old_id, new_id in moves:
        if os.path.exists(dst):
            # merge values
            a = json.load(open(src, encoding="utf-8"))
            b = json.load(open(dst, encoding="utf-8"))
            vals = b.get("values", [])
            for v in a.get("values", []):
                if v not in vals:
                    vals.append(v)
            b["values"] = vals
            if not dry:
                with open(dst, "w", encoding="utf-8") as fh:
                    fh.write(dump(b, "  ", False))
                os.remove(src)
        elif not dry:
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.move(src, dst)
        stat("tag file renamed (%s -> %s)" % (old_id, new_id))
        if not dry:
            d = os.path.dirname(src)
            while d != data_root and os.path.isdir(d) and not os.listdir(d):
                os.rmdir(d)
                d = os.path.dirname(d)


CUSTOM_SKIP = re.compile(r"^[^/]+/(animations|hitboxes|entity_info|variants)/")


def convert_tree(data_root, dry, changed_files):
    if not os.path.isdir(data_root):
        return
    for root, dirs, files in os.walk(data_root):
        for fn in sorted(files):
            if not fn.endswith(".json"):
                continue
            p = os.path.join(root, fn)
            rel = os.path.relpath(p, data_root)
            _current_file[0] = p
            with open(p, encoding="utf-8") as fh:
                text = fh.read()
            try:
                obj = json.loads(text)
            except ValueError as e:
                MANUAL.append((p, "JSON parse error: %s" % e))
                continue
            before = copy.deepcopy(obj)
            cat = classify(rel)
            if CUSTOM_SKIP.match(rel):
                walk_generic(obj)  # only id renames for our own formats
            else:
                obj = convert_json(obj, cat)
            if obj != before:
                stat("files changed [%s]" % cat)
                changed_files.append(p)
                if not dry:
                    indent, nl = detect_style(text)
                    with open(p, "w", encoding="utf-8") as fh:
                        fh.write(dump(obj, indent, nl))


def converted_rel_path(rel):
    """Path (relative to data/) a 1.20.1 file ends up at after rename_dirs + rename_tag_files."""
    ns, _, sub = rel.partition("/")
    for old, new in DIR_RENAMES:
        if sub.startswith(old + "/"):
            sub = new + sub[len(old):]
            break
    if sub.startswith("tags/") and sub.endswith(".json"):
        parts = sub.split("/")
        n = 3 if parts[1] == "worldgen" else 2
        reg = "/".join(parts[1:n])
        tag_id = "%s:%s" % (ns, "/".join(parts[n:])[:-5])
        nns, npath = rename_tag_id(tag_id).split(":", 1)
        return "%s/tags/%s/%s.json" % (nns, reg, npath)
    return "%s/%s" % (ns, sub)


def copy_forge_to_neoforge(repo, dry):
    src = os.path.join(repo, "forge/src/main/resources/data")
    dst = os.path.join(repo, "neoforge/src/main/resources/data")
    if not os.path.isdir(os.path.join(repo, "neoforge")):
        print("neoforge/ does not exist; skipping forge -> neoforge data copy")
        return None
    n = 0
    for root, dirs, files in os.walk(src):
        for fn in files:
            s = os.path.join(root, fn)
            rel = os.path.relpath(s, src)
            d = os.path.join(dst, rel)
            if os.path.exists(d) or os.path.exists(os.path.join(dst, converted_rel_path(rel))):
                continue  # already copied (and possibly renamed/converted) by an earlier run
            if not dry:
                os.makedirs(os.path.dirname(d), exist_ok=True)
                shutil.copy2(s, d)
            n += 1
    stat("forge data files copied to neoforge/", n)
    return dst


# --------------------------------------------------------------------------------------------------------------------
# Validation
# --------------------------------------------------------------------------------------------------------------------

def load_registry(mcmeta, name):
    p = os.path.join(mcmeta, "mcmeta-registries", name, "data.min.json")
    if os.path.exists(p):
        return set("minecraft:" + x for x in json.load(open(p)))
    return None


def validate(repo, roots, mcmeta, mod_items=None):
    bad = 0
    total = 0
    for r in roots:
        for root, dirs, files in os.walk(r):
            if "/.cache" in root:
                continue
            for fn in files:
                if fn.endswith(".json") or fn.endswith(".mcmeta"):
                    total += 1
                    p = os.path.join(root, fn)
                    try:
                        json.load(open(p, encoding="utf-8"))
                    except ValueError as e:
                        bad += 1
                        MANUAL.append((p, "JSON does not parse: %s" % e))
    STATS["validate: json files parsed"] = total
    STATS["validate: json parse failures"] = bad

    # leftover plural folders
    for r in roots:
        if not r.endswith("/data"):
            continue
        for ns in (os.listdir(r) if os.path.isdir(r) else []):
            for old, new in DIR_RENAMES:
                if os.path.isdir(os.path.join(r, ns, old)):
                    MANUAL.append((os.path.join(r, ns, old), "plural folder still present"))

    if not mcmeta:
        return
    items = load_registry(mcmeta, "item")
    blocks = load_registry(mcmeta, "block")
    entities = load_registry(mcmeta, "entity_type")
    if not items:
        return

    def check(idv, reg, regname, where):
        if isinstance(idv, str) and idv.startswith("minecraft:") and idv not in reg:
            MANUAL.append((where, "unknown vanilla %s id %s" % (regname, idv)))
            STATS["validate: unknown vanilla ids"] += 1

    def walk(o, f, cat):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in ("item", "id") and cat in ("recipe", "advancement"):
                    check(v, items, "item", f)
                if k == "name" and o.get("type") == "minecraft:item":
                    check(v, items, "item", f)
                if k == "block" and isinstance(v, str):
                    check(v, blocks, "block", f)
                walk(v, f, cat)
        elif isinstance(o, list):
            for v in o:
                walk(v, f, cat)

    for r in roots:
        if not r.endswith("/data"):
            continue
        for root, dirs, files in os.walk(r):
            for fn in files:
                if not fn.endswith(".json"):
                    continue
                p = os.path.join(root, fn)
                rel = os.path.relpath(p, r)
                cat = classify(rel)
                try:
                    o = json.load(open(p, encoding="utf-8"))
                except ValueError:
                    continue
                if cat == "tag":
                    reg = {"item": items, "block": blocks, "entity_type": entities}.get(rel.split("/")[2])
                    if reg:
                        for v in o.get("values", []):
                            vid = v["id"] if isinstance(v, dict) else v
                            if not vid.startswith("#"):
                                check(vid, reg, rel.split("/")[2], p)
                elif cat in ("recipe", "loot_table", "advancement") or rel.startswith("fossil/food/"):
                    walk(o, p, cat)


# --------------------------------------------------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--forge-to-neoforge", action="store_true")
    ap.add_argument("--mcmeta", default=None, help="dir with mcmeta-registries checkout (1.21.1-registries)")
    ap.add_argument("--quiet", action="store_true")
    a = ap.parse_args()
    repo = a.repo

    data_roots = [os.path.join(repo, "common/src/main/resources/data"),
                  os.path.join(repo, "common/src/generated/resources/data")]
    asset_roots = [os.path.join(repo, "common/src/main/resources/assets"),
                   os.path.join(repo, "common/src/generated/resources/assets")]
    if a.forge_to_neoforge:
        nd = copy_forge_to_neoforge(repo, a.dry_run)
        if nd:
            data_roots.append(nd)

    changed = []
    for r in data_roots:
        rename_dirs(r, a.dry_run)
        rename_tag_files(r, a.dry_run)
        convert_tree(r, a.dry_run, changed)

    validate(repo, data_roots + asset_roots, a.mcmeta)

    print("== migrate_data.py %s" % ("(dry run)" if a.dry_run else ""))
    for k in sorted(STATS):
        print("  %6d  %s" % (STATS[k], k))
    print("== MANUAL (%d)" % len(MANUAL))
    seen = set()
    for f, m in MANUAL:
        key = (f, m)
        if key in seen:
            continue
        seen.add(key)
        print("  %s: %s" % (os.path.relpath(f, repo) if f else "-", m))
    return 0


if __name__ == "__main__":
    sys.exit(main())
