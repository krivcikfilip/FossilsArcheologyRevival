# Data agent notes (resources / datapack / generated JSON) — 1.20.1 → 1.21.1 NeoForge

Owner scope: `common/src/main/resources/**`, `common/src/generated/**` (except `fossil.accesswidener`, `*.mixins.json`),
plus the conversion of `forge/src/main/resources/data/**` → `neoforge/src/main/resources/data/**`.

## How to re-run

```
python3 tools/port/migrate_data.py --forge-to-neoforge --mcmeta /home/claude/port/refs
```
- Idempotent (verified: second run changes 0 files; a run on a pristine `git archive HEAD` export reproduces the
  current tree byte-for-byte except the hand-written files listed under "Manual additions").
- `--dry-run` reports without touching files. `--mcmeta` (optional) points to a dir containing a
  `mcmeta-registries` checkout (`misode/mcmeta` branch `1.21.1-registries`) and enables typed id validation
  (vanilla item/block/entity ids in recipes, loot tables, advancements, tags, `food/`).
- `--forge-to-neoforge` copies `forge/src/main/resources/data` into `neoforge/src/main/resources/data` (only if `neoforge/`
  exists; never deletes the forge originals; skips files whose converted destination already exists) and converts it.
- **Phase 2:** datagen will regenerate `common/src/generated/**`. Before re-running datagen, delete
  `common/src/generated/resources/{data,assets}` (keep or delete `.cache`): the old `.cache` lists the *plural* paths, so
  datagen can't clean up files I moved, and anything datagen no longer produces would stay behind as an orphan.
  If the datagen providers are not yet 1.21-correct, the script can be re-run on their output as a fallback.

Reference ground truth used: `misode/mcmeta` `1.21.1-data`, `1.20.1-data`, `1.21.1-assets`, `1.21.1-registries`
(cloned to `/home/claude/port/refs/mcmeta-*`), NeoForge 1.21.1 sources (conditions, biome modifiers, `Tags.java`,
`ResourcePackLoader`), Create `mc1.21.1/dev` generated recipes (crushing format).

## Counts (full run from 1.20.1 HEAD)

| Change | Count |
|---|---|
| files moved `recipes`→`recipe` | 659 (625 generated, 30 main, 4 neoforge) |
| files moved `advancements`→`advancement` | 466 |
| files moved `loot_tables`→`loot_table` | 346 |
| files moved `structures`→`structure` (.nbt) | 74 |
| files moved `tags/items`→`tags/item` / `tags/blocks`→`tags/block` / `tags/entity_types`→`tags/entity_type` / `tags/fluids`→`tags/fluid` | 98 / 47 / 12 / 1 |
| files moved `forge/biome_modifier`→`neoforge/biome_modifier` | 5 |
| tag files renamed (id changed) | 5 (`c:water_buckets`→`c:buckets/entity_water`, `forge:eggs`→`c:eggs`, `forge:cooked_fishes`→`c:foods/cooked_fish`, `forge:raw_fishes`→`c:foods/raw_fish`, `forge:slimeballs`→`c:slime_balls`) |
| JSON files whose content changed | recipe 659, loot_table 87, advancement 67, biome_modifier 5, worldgen 3, tag 2, other 1 |
| recipe result string → `{"id"}` | 354 |
| recipe result `{"item"}` → `{"count","id"}` | 239 |
| stonecutting `result`+top-level `count` → `{"count","id"}` | 21 |
| fossil analyzer/sifter weighted outputs `{"item"}`→`{"id"}` | 344 entries |
| forge `conditions` → `neoforge:conditions` | 13 (+13 `fabric:load_conditions` removed) |
| Create crushing: `processingTime`→`processing_time`, results `{"item"}`→`{"id"}` | 7 files / 42 results |
| legacy ingredient `"data": 0` removed | 6 |
| item predicates converted (`tag`→`items: "#.."`, `enchantments`→`predicates.minecraft:enchantments`) | 110 |
| advancement `display.icon` `{"item","nbt"}`→`{"count","id"}` | 28 |
| loot `minecraft:loot_table` entry `name`→`value` | 51 |
| `looting_enchant` → `enchanted_count_increase` + `"enchantment": "minecraft:looting"` | 61 |
| untyped `{min,max}` number providers → `"type": "minecraft:uniform"` (defensive, chest tables) | 93 |
| IntProvider/FloatProvider `{"type","value":{..}}` flattened (1.20.5 MapCodec dispatch) | 3 |
| biome modifier types `forge:*`→`neoforge:*` | 5 |
| renamed vanilla ids (`minecraft:grass`→`short_grass` ×3, `minecraft:scute`→`turtle_scute` ×1) | 4 |
| tag references renamed | 6 |
| JSON files validated (data+assets, all parse) | 4108, 0 failures, 0 MANUAL items, 0 unknown vanilla ids |

