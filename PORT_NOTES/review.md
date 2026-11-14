# Integration review (1.21.1 NeoForge port, after phase 1b)

Reviewer scope: mismatches **between** the parts written by the seven agents (core, entity-a, entity-b, client, world, compat, build,
data). I did not write any of the port. Nothing was compiled with Gradle; no git commit was made.

## How it was checked

1. **javac attribution against reference sources** (the strongest check, `tools/port/javac_xref.py`).
   All 686 project files were type-checked by `javac` with the dependency **sources** on `-sourcepath`:
   - decompiled vanilla 1.21.1 (`extremeheat/extracted_minecraft_data`, branch `client1.21.1`, sparse clone in
     `refs/vanilla-1.21.1/client`),
   - NeoForge 21.1, FML loader, Architectury 13 (common + neoforge), GeckoLib 4.9.3, TerraBlender, JEI/REI/EMI/CarryOn/FD/Jade,
     More Hitboxes 1.21.1, ClothConfig,
   - library sources I cloned into `refs/libs/`: DFU, brigadier, Mixin, MixinExtras, JetBrains annotations, Gson, Guava, JOML,
     neoforged Bus, Netty (buffer/common/transport/codec), commons-lang3, slf4j, architectury-injectables.

   The decompiled vanilla contains `// $FF: synthetic method` bridge methods that break overload resolution
   (`TamableAnimal#level()` returning `EntityGetter`, `Vec3i`-returning `BlockPos` bridges), so a copy without them is used
   (`javac_xref.py --strip-bridges`). The result is about 400 errors. All of them fall into known categories. The script counts
   these and prints only the rest:

   | Category (not a bug) | Count |
   |---|---|
   | Library jars without usable sources (fastutil, lwjgl, authlib, log4j, asm, …) | 205 |
   | Architectury interface injection (`arch$tab`, `arch$registryName`) | 72 |
   | Access opened by `fossil.accesswidener`, Architectury's transitive AW or the NeoForge AT (each entry checked against vanilla, see 2.4) | 63 |
   | NeoForge patch or extension members (`IAttachmentHolder#getData`, `IBlockExtension#isFlammable/getToolModifiedState`, `Item#initializeClient`, `DataGenerator#addProvider/getPackOutput`, tag/loot/recipe datagen helpers + NeoForge AT, `BakedModel#getTransforms` default). Each one was checked by hand against `refs/NeoForge/patches` and the AT | 57 |
   | Errors that follow from the decompiled `ExtraCodecs`/`TagsProvider` failing to parse | 3 |
   | EMI refs are Yarn-mapped (not comparable) | 2 |
   | **Actionable** | 7 before the fixes, **0 after** |

2. **Regex symbol index** (`tools/port/xref_index.py`, the requested symbol-index script). It indexes 685 classes and 4679 method declarations, then checks:
   - imports of project classes that no longer exist: 0
   - each `@ExpectPlatform` method (30 of them) against `neoforge/.../forge/<Class>Impl`, comparing name, parameter types, return type and that the Impl method is static: all 30 match. The remaining Impl statics are helpers.
   - mixin config entries against the mixin classes: all match. `@Shadow`, `@Accessor` and injector method names are checked against the vanilla 1.21.1 sources.
   - `Class.method(...)` calls on project classes, by name and arity: 0 mismatches (the javac pass covers this more thoroughly).
3. **Manual checks** of every mixin target against vanilla 1.21.1, the NeoForge patches and GeckoLib 4.9.3. Data and code consistency was checked by scripts (section 3).

## Findings by severity

