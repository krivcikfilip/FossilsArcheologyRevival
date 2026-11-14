# world agent notes (1.20.1 → 1.21.1 NeoForge)

Scope: `common/src/main/java/com/github/teamfossilsarcheology/fossil/world/**` (paths below are relative to that folder).
Nothing was compiled. A `javac` parse of every file in world/ passes (only missing-symbol errors, as expected without Minecraft on the classpath).

## What changed (per area)

**Datagen bootstraps.** Every `bootstrap(BootstapContext<T>)` now takes `net.minecraft.data.worldgen.BootstrapContext<T>`. That covers
ModBiomes, ModDimensions, ModOreFeatures, ModConfiguredFeatures, ModOrePlacements, ModPlacedFeatures, ModStructures (and the private
`sites`/`simple` helpers), ModStructureSets and ModTemplatePools. Method names and keys are unchanged, so the `ModWorldGenProvider` method refs still fit.

**Dimensions** (`dimension/ModDimensions`)
- `changeDimension` stays `@ExpectPlatform`. Its signature is now `changeDimension(Entity, ServerLevel, DimensionTransition)`, which matches the
  existing neoforge `ModDimensionsImpl`.
- `teleportToAnuLair(MinecraftServer, Entity)` keeps its signature. It builds
  `new DimensionTransition(anuLair, spawnPoint, entity.getDeltaMovement(), 0, 90, DimensionTransition.DO_NOTHING)`, which is the old
  `PortalInfo(spawnPoint, speed, 0, 90)`. `DO_NOTHING` matches the old Forge `ModTeleporter`: no sound, no portal placement.
- `DimensionType CUSTOM` is unchanged. The record constructor is the same in 1.21.1, and the generated dimension_type JSON matches it.

**Chunk generators** (`chunk/`)
- `TreasureChunkGenerator.CODEC` and `AnuLairChunkGenerator.CODEC` are now `MapCodec` (`RecordCodecBuilder.mapCodec`, still
  `RegistryOps.retrieveElement`). `codec()` returns `MapCodec<? extends ChunkGenerator>`. The class names are kept for core's `ChunkStatusMixin`.
- `SingleStructureChunkGenerator.fillFromNoise(Blender, RandomState, StructureManager, ChunkAccess)`: the `Executor` parameter was removed in 1.21.
  `applyCarvers(..., GenerationStep.Carving)`, `buildSurface`, `createStructures(RegistryAccess, ChunkGeneratorStructureState, StructureManager,
  ChunkAccess, StructureTemplateManager)`, `applyBiomeDecoration`, `getBaseColumn`, `addDebugScreenInfo`, `getBaseHeight`, `getMinY`,
  `getSeaLevel`, `getGenDepth` and `spawnOriginalMobs` are unchanged in 1.21.1. `Structure#generate(...)` also keeps its 10-argument form.
- Dimension JSON (`"generator": {"type": "fossil:anu_lair", "biome_source": ...}`) still parses. The MapCodec ignores `biome_source`, as before.

**Structures** (`feature/structures/`)
- `AnuCastleStructure.CODEC`, `HellBoatStructure.CODEC` and `TreasureRoomStructure.CODEC` are now `MapCodec`.
- `ModStructureType` is rewritten as an Architectury `DeferredRegister<StructureType<?>>` on `Registries.STRUCTURE_TYPE`.
  `ANU_CASTLE`, `HELL_BOAT` and `TREASURE_ROOM` are now `RegistrySupplier<StructureType<X>>`, and `type()` returns `.get()`. The ids are unchanged
  (`fossil:anu_castle`, `fossil:hell_boat`, `fossil:treasure_room`).
  The old static `Registry.register` ran during common setup, but on NeoForge the vanilla registries are frozen by then.
  `register()` is idempotent (guard flag). **It must be called first from the mod constructor**, as described in the requests below.
- `AnuCastleStructure` (a custom jigsaw copy):
  - `PoolElementStructurePiece` gets the 1.21 `LiquidSettings` argument. It uses `APPLY_WATERLOGGING`, which is both the vanilla
    `JigsawStructure` default and the 1.20.1 behaviour.
  - `new ResourceLocation(String)` became `ResourceLocation.parse`.
  - The castle uses its own `Placer`, not `JigsawPlacement$Placer`, so no AW entry is needed. Core's remark about `placing` does not apply.
  - `getShuffledJigsawBlocks`, `JigsawBlock.canAttach(StructureBlockInfo, StructureBlockInfo)` and `JigsawJunction` are unchanged in 1.21.1.
    `JigsawBlockInfo` only arrives in 1.21.2.
- `ModStructures`: the `JigsawStructure(settings, pool, size, height, expansionHack, Heightmap.Types)` 6-argument constructor still exists in 1.21.1
  (verified). It fills `maxDistanceFromCenter=80`, no aliases, and default padding/liquid settings, the same as vanilla villages.
- `HellBoatPiece` and `TreasureRoomPiece`: the `TemplateStructurePiece` constructors, `addAdditionalSaveData` and `handleDataMarker` are unchanged
  (verified). No `StructureTemplate#placeInWorld` or processor code exists in world/, so no `HolderLookup.Provider` changes were needed.
- `ModStructureSets` (`ConcentricRingsStructurePlacement(int,int,int,HolderSet)`, `RandomSpreadStructurePlacement`) and `ModTemplatePools`
  (`StructurePoolElement.single(String)`) are unchanged apart from `BootstrapContext`.