## Format decisions (what the script produces)

- **Recipes (vanilla types)** — match 1.21.1 vanilla datagen: crafting/stonecutting/smithing `"result": {"count": n, "id": ".."}`,
  cooking `"result": {"id": ".."}` (NeoForge patches cooking to `ItemStack.CODEC`, count optional). Ingredients stay
  `{"item"}` / `{"tag"}` (unchanged in 1.21.1; the `Ingredient` format only changes in 1.21.2).
- **Load conditions**: `"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": ".."}]`,
  `{"type": "neoforge:not", "value": {...}}` (NeoForge `ConditionalOps.DEFAULT_CONDITIONS_KEY`, `NeoForgeMod#CONDITION_CODECS`).
  Fabric `fabric:load_conditions` removed (Fabric is dropped).
- **Loot tables**: match_tool → `{"predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "fossil:archeology", "levels": 1}]}}}`;
  `{"items": ["minecraft:shears"]}` lists left as lists (still a valid HolderSet). `table_bonus`/`apply_bonus` unchanged (enchantment id = holder).
  `minecraft:dynamic` entries (`fossil:bones_*`, filled by Java dynamic drops) unchanged.
- **Advancements**: icon `{"count": 1, "id": ".."}` (`{Damage:0}` nbt dropped — default value); inventory_changed
  `{"items": "#tag"}` / `{"items": [..]}`; entity conditions (`entity_properties` + `"type"`) unchanged.
- **Tags**: NeoForge 1.21.1 conventional names (`Tags.java`). See "Requests" for the Java constants that still use old names.
- **Worldgen**: only the provider wrapper flattening was needed (`ash_disk`, `magma_disk` radius; `volcano_fossil` y_spread).
  Structures / structure sets / template pools / biomes / dimension types / damage type: key-for-key compared against vanilla
  1.21.1 — no other format change applies to our files.