### Would not compile (all fixed)
| # | File:line | Problem | Fix |
|---|---|---|---|
| C1 | `common/.../event/ModEvents.java:29` | `import net.minecraft.core.BlockSource` – class moved to `net.minecraft.core.dispenser` (1.20.3). (entity-b's `ThrownBirdEgg` already used the new package, core's file did not.) | import changed |
| C2 | `common/.../material/TarFluid.java:43` | `new AABB(BlockPos, BlockPos)` removed in 1.20.5 (vanilla 1.21.1 has only `AABB(BlockPos)`, `AABB(Vec3,Vec3)`, 6 doubles) | `new AABB(pos)`, the identical unit box |
| C3 | `common/.../block/custom_blocks/CultureVatBlock.java:70` | same `AABB(BlockPos, BlockPos)` | `new AABB(Vec3.atLowerCornerOf(a), Vec3.atLowerCornerOf(b))` (same corners) + `Vec3` import (line 41) |
| C4 | `common/.../entity/monster/Anubite.java:114` | `DamageSource#isIndirect()` no longer exists (1.21.1 has `isDirect()`) | `!source.isDirect()` (1.20.1 `isIndirect` = `causingEntity != directEntity`, 1.21.1 `isDirect` = `==`) |
| C5 | `common/.../entity/ai/navigation/TrilobitePathNavigation.java:29` | `PathNavigation#isInLiquid()` was removed. Vanilla navigations now call `mob.isInLiquid()` | `mob.isInLiquid()` (Entity: water/bubble or lava, same check) |
| C6 | `neoforge/.../forge/data/providers/ModItemProvider.java:236` | `public void spawnEggItem(Item)` clashes with NeoForge 21.1 `ItemModelProvider#spawnEggItem(Item)`, which returns `ItemModelBuilder` ("return type void is not compatible") | now `@Override public ItemModelBuilder spawnEggItem(Item)`, same model output |

Unchanged since 1.20.1 and missed by the owners: C1–C5. C6 only shows up against the real NeoForge source.

### Would crash at startup / on first use (fixed)
| # | File:line | Problem | Fix |
|---|---|---|---|
| S1 | `neoforge/.../forge/mixin/WalkNodeEvaluatorMixin.java:21` | `@Inject(method = "isBurningBlock")` on `WalkNodeEvaluator`. In 1.21.1 `isBurningBlock(BlockState)` is a **static method of `NodeEvaluator`** (vanilla `NodeEvaluator:104`; `WalkNodeEvaluator` only calls it, and NeoForge does not patch it). With `defaultRequire = 1` the mixin fails when `WalkNodeEvaluator` is loaded (first pathfinding), which is a crash. The file's PORT-TODO assumed the method still existed. | Injector moved unchanged into the new `neoforge/.../forge/mixin/NodeEvaluatorMixin.java` (targets `NodeEvaluator`, first `BlockState#is(TagKey)` = `BlockTags.FIRE`). Added to `fossil_forge.mixins.json`. The old injector is kept commented. `findAcceptedNode`/`canReachWithoutCollision` is still valid and remains in `WalkNodeEvaluatorMixin` |

### Wrong behaviour / runtime risk (not fixed, design decision or needs a game)
| # | Where | Finding | Suggestion |
|---|---|---|---|
| W1 | `item/JavelinItem.java` (`JavelinProperties`, 16 stack + `MAX_DAMAGE`) | Vanilla 1.21.1 `ItemStack.validateComponents` rejects **MAX_DAMAGE + MAX_STACK_SIZE > 1** ("Item cannot be both damageable and stackable"). This is used by `STRICT_CODEC` (crafting/stonecutting/smithing results, advancement icons), by `applyComponentsAndValidate`, and by `ServerboundSetCreativeModeSlotPacket` (`validatedStreamCodec`). Expected effect: **moving a javelin in the creative inventory fails to decode on the server, so the player is disconnected**. Our data is fine: javelins only appear as worktable results, which use `ItemStack.CODEC` without validation, and in no advancement icon. Also, `isDamageableItem()` is false until a `DAMAGE` component exists, so Unbreaking/Mending and the anvil treat fresh javelins as not damageable. | Phase 2 design decision: max stack 1 (like the trident), or keep the stacking with a custom damage component instead of `MAX_DAMAGE`. Test creative first (checklist item 6). |
| W2 | `forge/data/providers/ModEntityTypeTagsProvider.java:56` | javac reports `appender.add(ResourceKey<EntityType<?>>)` as "no suitable method". I traced this to the decompiled `TagsProvider.java` failing to parse. A standalone reproduction of the same class shape compiles, so it is most likely **not** a real error. It is datagen only. | If the first build complains: `TagsProvider.TagAppender<EntityType<?>> appender = tag(key);` |
| W3 | `mixin/JigsawPlacementMixin` (core PORT-TODO) | Diffed against vanilla 1.21.1 `Placer#tryPlacingChildren`. The field names `pools/maxDepth/chunkGenerator/structureTemplateManager/pieces/random/placing`, `readPoolKey(info, PoolAliasLookup)`, the `PieceState` record ctor, `encapsulate` and the placement priority all match. **No issue.** The PORT-TODO can be closed. | – |
| W4 | `Failuresaurus#getCurrentSwingDuration` is `protected` | The NeoForge AT makes `LivingEntity#getCurrentSwingDuration` **public**. Common is compiled against the AW (protected), so it compiles, and the JVM allows the weaker override. | Harmless. Optionally make it `public` |
| W5 | `neoforge.mods.toml` | Optional dependency entries for FD add-ons whose compat is disabled (`argentinas_delight`, `delightful`, …), with old 1.20.1 lower bounds. They are harmless because the ranges are open-ended. `geckolib` range `[4.5,)` while the code was written and checked against 4.9.3 only. `neoforge [21.1.0,)` while the build uses 21.1.266. | Consider raising the lower bounds to the tested versions |