**Features** (`feature/`)
- `Feature` still takes a `Codec<FC>` in 1.21.1 (verified), so `AshDiskConfiguration.CODEC` and the `NoneFeatureConfiguration.CODEC` users
  are unchanged.
- `AshDiskConfiguration` uses `IntProvider.codec(0, 11)`. It reads the flattened `{"type":"minecraft:uniform","min_inclusive",..}` that the
  data agent produced for `ash_disk`/`magma_disk`. The JSON fields `radius`/`magma` match the codec.
- `ModFeatures` is unchanged. Registration is done by neoforge `ModFeaturesImpl` (RegisterEvent).
- `ModPlacedFeatures.register()` only calls Architectury `BiomeModifications.addProperties`, which appends to a list. That is still safe in
  common setup because no registry write happens. `GenerationProperties.Mutable#addFeature(Decoration, ResourceKey)` and
  `ClimateProperties#getTemperature` were verified in Architectury 13.
- **Tree growers deleted**: `feature/tree/{Calamites,Cordaites,Mutant,Palm,Sigillaria,Tempskya}TreeGrower.java`
  (`AbstractTreeGrower` no longer exists). They are replaced by core's `block/ModTreeGrowers`, and no other references exist in common/ or neoforge/.
  `ModConfiguredFeatures.*_TREE_KEY` names are kept. The tree features (`*TreeFeature`, `CustomTreeFeature`, `TreeBranchLayouts`) needed no change.

**Villages** (`feature/village/ModVillages`): `ResourceLocation.withDefaultNamespace("empty")` and `ResourceLocation.parse(location)`.
`Registry#getHolderOrThrow`, `StructurePoolElement.legacy(String, Holder)` and the AW'd `StructureTemplatePool.templates/rawTemplates` are still
valid in 1.21.1.

**Biomes / surface rules**: only the `BootstrapContext` rename. `ModSurfaceRules` is unchanged (SurfaceRules API is the same, and
TerraBlender 1.21.1 `SurfaceRuleManager.addSurfaceRules(RuleCategory, String, RuleSource)` was verified). The neoforge `ForgeFossilRegion` (build's file) matches
TerraBlender 1.21.1 `Region` (`addBiomes(Registry, Consumer)`, `addModifiedVanillaOverworldBiomes`, `getWeight`).

**Effects**: `ComfyBedEffect` needs no change. It overrides no tick method, and the `protected MobEffect(MobEffectCategory, int)` constructor still exists.
`ModEffects.COMFY_BED` stays `RegistrySupplier<MobEffect>`. Where a `Holder<MobEffect>` is needed, callers wrap it (comment added).

## PORT-TODO list
- `feature/structures/ModStructureType.java:31`: needs `ModStructureType.register()` in `FossilMod.init()` (core request below).
- `dimension/ModDimensions.java:70`: in-game check of the anu lair teleport for players and non-player entities. The call comes from `entityInside`,
  not through the 1.21 `Portal` interface.
- `feature/structures/AnuCastleStructure.java:43`: in-game check that the whole anu castle generates (children, waterlogging).

## Verified against
mappings.dev 1.21.1 (ChunkGenerator, Structure, StructureType, JigsawStructure, PoolElementStructurePiece, StructurePoolElement, JigsawBlock,
DimensionTransition, Feature, RegistryOps, MobEffect, ConcentricRingsStructurePlacement, TemplateStructurePiece, ChunkGeneratorStructureState,
StructureManager, LiquidSettings), NeoForge 1.21.1 patches (ChunkGenerator, Structure, BootstrapContext, Beardifier → `getElement()`),
Architectury 13 (DeferredRegister, RegistrarManagerImpl, BiomeModifications), TerraBlender 1.21.1, vanilla 1.21.1 structure JSON (liquid_settings default).

## Assumptions / pre-existing quirks (unchanged)
- `HellBoatPiece`/`TreasureRoomPiece` register as `StructurePieceType.NETHER_FOSSIL`. Saved pieces therefore reload as vanilla `NetherFossilPiece`.
  This was already the case in 1.20.1 and is kept.
- `teleportToAnuLair` does not null-check `server.getLevel(ANU_LAIR)`. Kept as before.

## Requests to other owners
**core**
- `FossilMod.init()`: add `ModStructureType.register();`, for example right after `ModDimensions.register();`. Architectury's NeoForge registrar
  calls `Registry.register` directly when `register()` runs after `RegisterEvent`, so the current first call in common setup would crash on the frozen registry.
- Optional: the 1.21 way for `AnuPortal`/`HomePortal` is the `Portal` interface (`getPortalDestination` + `entity.setAsInsidePortal`),
  which gives the vanilla portal delay/cooldown handling. A direct `changeDimension` from `entityInside` still works, so this is not required.

**build**
- `ForgeFossilMod#onCommon`: `ModStructureType.register()` there is now a no-op once core calls it from `FossilMod.init()`. It can be removed.
  If core does not add the call, move it into the `ForgeFossilMod` constructor instead.
  `ModPlacedFeatures.register()` in common setup is fine (no registry write).
- `ModDimensionsImpl` already matches the new signature. Its PORT-TODO can be resolved.
- `ModWorldGenProvider`: all world bootstraps now take `BootstrapContext`. That PORT-TODO is resolved for world (entity's `ModDamageTypes` is not mine).