- **Assets**: no change needed. All vanilla model/texture references resolve in 1.21.1 assets; the only model loader is
  `fossil:plant_loader` (custom, kept); no `forge:` loaders; lang/sounds/particles/geo/animations unchanged
  (GeckoLib 1.21.1 still loads `geo/` and `animations/`). Armor textures stay at `assets/minecraft/textures/models/armor/bone_layer_{1,2}.png`
  (core's `ModArmorMaterials` uses `ResourceLocation.withDefaultNamespace("bone")` for the layer).

## Custom JSON shapes for core (codecs must read exactly this)

### `fossil:culture_vat`, `fossil:worktable` (WithFuelRecipe, 152 files)
```json
{
  "type": "fossil:culture_vat",
  "duration": 6000,                       // int, optional (serializer default)
  "fuel_duration": 123,                   // int, optional (serializer default) — not present in current data
  "fuel":  { "item": "fossil:bio_goo" },  // Ingredient.CODEC_NONEMPTY
  "input": { "item": "fossil:edaphosaurus_dna" },  // Ingredient.CODEC_NONEMPTY
  "result": { "id": "fossil:egg_item_edaphosaurus" }  // ItemStack.CODEC (count optional, default 1; components optional)
}
```
Suggested: `Ingredient.CODEC_NONEMPTY.fieldOf("input")`, `...fieldOf("fuel")`, `ItemStack.CODEC.fieldOf("result")`,
`Codec.INT.optionalFieldOf("duration", default)`, `Codec.INT.optionalFieldOf("fuel_duration", default)`.
(1.20.1 read `result` as a plain item-id string; it is now an ItemStack object like vanilla cooking.)

### `fossil:analyzer`, `fossil:sifter` (MultiOutputAndSlotsRecipe, 38 files)
```json
{
  "type": "fossil:analyzer",
  "input": { "item": "minecraft:glow_ink_sac" },   // Ingredient (object, or array — Ingredient.CODEC accepts both)
  "outputs": [
    { "id": "fossil:glow_squid_dna", "weight": 40.0 },
    { "id": "minecraft:string", "count": 3, "weight": 60.0 }   // count optional (analyzer/wool.json uses count 3)
  ],
  "total": 100.0                                    // double, optional; default = sum of weights
}
```
Each output entry = ItemStack fields (`id`, optional `count`, optional `components`) **flat** + `"weight"` (double).
Suggested codec: `RecordCodecBuilder.create(i -> i.group(ItemStack.CODEC... )` is not flat-mappable, so either
`ItemStack.MAP_CODEC` (if present in 1.21.1 mappings) + `Codec.DOUBLE.fieldOf("weight")`, or explicit fields
`BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id")`, `ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1)`,
`DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)`, `Codec.DOUBLE.fieldOf("weight")`.
Keep the 1.20.1 semantics: running total → NavigableMap; `total > sum` adds an AIR (empty) entry; warn if smaller.

### `create:crushing` (7 files, `recipe/crushing/`, only loaded when Create is present)
`{"neoforge:conditions":[{"type":"neoforge:mod_loaded","modid":"create"}], "type":"create:crushing", "ingredients":[{"item":..}],
"processing_time":250, "results":[{"chance":0.2,"id":".."}, ...]}` — Create 6 / 1.21.1 format (verified against Create's generated data).

### Advancement triggers (`fossil:*`)
- `fossil:implant_embryo`: `{"item": <ItemPredicate 1.21 format>}` e.g. `{"item": {"items": ["fossil:syringe_bat"]}}`; optional `"player"`.
  Suggested: `EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")`, `ItemPredicate.CODEC.optionalFieldOf("item")`.
- `fossil:incubate_egg`: `{"entity": [{"condition":"minecraft:entity_properties","entity":"this","predicate":{"type":"fossil:alligator_gar"}}]}`;
  suggested `EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity")`.
- `fossil:open_sarcophagus`, `fossil:scarab_tame`: `"conditions": {}` → only optional `"player"`.

### Loot function `fossil:customize_to_dinosaur` (63 uses)
`{"function": "fossil:customize_to_dinosaur", "entity": "this"}` (+ optional `"conditions"`).
Suggested `MapCodec`: `commonFields(i).and(LootContext.EntityTarget.CODEC.fieldOf("entity"))`.

### Data-driven registries now required (static JSON added by me, see below)
- `data/fossil/enchantment/{archeology,paleontology}.json` — **required**: 7 fossil block loot tables (fossil ore variants, 42 match_tool conditions) reference
  `fossil:archeology`/`fossil:paleontology` in match_tool predicates; in 1.21 a missing enchantment makes those loot tables fail to load.
- `data/fossil/jukebox_song/{bones,anu,scarab,discovery}.json` — matches core's `ModJukeboxSongs` keys.

## Manual additions / edits (not done by the script)

| File | What |
|---|---|
| `common/src/main/resources/data/fossil/enchantment/archeology.json`, `paleontology.json` | 1.21 enchantment definitions equivalent to the old classes: weight 1, anvil_cost 8 (VERY_RARE), max_level 3, min_cost 1+10/lvl, max_cost 51+10/lvl, slots mainhand, supported_items `#minecraft:enchantable/mining_loot` (= old DIGGER category = DiggerItem), exclusive_set `[minecraft:silk_touch, <other one>]`, no effects (behaviour lives in the loot tables). Identical values to core's `ModEnchantments.bootstrap`. |
| `common/src/main/resources/data/minecraft/tags/enchantment/non_treasure.json` | adds both → enchanting table, random loot, trades, mob/trader equipment (old: not treasure, discoverable, tradeable). |
| `common/src/main/resources/data/fossil/jukebox_song/*.json` | bones 196 s / comparator 0, anu 221/1, scarab 153/2, discovery 250/3 (old RecordItem values); description `item.fossil.music_disc_<x>.desc` (existing lang key); sound `fossil:music_<x>`. |
| `neoforge/src/main/resources/data/c/tags/item/foods/cooked_fish.json` | was found **empty** (0 bytes, mtime not from my script runs — likely a concurrent write by another agent); restored to the converted content. |
| `tools/port/migrate_data.py` | the script. |

## PORT-TODO / uncertainties

1. **Custom recipe codecs** (core): shapes above are my proposal; if core chooses a different shape, change `convert_recipe()` (FOSSIL_WITH_FUEL / FOSSIL_MULTI_OUTPUT branches) and re-run on a pristine export, or patch the JSON by hand. Highest-risk item: until both sides agree, all 190 machine recipes fail to parse.
2. **Duplicate resources after datagen**: once datagen emits `fossil/enchantment/*.json` / `fossil/jukebox_song/*.json` into `common/src/generated`, delete my static copies in `common/src/main/resources/data/fossil/{enchantment,jukebox_song}` (and the `non_treasure` tag if datagen produces it), otherwise processResources sees duplicate paths.
3. `c:axes`, `c:hoes`, `c:pickaxes`, `c:shovels`, `c:swords` (generated) are old Fabric-convention names that no longer exist in NeoForge 1.21 conventions (tools are `#minecraft:swords` etc., already populated by us). Kept as-is because `ModItemTags.C_*` still references them; harmless but useless — see requests.
4. `c:buckets/entity_water` now also contains vanilla fish buckets (NeoForge's own tag), so `#fossil:filter_buckets` (creative tab filter) gets them too; on 1.20.1 Forge `c:water_buckets` held only our buckets. Probably irrelevant (filter applies to our tab).
5. Structure `.nbt` files are DataVersion 2975 (1.18.2); vanilla DFU upgrades them on load. Fossil block entities inside them carry no item stacks (checked), so no custom item-NBT upgrade problem. Pre-existing bug kept: `structure/anu_castle_nw.nbt` has a chest with `LootTable: "minecraft:chests/anu_castle"` (should be `fossil:`).
6. Pre-existing: `neoforge/.../advancement/recipes/fossil.fa_item_tab/bio_goo_from_*.json` reward recipe ids `fossil:bio_goo_from_*_forge` that don't exist (harmless: missing reward recipes are ignored).
7. `minecraft:grass`→`short_grass` was renamed in `food/plant.json` (custom format read by our Java) and two analyzer outputs; `minecraft:scute`→`turtle_scute` in `recipe/analyzer/scute.json` (the file name keeps `scute`). Datagen providers must use `Items.SHORT_GRASS` / `Items.TURTLE_SCUTE`.
8. Loot `fossil:archeology`/`paleontology` match_tool uses exact `"levels": 1|2|3` (as in 1.20.1); levels >3 from commands wouldn't match (unchanged behaviour).
9. `prehistoricfauna` compat tags were only folder-renamed; whether Prehistoric Fauna exists for 1.21.1 / uses the same ids is unknown.

## Requests to other owners

### core
- `tags/ModItemTags.java`: `C_WATER_BUCKETS` → `c:buckets/entity_water` (or `Tags.Items.BUCKETS_ENTITY_WATER`); `C_SWORDS/C_PICKAXES/C_AXES/C_SHOVELS/C_HOES` → use vanilla `ItemTags.SWORDS` … (then I/datagen can drop the `c:axes` … files); `C_FOODS`, `C_GLASS_BLOCKS` (+ `ModBlockTags.C_GLASS_BLOCKS`) names are still valid.
- Recipe serializers (MapCodec/StreamCodec) for culture_vat/worktable/analyzer/sifter reading the JSON shapes above.
- Trigger codecs for implant_embryo/incubate_egg/open_sarcophagus/scarab_tame and the `customize_to_dinosaur` loot function codec as above.
- Any Java hard-coded resource path with the old plural folders (none found by grep in common/forge Java).

### build (neoforge/)
- `neoforge/src/main/resources/pack.mcmeta`: the `forge:resource_pack_format`/`forge:data_pack_format` keys are dead. NeoForge 21.1 reads mod `pack.mcmeta` optionally (`ResourcePackLoader.readWithOptionalMeta`), so either delete it or use
  `{"pack": {"description": "${mod_id} resources", "pack_format": 48, "supported_formats": [34, 48]}}` (48 = data, 34 = resources for 1.21.1).
- I converted `neoforge/src/main/resources/data/**` in place (4 recipes, 4 advancements, 9 structure nbt, 4 tags now under `data/c/tags/item/`). Please do not re-copy `forge/src/main/resources/data` over it; delete `forge/` data when forge/ is removed.
- Datagen providers (phase 2) must emit the new formats/names: `ModItemTagsProvider` (`forge:*` → `c:*` names above, `c:buckets/entity_water`), `ModRecipeProvider` (NeoForge `IConditionBuilder`/`neoforge:mod_loaded` for farmersdelight/create recipes; Create crushing via Create 6 format; `Tags.Items.GLASS_BLOCKS`, `Tags.Items.EGGS`), biome modifiers registered under `NeoForgeRegistries.Keys.BIOME_MODIFIERS` (`neoforge:add_features`/`add_spawns`), enchantments + jukebox songs via `RegistrySetBuilder` (core provides `ModEnchantments.bootstrap` / `ModJukeboxSongs.bootstrap`), `Items.SHORT_GRASS`, `Items.TURTLE_SCUTE`.
- Before re-running datagen in phase 2: wipe `common/src/generated/resources/{data,assets}` (see "How to re-run").

### world
- Custom configured features (`fossil:ask_disk` radius) and placed features now carry flattened providers (`{"type":"minecraft:uniform","min_inclusive":4,"max_inclusive":6}`); feature config codecs must use `IntProvider.CODEC` / `IntProvider.codec(min,max)` (they will, if they use the vanilla codecs). Dimension `fossil:anu_lair` / `fossil:treasure_room` chunk generators and structures `fossil:anu_castle|hell_boat|treasure_room` JSON unchanged.

### client
- Block models keep `"loader": "fossil:plant_loader"` (23 models); register the geometry loader under that id in `ModelEvent.RegisterGeometryLoaders`.