### Cosmetic / housekeeping
- **Stray files committed** (javac argument files left by agents): `javac.20261114_172256.args` (repo root),
  `common/src/main/java/javac.20261114_165622.args`, `neoforge/src/main/java/javac.20261114_164758.args`,
  `neoforge/src/main/java/javac.20261114_164803.args`. Not compiled or packaged (not `.java`, not in resources). Delete them.
- `fossil.accesswidener`: `MenuScreens#register` and `MenuScreens$ScreenConstructor` are unused now (client uses Architectury `MenuRegistry`). They are valid in 1.21.1, so harmless.
- `WalkNodeEvaluatorMixin` now has unused imports (`Inject`, `CallbackInfoReturnable`, `ModBlocks`, `BlockState`), left because the commented code uses them.
- `common/src/generated/resources/.cache` is packaged into the jar. This was already the case on 1.20.1.

## Checks that found no problems
### 2.1 Cross-package API (core ↔ entity ↔ client ↔ world ↔ compat ↔ build)
- Every request in the notes files was applied consistently:
  - AW `Entity.eyeHeight` and `AbstractArrow.setPierceLevel`
  - leash hook in `ModEvents` (`INTERACT_ENTITY` + `canBeLeashedBy`)
  - `ModStructureType.register()` in `FossilMod.init()`
  - `AnuLair.FACTORY` used by AnuBoss, ExplosionMixin and ModEvents
  - `C2SStructureMessage` `finalizeSpawn` arity
  - `PathingDebug(PathType)`
  - `fixTransitions`/`tickAdditiveAnimations` parameter types
  - `fossil$isFirstTick`
  - `getScale` / GeckoLib double scaling
  - `Javelin#setKnockback`
  - `ThrownBirdEgg` → `DispenseItemBehavior`
  - `JUKEBOX_SONG` and `ENCHANTMENT` bootstraps
  - recipe `RecipeHolder` unwrapping in JEI, REI and EMI
  - `S2CMammalCapMessage` / `CAP_CHANNEL` in the build Impls
- `@ExpectPlatform`: 30/30 match the Impl (name, parameters, return type, static). The deleted `AncientHelmetItemImpl` and `CustomRecordItemImpl` have no remaining `@ExpectPlatform`.
- No duplicate fully qualified class names between common and neoforge. No import of a removed project class.
- Event buses: every `modEventBus.addListener` takes an `IModBusEvent`. `ForgeModEvents#registerTests` is correctly kept off the game bus.
  - `@EventBusSubscriber` classes: Bus.MOD for `RegisterEvent`/`GatherDataEvent`, the game bus for everything else.

