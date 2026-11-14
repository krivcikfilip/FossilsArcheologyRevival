# build agent: notes (1.20.1 Forge/Fabric to 1.21.1 NeoForge)

Owner of: `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `common/build.gradle`, old `forge/` (left untouched),
new `neoforge/`. Nothing was compiled; no Maven/Mojang access here. Versions were checked against the Maven metadata or Modrinth/Cloudsmith
listings through WebFetch where possible (see the confidence column).

## 1. Build system

| File | Change |
|---|---|
| `settings.gradle` | `include("common")`, `include("neoforge")`. `fabric` and `forge` are commented out; their folders stay on disk. `maven.neoforged.net/releases` added to `pluginManagement`. |
| `build.gradle` | Loom stays at `1.10-SNAPSHOT` (it supports NeoForge 21.1; the snapshot is still published). Shadow switched to `com.gradleup.shadow` 8.3.6, because johnrengelman 8.1.1 is unmaintained and has problems with Java 21. Repositories: added NeoForged and `maven.blamejared.com` (JEI 1.21.1); removed Ladysnake (fabric-only CCA, left as a comment). `expandProps` now holds neoforge/architectury/geckolib values instead of the fabric/forge ones; `processResources` expands `META-INF/neoforge.mods.toml` and `pack.mcmeta`. |
| `gradle.properties` | Java 21, MC 1.21.1, `enabled_platforms=neoforge`, new version numbers (table below). The fabric loader version is kept because `:common` still needs it for `@Environment`. |
| `gradle/wrapper` | Unchanged (Gradle 8.14). That is new enough for Loom 1.10. |
| `common/build.gradle` | GeckoLib switched to `geckolib-common-1.21.1` (mclib removed), More Hitboxes/TerraBlender/REI/JEI/EMI moved to their 1.21.1 coordinates. |
| `neoforge/build.gradle` | New. `architectury { neoForge { platformPackage = "forge" } }` (see section 2). `neoForge "net.neoforged:neoforge:…"`, `architectury-neoforge`, `transformProductionNeoForge`, `developmentNeoForge`. The MixinExtras jar-in-jar is dropped because NeoForge bundles it. Mixin configs move to the mods.toml. `remapJar.atAccessWideners.add "fossil.accesswidener"` turns the AW into an AT, as architectury-api's neoforge module does. The data run is kept. Publishing now says NeoForge. |
| `neoforge/gradle.properties` | `loom.platform=neoforge` |

### Versions

| Property | Value | How chosen / confidence |
|---|---|---|
| neoforge_version | 21.1.266 | Newest 21.1.x in maven.neoforged.net metadata. It matches the refs/NeoForge 1.21.1 branch head. **High** |
| neoforge_loader_version_range | [4,) | FML 4 = 1.21.1 (Architectury template generator: loader_major 4). **High** |
| parchment | 1.21.1 / 2024.11.17 | Taken from GeckoLib's 1.21.1 build. **High** (newer dates probably exist) |
| architectury | 13.0.8 | Listed in maven.architectury.dev metadata (13.0.12 is the newest). **High** |
| architectury-loom | 1.10-SNAPSHOT | The snapshot metadata exists (2025-10). **Medium-high** |
| shadow | com.gradleup.shadow 8.3.6 | Listed on plugins.gradle.org. **High** |
| geckolib | 4.9.3 (`geckolib-neoforge-1.21.1`, `geckolib-common-1.21.1`) | Listed on Cloudsmith and matches refs/geckolib. **High** (common artifact name: medium) |
| terrablender | 1.21.1-4.1.0.8 | `git describe` of the TerraBlender 1.21.1 branch head gives `4.1.0-8` (version = tag + offset). **High** |
| more hitboxes | 1.9.4-alpha (`morehitboxes-{common,neoforge}-1.21.1`) | Listed on Cloudsmith. The group is assumed unchanged. **Medium** |
| REI | 16.0.799 (`RoughlyEnoughItems-api`, `-default-plugin`, `-neoforge`) | Listed in maven.shedaniel.me metadata. **High** |
| JEI | 19.70.0.469 (`jei-1.21.1-common-api`, `jei-1.21.1-neoforge-api`) | Listed in maven.blamejared.com metadata. **High** |
| EMI | 1.1.24+1.21.1 (`emi-xplat-intermediary:…:api`, `emi-neoforge`) | Version from Modrinth and from the EMI 1.21 branch. Terraformers maven was not reachable. **Medium** |
| Jade | `maven.modrinth:jade:15.10.6+neoforge` | Modrinth. The API used was verified against Jade's 1.21-neoforge source. **High** |
| Carry On | `maven.modrinth:carry-on:PV8oLZ1q` (2.2.6 for 1.21.1 NeoForge) | Modrinth. The mixin target was verified against the CarryOn 1.21.1 source. **High** |
| Farmer's Delight | `maven.modrinth:farmers-delight:1.21.1-1.3.4` | Modrinth. Fields were verified against the FD 1.21 branch (mod_version 1.3.4). **High** |
| debugutils | `maven.modrinth:debugutils:GlCTL0rQ` (1.21.1-1.0.9-neoforge) | Modrinth. **High** |
| fabric_loader (common only) | 0.16.7 | Unchanged. **High** |

### Maven hosts the build needs
- Plugins: `maven.fabricmc.net`, `maven.architectury.dev`, `maven.neoforged.net/releases`, `maven.minecraftforge.net`, `plugins.gradle.org` (shadow, minotaur, curseforgegradle).
- Loom also needs Mojang: `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`.
- Dependencies: `maven.neoforged.net/releases` (NeoForge, FML, NeoForm), `maven.architectury.dev` (Architectury API), `maven.parchmentmc.org`,
  `dl.cloudsmith.io/public/geckolib3/geckolib/maven/`, `dl.cloudsmith.io/public/darkpred-mods/more-hitboxes/maven/`,
  `maven.minecraftforge.net` (TerraBlender), `maven.shedaniel.me` (REI, cloth), `maven.blamejared.com` (JEI), `maven.terraformersmc.com` (EMI),
  `api.modrinth.com/maven` (Jade, Carry On, FD, debugutils), `cursemaven.com` (currently unused, kept), `modmaven.dev`, `maven.fabricmc.net` (fabric-loader for common),
  `repo.maven.apache.org`/Maven Central (MixinExtras, misc), `maven.createmod.net` + `maven.tterrag.com` (Create, currently commented out), `dvs1.progwml6.com` (legacy JEI mirror).

## 2. `neoforge/` module (copied from `forge/`, which is unchanged)

**Architectury `@ExpectPlatform` package rule (verified):** for NeoForge, Architectury's default lookup is `<pkg>.neoforge.<Class>Impl`.
That can be overridden with `architectury { neoForge { platformPackage = "forge" } }`, which architectury-api's own `neoforge/build.gradle`
and `testmod-neoforge` do. PORTING.md says package names stay, so **the `…forge` packages were kept and `platformPackage = "forge"` is set**.
No Java packages were renamed. (If someone renames them later, drop the `platformPackage` line.)

| Area | What changed |
|---|---|
| `forge/ForgeFossilMod` | `@Mod` constructor is now `(IEventBus, ModContainer)`. `EventBuses`/`DistExecutor`/`MinecraftForge.EVENT_BUS.register(this)` are removed. Configs go through `modContainer.registerConfig`. Chunk generator codecs are registered in `RegisterEvent`, because vanilla registries are frozen after registration and the old `Registry.register` in common setup would crash. `SpawnPlacements.register` became `RegisterSpawnPlacementsEvent` (`SpawnPlacementTypes.IN_WATER`, Operation.REPLACE). The geometry loader is registered with its full id `fossil:plant_loader`. Attachments and block capabilities are wired up here. |
| capabilities | Forge entity caps became NeoForge **data attachments**: `forge/capabilities/ModAttachments` (`fossil:mammal`, `fossil:player`, the same ids; FIRST_HATCH has `copyOnDeath`). The `MammalCapProvider`/`FirstHatchCapProvider` classes were deleted. `MammalCap`/`FirstHatchCap` use NeoForge `INBTSerializable` (provider-taking). `ModCapabilitiesImpl` reads with `getExistingData` and creates the attachment only for mammals, which is the old "attach only to mammals" behaviour. |
| block capabilities | `EnergyContainerBlockEntityMixin` and `MachineContainerBlockEntityMixin` (which overrode `getCapability`) were deleted from neoforge/ and from `fossil_forge.mixins.json`. They are replaced by `forge/capabilities/ModBlockCapabilities` (`RegisterCapabilitiesEvent`): ItemHandler with the same side mapping for analyzer, culture vat, sifter and worktable; EnergyStorage for analyzer and culture vat when `machinesRequireEnergy` is on; an unsided `InvWrapper` for the feeder (Forge's BaseContainerBlockEntity default). |
| energy | `FAEnergyStorage` now extends `net.neoforged.neoforge.energy.EnergyStorage`. Provider-taking (de)serialize; `load/save` write the IntTag directly. |
| config | `ForgeConfigSpec` is now `ModConfigSpec`. `ForgeConfigFix` uses `getLoadedConfig().config()` and `save()`. |
| events (`forge/event/ForgeModEvents`) | `@EventBusSubscriber` (game bus). `LivingTickEvent` became `EntityTickEvent.Pre`. `AttachCapabilitiesEvent` was removed. Clone copy is kept. `PlayerSleepInBedEvent`/`SleepingTimeCheckEvent` ALLOW became `CanPlayerSleepEvent`/`CanContinueSleepingEvent`; only the NOT_POSSIBLE_NOW problem is lifted. Effects are `Holder<MobEffect>` (`BuiltInRegistries.MOB_EFFECT.wrapAsHolder`). `OnDatapackSyncEvent.getRelevantPlayers()`. `RegisterGameTestsEvent` (a mod bus event) is registered in the constructor. |
| client | `ClientModEvents` was renamed to NeoForge (`RenderBlockScreenEffectEvent`). `PlantModelGeometry#bake` lost its `ResourceLocation` parameter (NeoForge 20.5). `IGeometryLoader`/`IDynamicBakedModel` packages renamed. |
| Impl classes | `FlammableRotatedPillarBlockImpl`: ToolAction became `ItemAbility`/`ItemAbilities.AXE_STRIP`. Item Impls: `IClientItemExtensions` package rename (`initializeClient` is deprecated but still called in 21.1). `ModDimensionsImpl` was rewritten to `changeDimension(Entity, ServerLevel, DimensionTransition)`. `ModFeaturesImpl` uses `Registries.FEATURE`. `ModVillagesImpl` uses `ResourceLocation.withDefaultNamespace`. `VersionImpl`/`FossilMixinPluginImpl` use `net.neoforged.fml.loading.FMLLoader`. `AncientHelmetItemImpl` and `CustomRecordItemImpl` were **deleted**, because core removed those `@ExpectPlatform`s (GeckoLib `createGeoRenderer` / jukebox_playable). |
| mixins | `fossil_forge.mixins.json`: JAVA_21, two cap mixins removed. `GeoEntityRendererMixin` target descriptor updated to GeckoLib 4.9 (`…ZFIII)V`, packed colour), verified in refs. `PickupHandlerMixin` verified against CarryOn 1.21.1. `WalkNodeEvaluatorMixin` is unchanged (PORT-TODO on the ordinal). |
| compat | Jade, REI and EMI plugins are unchanged (APIs verified against Jade 1.21-neoforge and REI 16.x sources). `MultiOutputEmiRecipeImpl`: `EmiUtil.values` became `EmiTagKey.of(tag).stream()`, and `EmiTags.getRawValues(EmiTagKey)`. `FarmersDelightCompat`: food value now comes from `DataComponents.FOOD` (verified against FD 1.21). **All FD add-on compats and `AlexsMobsCompat` are disabled**: a no-op method, with the original code kept as line comments and per-class notes on whether a 1.21.1 NeoForge build exists. |
| world | `ForgeFossilBiomeModifiers` uses `BiomeModifiers.Add*BiomeModifier`, `NeoForgeRegistries.Keys.BIOME_MODIFIERS` and `BootstrapContext`. `ForgeFossilRegion` is unchanged (TerraBlender API verified). |
| datagen | `DataGenerators`: NeoForge `GatherDataEvent`/`AdvancementProvider`. Recipes and loot get the lookup of `ModWorldGenProvider.getRegistryProvider()`, which contains our datapack enchantments. Recipes: `RecipeOutput`, `generateRecipes(..., FeatureFlags.VANILLA_SET)`, `SHORT_GRASS`, `TURTLE_SCUTE`. Loot: provider-taking sub providers, `NestedLootTable`, `EnchantedCountIncreaseFunction.lootingMultiplier`, enchantment item sub predicate, `TransparentBlock`, and `ResourceKey<LootTable>` in the generic tables. The old validate override was dropped (NeoForge's default does the same). Advancements: `AdvancementHolder`, `AdvancementType`, `Criterion<?>`. World gen: added `Registries.ENCHANTMENT`/`JUKEBOX_SONG` bootstraps from core. Food values use `.nutrition()`. `FossilCreateRecipeProvider` (Create 6 API) and `PFaunaTagsProvider` (no 1.21.1 PF) are no-op stubs with the old code commented. |
| gametests | NeoForge `GameTestHolder`/`PrefixGameTestTemplate`. `RecipeTests` unwraps `RecipeHolder`. The unused `BatchTestCommand`/`BatchArgument` are stubbed with the old code kept. |
| resources | `META-INF/mods.toml` became `META-INF/neoforge.mods.toml`: `type="required"/"optional"`, `[[mixins]]` entries for both configs, loader `[4,)`, deps `neoforge [21.1.0,)`, architectury 13+, geckolib 4.5+, terrablender 4+, morehitboxes 1.9.3-alpha+, FD id fixed to `farmersdelight`, duplicate emi entry removed. `pack.mcmeta`: pack_format 48 with `supported_formats` [34, 48], forge keys removed (data agent's request). The data agent converted `data/**` (recipe/advancement/structure/`c` tags) in place. I also pointed the four advancement reward/criterion recipe ids from `fossil:*_forge` (which never existed) to the real recipe ids. |

**Phase 2 datagen:** wipe `common/src/generated/resources/{data,assets}` before re-running datagen. The old cache lists the 1.20 (plural)
folder names (data agent request; also noted in `DataGenerators`'s javadoc).

## 3. PORT-TODOs (file:line)
Paths are relative to `neoforge/src/main/java/com/github/teamfossilsarcheology/fossil/` unless stated.

Build/resources
- `gradle.properties:12`: Parchment date may be newer.
- `gradle.properties:39`, `common/build.gradle:25`, `neoforge/build.gradle:51`: More Hitboxes 1.21.1 artifact names/group and API (alpha) are unverified.
- `gradle.properties:44`, `common/build.gradle:33`, `neoforge/build.gradle:63`: EMI 1.21.1 coordinates (`emi-xplat-intermediary`, `emi-neoforge`).
- `common/build.gradle:23`: GeckoLib common artifact is mojmap. If Loom complains in `:common`, use `geckolib-neoforge-1.21.1`.
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml:68`: More Hitboxes version range uses the alpha suffix.

Java
- `forge/capabilities/ModAttachments.java:19`: 1.20.1 ForgeCaps data (pregnancies, first-hatch flag) is not migrated to attachments.
- `forge/energy/FAEnergyStorage.java:56`: forward the provider if core changes `CommonEnergyStorage#load/save`.
- `world/dimension/forge/ModDimensionsImpl.java:13`: needs common `changeDimension(Entity, ServerLevel, DimensionTransition)`.
- `forge/mixin/WalkNodeEvaluatorMixin.java:16`: ordinal of the first `BlockState#is(TagKey)` in `isBurningBlock`.
- `compat/emi/forge/MultiOutputEmiRecipeImpl.java:32`: EMI internal API (`EmiTagKey`).
- `item/forge/{AncientChest,AnuStatue,AnubiteStatue,Sarcophagus}BlockItemImpl.java:16-17`: `initializeClient` is deprecated; move to `RegisterClientExtensionsEvent`.
- `forge/compat/alexsmobs/AlexsMobsCompat.java:8`: no 1.21.1 Alex's Mobs.
- `forge/compat/farmers/addon/*.java` (header javadoc, lines 7-12): disabled add-ons. Ones **with** a 1.21.1 NeoForge Modrinth release: crabbers-delight 1.3.1, oceans-delight 1.0.4, pineapple-delight 1.1.4-1.21.1-neoforge, coffee-delight 1.4.1, ends-delight 2.6.1+neoforge.1.21.1, brewin-and-chewin 4.5.0+1.21.1-neoforge, festive-delight 2.0.0, miners-delight 1.4.5, enders-delight 1.3.1, seed-delight 1.0.1, corn-delight 1.2.10-1.21.1, cultural-delights 0.19.5-1.21.1. **None found** (Modrinth only; CurseForge not checked): delightful (and the ars-nouveau/BYG variants), butchers-delight-foods, casualness-delight, nethers-delight, farmers-respite, alexs-delight, argentinas-delight.
- `forge/data/providers/ModRecipeProvider.java:54`: core recipe builders need `save(RecipeOutput)`.
- `forge/data/providers/ModRecipeProvider.java:488`, `forge/data/providers/FossilCreateRecipeProvider.java:6`: Create 6 datagen.
- `forge/data/providers/PFaunaTagsProvider.java:5`: no Prehistoric Fauna for 1.21.1.
- `forge/data/providers/ModWorldGenProvider.java:25`: all bootstraps must take `BootstrapContext`.
- `forge/data/loot/ModBlockLootTables.java:66`: loot uses the world-gen registry lookup for our enchantments.
- `forge/data/advancements/FossilAdvancements.java:83`: `PlayerHurtEntityTrigger.TriggerInstance#playerHurtEntity(Optional<EntityPredicate>)`.
- `forge/data/advancements/FossilAdvancements.java:90`: custom trigger factories must return `Criterion<?>`.
- `forge/tests/RecipeTests.java:37`: depends on the core recipe API.
- `forge/tests/BatchTestCommand.java:8`, `forge/tests/BatchArgument.java:5`: unused debug command, stubbed.

## 4. Assumptions and risks
- Vanilla 1.21.1 signatures that are not in the NeoForge patches were written from knowledge, not from source. This covers `DimensionTransition`'s 7-arg constructor, `generateRecipes(…, FeatureFlagSet)`, loot helper methods, `EnchantedCountIncreaseFunction`, `ItemSubPredicates.ENCHANTMENTS`, `BlockSetType(String)` and `PlayerHurtEntityTrigger`. Most are confirmed indirectly by patch context lines.
- `EventBusSubscriber.Bus.MOD` is deprecated for removal in the latest FML 4.0.x but still works. It is used by `DataGenerators` and `ModFeaturesImpl`.
- Config values are read through `ModConfigSpec#get`, which throws before the COMMON config is loaded. This was the same on Forge, but load timing differs on NeoForge 21. Watch for early `FossilConfig` calls (for example during registration or `Region#getWeight`).
- Datagen code is the least verified part. It is only used by the data run, but it lives in `main` and must compile.

## 5. Requests to other owners
**core**
- `fossil_common.mixins.json`: set `"compatibilityLevel": "JAVA_21"` (the neoforge config already is).
- Recipe builders (`AnalyzerRecipeBuilder`, `SifterRecipeBuilder`, `WorktableRecipeBuilder`, `CultureVatRecipeBuilder`, `MultiOutputAndSlotsRecipeBuilder`): `save(RecipeOutput)` (datagen, `ModRecipeProvider`).
- Advancement triggers `IncubateEggTrigger.TriggerInstance.incubateEgg(EntityType)`, `ImplantEmbryoTrigger…implantEmbryo(Item)`, `OpenSarcophagusTrigger…useScarab()`, `ScarabTameTrigger…useScarab()` must return `Criterion<?>`.
- `CommonEnergyStorage#load(Tag)/save()`: keep these signatures, or tell build (FAEnergyStorage) if they gain a `HolderLookup.Provider`.
- `ModRecipes.getAllRecipesFor…` / `getCultureVatRecipeForItem` and `WithFuelRecipe.ContainerWithAnyFuel` are used by `forge/tests/RecipeTests` (expects `RecipeHolder` from `RecipeManager#getAllRecipesFor`).
- `AncientHelmetItem`/`CustomRecordItem` `@ExpectPlatform`s were removed by core; the neoforge Impls were deleted accordingly. Please tell build if any other `@ExpectPlatform` is added, removed or changes signature. The current set matches 1:1 (17 Impl classes).
- `S2CMammalCapMessage(Animal, int, EntityInfo)` and `MessageHandler.CAP_CHANNEL.sendToPlayers(Iterable<ServerPlayer>, …)` are used by `ModCapabilitiesImpl`/`ForgeModEvents`.
- Glass blocks should extend `TransparentBlock`, which the block loot table uses for silk-touch-only drops.

**world**
- `ModDimensions.changeDimension`: replace `PortalInfo` with `DimensionTransition` in the `@ExpectPlatform` signature and its callers (`ModDimensions` line ~68, `HomePortal` line ~50), for example `new DimensionTransition(level, pos, entity.getDeltaMovement(), yRot, xRot, DimensionTransition.DO_NOTHING)`. Since this is now vanilla, the `@ExpectPlatform` could be removed entirely (then delete `world/dimension/forge/ModDimensionsImpl`).
- All `bootstrap(BootstapContext<…>)` must become `BootstrapContext` (ModBiomes, ModStructureSets, ModOreFeatures, ModConfiguredFeatures, ModTemplatePools, ModStructures, ModPlacedFeatures, ModOrePlacements, ModDimensions; plus entity's `ModDamageTypes`).
- `ModPlacedFeatures.register()`, `ModStructureType.register()` and `ModTriggers.register()` (core) are still called from `FMLCommonSetupEvent`. They must not `Registry.register` into vanilla registries there, because the registries are frozen on NeoForge. Chunk generator codecs were moved to `RegisterEvent` in `ForgeFossilMod`.
- `TreasureChunkGenerator.CODEC` / `AnuLairChunkGenerator.CODEC` must be `MapCodec` (1.20.5) for `Registries.CHUNK_GENERATOR`.

**entity-a / entity-b**
- `PrehistoricFish::canSpawn` and `Coelacanth::canCoelacanthSpawn` must match `SpawnPlacements.SpawnPredicate<T>` (`(EntityType<T>, ServerLevelAccessor, MobSpawnType, BlockPos, RandomSource)`).
- `PrehistoricEntityInfo.isMammal(Mob)`, `Prehistoric#getMatingCooldown()`, `data().adultAgeInTicks()` and `getAge()` are used by the attachments and by Jade.

**client**
- `PlantModelBakery.bakeFace(...)`, `PlantBlockModel.LOADER` (= `fossil:plant_loader`), `OverlayRenderer.renderTar(PoseStack)`, `CustomItemRenderer.INSTANCE`, `ClientInit.immediate()/later()` are used by neoforge.

**compat**
- `FossilEmiPlugin`, `FossilREIClientPlugin`/`FossilREICommonPlugin` are extended by the neoforge entrypoints. `MultiOutputEmiRecipe.WeightedItem(probability, stack, tagKey)` and the two `@ExpectPlatform`s (`values`, `toIngredient`) are implemented as-is.
- `CarryOnCompat.overridePickUp(LocalRef<UUID>, Entity, ServerPlayer)` is called by `PickupHandlerMixin`.

**data**
- Done (pack.mcmeta). Common `data/**/advancement/recipes/**` that reference `fossil:*_forge` recipe ids do not exist in the neoforge copy any more (fixed there).