### 2.2 Mixins (each target checked against vanilla 1.21.1, the NeoForge patches and GeckoLib 4.9.3)
- AbstractFish `aiStep`/`onGround`; Explosion `explode`/`ObjectArrayList.addAll` (still present after NeoForge's patch)
- Mob `serverAiStep` (`tick`, `tickRunningGoals(Z)`, `MoveControl/LookControl.tick`); NaturalSpawner `getRandomPosWithin`; ServerEntity `sendChanges` (accept ordinal 0 = `ClientboundSetPassengersPacket`)
- PlayerInfo `getSkin`; TerrainParticle ctor with `BlockPos`; Camera `setup`/`move(FFF)`; LevelRenderer `renderLevel(DeltaTracker,…)` plus the shadows
- Minecraft `disconnect(Screen,Z)`/`pickBlock`/`startUseItem`/`startAttack`/`continueAttack`/`rightClickDelay`; MouseHandler `onScroll`; Particle `gravity`; PanoramaRenderer `render`
- `ChunkStatusTasks.generateStructureStarts`; JigsawStructure fields and `addPieces` arity; SynchedEntityData `itemsById`/`isDirty`
- GeckoLib: `AnimatableManager.isFirstTick`, `AnimationController` fields/`process`/`MathParser.setVariable(String, DoubleSupplier)`, `AnimationProcessor` `bones/model/reloadAnimations/tickAnimation`, `GeoEntityRenderer.actuallyRender(...ZFIII)V` + `animatable`
- CarryOn `require = 0`
- Only `WalkNodeEvaluatorMixin` was wrong (S1)

### 2.3 Data ↔ code
- **Machine recipes**: all 190 files (`analyzer` 36, `sifter` 2, `culture_vat` 122, `worktable` 30) match the MapCodecs:
  - `input`/`outputs[{id,count∈[1,99],components?,weight}]`/`total?`
  - `input`/`fuel`/`result{id,count?}`/`duration?`/`fuel_duration?`
  - no unknown keys
- **Advancement triggers**: `fossil:implant_embryo` (26, `item`), `incubate_egg` (58, `entity`), `open_sarcophagus`, `scarab_tame` are registered under the same ids in `ModTriggers` (DeferredRegister on `TRIGGER_TYPE`). The condition keys match the codecs.
- **Loot**: `fossil:customize_to_dinosaur` (63 uses, `"entity": "this"`) matches the registered `LootItemFunctionType` + MapCodec. The `minecraft:dynamic` `fossil:bones_*` entries match the `withDynamicDrop(ModItemTags.*_BONES.location())` ids. `fossil:archeology`/`paleontology` exist as data-driven enchantments (and lang keys).
- **Tags**:
  - Every `TagKey` declared in Java (fossil and `c:`) exists as a JSON file in our resources or in NeoForge's generated `c:` tags.
  - Every `#tag` / `"tag"` reference in data JSON resolves against our data, vanilla 1.21.1 or NeoForge. No foreign namespaces appear in tag values.
  - `minecraft:aquatic/undead/arthropod/can_breathe_under_water` entity tags exist (16/1/7/21 entries).
- **Worldgen**:
  - The `fossil:` feature types in configured features (`ask_disk`, 6 trees, `moai_statue`, `volcano_cone`, `volcano_vent`) are registered by `ModFeaturesImpl` (RegisterEvent).
  - The structure types `anu_castle`, `hell_boat` and `treasure_room` are registered by `ModStructureType`, and the chunk generators `anu_lair` and `treasure_room` in `RegisterEvent`.
  - All 14 `PlacedFeature` keys in Java have JSON.
  - The biome modifiers use `neoforge:add_spawns/add_features` and reference existing placed features.
- **Jukebox songs** reference registered sound events.
- **Registry names** of items, blocks, block entities, menus, sounds, particles, effects, fluids, recipes and tabs are identical to 1.20.1 (string diff), so the converted data still points at real objects.
- **mods.toml**: the `[[mixins]]` entries for both configs are present, and the expanded properties all exist in `expandProps`. The default `META-INF/accesstransformer.cfg` is picked up by FML 4 without a `[[accessTransformers]]` entry (checked in the FML 1.21.1 source `ModFile`).

### 2.4 Access widener
All 22 entries were checked against vanilla 1.21.1. Each target exists with that exact descriptor, which matters because Loom fails on a missing member:
- `PieceState` is a record with ctor `(PoolElementStructurePiece, MutableObject, I)`
- `Entity.dimensions` and `Entity.eyeHeight` are private and non-final
- `getCurrentSwingDuration` and `setPierceLevel(B)V` are private
- `getEntryAtPosition` is `protected final`
- …

Every private or protected access that javac reported is covered by:
- our AW;
- Architectury's transitive AW: block ctors, `MenuSupplier`, `BlockEntitySupplier`;
- or, on neoforge, the NeoForge AT: `BlockSetType/WoodType.register`, `goalSelector`, `createSilkTouchOnlyTable`, `inventoryTrigger`.

### 2.5 Registration timing
- **No direct `Registry.register` into vanilla registries**. All 18 DeferredRegisters, plus `ModAttachments`, are registered from `FossilMod.init()` or the mod constructor:
  - chunk generators in `RegisterEvent`
  - structure types and triggers in init. Their repeat calls in common setup are idempotent no-ops.
- **Common-setup code only appends to lists**: `ModPlacedFeatures.register()` → `BiomeModifications`; TerraBlender `Regions`/`SurfaceRuleManager` are called inside `enqueueWork`.
- **Registration order**: data components and armor materials come before items (NeoForge `GameData` ordering). The `ArmorItem` holder is only resolved lazily (memoized).
- **Config reads**: none during registration. They are all inside runtime methods or lambdas (`ForgeFossilRegion#getWeight`, the biome-modification lambda, capability providers).
- **Client-only classes reachable from common**: only `AncientHelmetItem` is new compared with 1.20.1. It is GeckoLib's `createGeoRenderer` pattern, and the anonymous provider class is only loaded on the client.
- `ClientInit.immediate()` does no `RegistrySupplier#get()`. Menu screens go through `listen` → Architectury `RegisterMenuScreensEvent`.

### 2.6 Duplicate resources
- No identical path, including case-insensitive matches, exists in more than one of `common/src/main/resources`, `common/src/generated/resources` and `neoforge/src/main/resources`.
- No leftover plural 1.20 folders. The remaining `advancement/recipes` and `loot_table/blocks` are legitimate sub-folders.
- Known future duplicate (data notes): once datagen emits `fossil/enchantment/*` and `fossil/jukebox_song/*`, delete the static copies in `common/src/main/resources`.

## Fixed (file:line, all marked `// PORT(1.21.1-review)`)
- `common/src/main/java/com/github/teamfossilsarcheology/fossil/event/ModEvents.java:29`
- `common/src/main/java/com/github/teamfossilsarcheology/fossil/material/TarFluid.java:43-44`
- `common/src/main/java/com/github/teamfossilsarcheology/fossil/block/custom_blocks/CultureVatBlock.java:41,70-71`
- `common/src/main/java/com/github/teamfossilsarcheology/fossil/entity/monster/Anubite.java:114`
- `common/src/main/java/com/github/teamfossilsarcheology/fossil/entity/ai/navigation/TrilobitePathNavigation.java:29`
- `neoforge/src/main/java/com/github/teamfossilsarcheology/fossil/forge/data/providers/ModItemProvider.java:236-241`
- `neoforge/src/main/java/com/github/teamfossilsarcheology/fossil/forge/mixin/WalkNodeEvaluatorMixin.java:21-30` (injector commented out, moved)
- `neoforge/src/main/java/com/github/teamfossilsarcheology/fossil/forge/mixin/NodeEvaluatorMixin.java` (new)
- `neoforge/src/main/resources/fossil_forge.mixins.json:10` (`NodeEvaluatorMixin` added)

After the fixes the javac attribution pass reports 0 actionable errors (it was 7). `javac -XDshould-stop.ifError=PARSE` on all changed
files is clean, and the mixin JSON parses.

## Left for phase 2 (with reasoning)
- W1 javelin stackable + damageable: a gameplay decision (stack size vs durability). It must be tested in creative before release.
- W2: probably a false positive. Fix only if Gradle complains.
- The datagen providers (`neoforge/.../forge/data/**`) are the least verified code. javac can only check them partially because the decompiled
  `TagsProvider`/`RegistrySetBuilder` do not parse. Expect the first `runData` to need small fixes.
- Behaviour items owned by the agents remain open (see their PORT-TODOs): dino hitbox/eye height, riding offsets, `NbtCondition`
  variants from `ENTITY_DATA`, the anu-lair teleport, the stone tablet bounding box, flight pathing, the REI scrollbar.
- Stray `javac.*.args` files: delete when committing.

## First Gradle build + first in-game test: prioritized checklist
1. `./gradlew :common:compileJava :neoforge:compileJava`.
   - Expected friction: artifact coordinates (More Hitboxes, EMI `emi-xplat-intermediary`, `geckolib-common-1.21.1`), then datagen classes.
   - Check that Loom applies `fossil.accesswidener` in `:neoforge` dev runs (otherwise `Entity.eyeHeight`/`dimensions` and `setPierceLevel` fail).
2. `./gradlew :neoforge:runData`, after wiping `common/src/generated/resources/{data,assets}`.
   - Then remove the static `fossil/enchantment`, `fossil/jukebox_song` and `minecraft/tags/enchantment/non_treasure` duplicates if datagen produced them.
   - `git diff` the regenerated JSON against the script-converted JSON (they should be nearly identical).
3. `runClient` → check the log for:
   - mixin errors (`InvalidInjectionException`, "No refMap", "@Shadow field not found"), especially NodeEvaluatorMixin, WalkNodeEvaluatorMixin, JigsawPlacementMixin, ServerEntityMixin, GeoEntityRendererMixin, AnimationController/ProcessorMixin;
   - `RegisterPayloadHandlersEvent` duplicates;
   - registry errors.
4. Create a world → datapack log: **no** "Couldn't parse"/"Failed to load" for:
   - `fossil:` recipes (190 machine and 400+ vanilla-type);
   - advancements;
   - loot tables (especially the 7 fossil ores with enchantment predicates);
   - tags, enchantments, jukebox songs, worldgen, dimension types.
5. Machines: put items into the analyzer, sifter, culture vat and worktable by hand and with hoppers. Check that the JEI/REI/EMI categories show recipes, and test energy mode on/off.
6. **Creative: take a javelin, move it around the inventory, throw it, pick it back up (W1).**
7. Spawn several dinosaurs (eggs, spawn eggs, `/summon`) and check:
   - size, hitbox, sleeping box;
   - riding and flying (pteranodon);
   - taming and leashing (owner only);
   - fish buckets: save/load;
   - Tar pathing: mobs avoid tar, which is the NodeEvaluatorMixin.
8. Start a dedicated server (`runServer`) and join it.
   - This shows client-class leaks and S2C payload registration on a dedicated server: `registerS2CPayloadType`.
   - Check that config, entity info and food mappings sync.
9. Worldgen and dimensions:
   - the volcano biome (TerraBlender), moai, villages (archeologist/paleontologist houses), hell boat;
   - the anu lair via the sarcophagus: castle, boss, barriers, explosion protection;
   - the treasure room; HomePortal back.
10. Saved data: reload the world with the anu lair, pregnant mammals (attachments), placed machines and javelins.

## Tools / refs added
- `tools/port/javac_xref.py`: the javac attribution pass with the false-positive classifier. Run `--strip-bridges` first, then pass a sourcepath file.
- `tools/port/xref_index.py`: the regex symbol index with the import, `@ExpectPlatform`, mixin and call/arity checks. Use `--vanilla <dir>` for the @Shadow/@Accessor checks.
- `refs/vanilla-1.21.1/` (decompiled vanilla client sources) and `refs/libs/*` (library sources), all read-only.
