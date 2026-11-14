# core agent notes (1.20.1 → 1.21.1 NeoForge)

Scope: `common/src/main/java/com/github/teamfossilsarcheology/fossil/**` except `entity/ client/ world/ compat/`, plus
`fossil.accesswidener` and `fossil_common.mixins.json`. Shared API changes for other agents: **PORT_NOTES/API.md**.

## What changed (per area)

**Root**: `FossilMod.location` → `ResourceLocation.fromNamespaceAndPath`; registers `ModDataComponents` (before items) and
`ModTriggers` (moved here from the platform common-setup, idempotent).

**Data components** (`item/component/ModDataComponents`, new): `LASER_ENTITY` (UUID), `LASER_USE_START_TIME` (Long).
Inventory of all ItemStack NBT in the repo: API.md §1.1.

**Network**: `NetworkChannel` → new `network/PayloadChannel` (same send API, `CustomPacketPayload` + `StreamCodec.ofMember(write, ctor)`,
`registerS2C` = receiver on client / `registerS2CPayloadType` on dedicated server, `registerC2S`). All 23 messages converted
(read/write code unchanged). Bidirectional messages (InstructionMessage, SyncDebugInfoMessage, C2SDisableAIMessage) get two ids
(NeoForge rejects one id in both directions). `C2SSlowMessage`: AttributeModifier with id `fossil:debug_slow_down`, `ADD_MULTIPLIED_TOTAL`.

**Items**: Properties components (food `saturationModifier`, tools/swords `attributes(SwordItem/DiggerItem.createAttributes(...))`, armor
`durability(Type.getDurability(mult))`); `ModArmorMaterials` → registered `ArmorMaterial` records (layer `minecraft:bone`/`minecraft:ancient`
= same texture path as before); `ModToolTiers` `getIncorrectBlocksForDrops`; music discs → `jukeboxPlayable(ModJukeboxSongs.X)`;
`AncientHelmetItem` no longer platform specific (GeckoLib `createGeoRenderer`); `JavelinItem` keeps 16 stack/30 durability via
`JavelinProperties` (MAX_DAMAGE + MAX_STACK_SIZE components, ignores TieredItem's `durability(tier.getUses())`), enchantment levels via
`ModEnchantments.getItemEnchantmentLevel`, `igniteForSeconds`; `getUseDuration(stack, entity)`; `appendHoverText(..., TooltipContext, ...)`;
`hurtAndBreak(n, entity, slot)`; `finalizeSpawn` without tag; `EntityType.create` without tag; `StoneTabletItem` uses `ENTITY_DATA`;
`DinoEggItem` uses `AdvancementHolder`.

**Blocks**: `Properties.copy` → `ofLegacyCopy` (same semantics as 1.20.1 copy); new ctor orders for Button/PressurePlate/Door/TrapDoor/FenceGate;
`SandBlock` → `ColoredFallingBlock(new ColorRGBA(0x8C765C))`; `DropExperienceBlock(ConstantInt.of(0), ...)`; `SaplingBlock(ModTreeGrowers.X, ...)`
(new `block/ModTreeGrowers`); `use` → `useItemOn` wrapper keeping the old logic in a private `use` (`util/InteractionResults.toItemResult`);
`super.use` → `InteractionResult.PASS`; `SarcophagusBlock` delegates upper layers to the bottom part's private `use` (BlockState#use is gone);
`codec()` everywhere it is abstract (simpleCodec or `MapCodec.unit(this)`); `isPathfindable(state, type)`; `getCloneItemStack(LevelReader, ...)`;
`isValidBonemealTarget` without isClient; `playerWillDestroy` returns BlockState; `AbstractGlassBlock` → `TransparentBlock`;
`HomePortal`/`AnuPortal`: `canUsePortal(false)`, `portalEntrancePos` removed (commented), HomePortal teleports with vanilla `DimensionTransition`
(`DO_NOTHING`, same as the old Forge ITeleporter); `FossilBlock` fortune via `Enchantments.FORTUNE` holder.

**Block entities**: `loadAdditional/saveAdditional(tag, provider)`, `getUpdateTag(provider)`, `ContainerHelper` with provider,
`ItemStack.parseOptional/saveOptional`; `BaseContainerBlockEntity#getItems/setItems` (setItems copies into the fixed lists);
`isSameItemSameComponents`; Worktable enchantment copy via components; Analyzer/Sifter `recipe.assemble(level.random)`.

**Recipes**: `Recipe<RecipeInput>`; `MultiOutputAndSlotsRecipe` stores raw `List<WeightedOutput>` + `Optional<Double> total` (codec round-trips,
same NavigableMap semantics incl. AIR filler and warning) with `MachineRecipeInput` (random source for assemble);
`WithFuelRecipe.ContainerWithAnyFuel` is a `RecipeInput`; MapCodec/StreamCodec serializers matching PORT_NOTES/data.md exactly;
`ModRecipes` helpers unwrap `RecipeHolder`; datagen builders on `RecipeOutput` (build recipe objects, serialized by the codec).

**Enchantments**: data driven; `ModEnchantments` = ResourceKeys + `bootstrap` (values identical to the old classes and to data's JSON) +
lookup helpers; old Enchantment subclasses deleted.

**Advancements**: codec based `SimpleCriterionTrigger`s (record instances, JSON = data.md), registered with a DeferredRegister on
`Registries.TRIGGER_TYPE`; datagen factories return `Criterion<…>`.

**Loot**: `CustomizeToDinoFunction.CODEC` (MapCodec, `commonFields` + `entity`), generic `LootItemFunctionType<T>`.

**Villagers**: `MerchantOffer` with `ItemCost`; `EnchantBookForEmeralds` takes a `ResourceKey<Enchantment>` and resolves it from the trader's
registries (returns null if missing); `isTreasureOnly` → `EnchantmentTags.TREASURE`. POI block-state registration removed on NeoForge
(NeoForge does it automatically and the vanilla method would throw on duplicates) – kept commented.

**Food**: `getFoodProperties().getNutrition()` → `components().get(DataComponents.FOOD).nutrition()`; `EntityDimensions` record accessors.

**Events** (`event/ModEvents`): `BlockSource` record accessors, `SavedData.Factory`, `hurtAndBreak` slot, finalizeSpawn.

**Tags**: `C_*` tool tags alias vanilla `ItemTags.*`, water buckets → `c:buckets/entity_water` (data agent request).

**Mixins** (compatibilityLevel JAVA_21): re-targeted/updated
- `ChunkStatusMixin` → `ChunkStatusTasks.generateStructureStarts(WorldGenContext, ChunkStep, StaticCache2D, ChunkAccess)` (old target was a
  Fabric intermediary lambda name that never matched on Forge).
- `SyncedEntityDataMixin`: array `itemsById`, no lock.
- `ExplosionMixin`: SavedData.Factory.
- `CameraMixin`: `move(float, float, float)`.
- `DebugLevelRendererMixin`: `renderLevel(DeltaTracker, boolean, Camera, GameRenderer, LightTexture, Matrix4f, Matrix4f)`.
- `DebugMinecraftMixin`: `clearLevel(Screen)` → `disconnect(Screen, boolean)`.
- `PlayerInfoMixin`: `getCapeLocation/getElytraLocation` → `getSkin()` (PlayerSkin record).
- `TitleScreenMixin`: now targets `PanoramaRenderer#render(GuiGraphics,int,int,float,float)` (only while TitleScreen is open).
- `JigsawStructureMixin`: new fields + `addPieces(..., PoolAliasLookup, DimensionPadding, LiquidSettings)`.
- `JigsawPlacementMixin`: `tryPlacingChildren(..., PoolAliasLookup, LiquidSettings)`, `readPoolKey`, `SequencedPriorityIterator` with
  `placement_priority`, `PoolElementStructurePiece(..., liquidSettings)`.
- GeckoLib mixins: 4.x/1.21.1 package flattening, `MathParser.setVariable` target, `GeoBone/GeoModel`; `AnimatableManagerAccessor` method renamed.
- Unchanged (verified as far as possible): AbstractFish, Cat, Ocelot, Mob, NaturalSpawner, ServerEntity, DebugMouseHandler, ParticleAccessor,
  TerrainParticle.

**Access widener**: removed `Item$Properties.maxStackSize`, `Entity.portalTime`, `Entity.portalEntrancePos`, `Entity.eyeHeight` (fields no longer exist).

## PORT-TODO list (file:line, relative to `common/src/main/java/com/github/teamfossilsarcheology/fossil/`)
- `mixin/JigsawPlacementMixin.java:79` – copy of vanilla `tryPlacingChildren`; field names `pools`/`maxDepth` assumed (mappings.dev showed
  Yarn-like `registry/maxSize`), diff against real 1.21.1 source.
- `mixin/ChunkStatusMixin.java:26` – verify structures in anu lair/treasure room with generate-structures=false.
- `mixin/SyncedEntityDataMixin.java:36` – `EntityDataAccessor#id()` (record accessor) assumed.
- `mixin/ServerEntityMixin.java:27` – `Consumer#accept` ordinal 0 in `sendChanges` assumed to still be the passenger packet.
- `mixin/AbstractFishMixin.java:12` – `onGround()` call in `aiStep` assumed unchanged.
- `mixin/TitleScreenMixin.java:26` – visual check (menu blur).
- `mixin/DebugLevelRendererMixin.java:35` – debug renderer, `PoseStack#mulPose(Matrix4f)` assumed.
- `item/JavelinItem.java:106` – stackable damaged javelins (DAMAGE + max stack 16; `ItemStack.STRICT_CODEC`/commands may reject them).
- `sounds/ModJukeboxSongs.java:39` – RegistrySupplier as `Holder<SoundEvent>` in datagen encoding.

## Assumptions / verified sources
- Architectury 13 `NetworkManager` / `NetworkAggregator` / NeoForge `NetworkManagerImpl`, `DeferredRegister`, `RegistrySupplier` (is a `Holder`),
  `CreativeTabRegistry`, fluid attributes: from `refs/architectury-api`.
- NeoForge: registration order (data components + armor materials before items: `GameData.getRegistrationOrder`), POI auto registration
  (`NeoForgeRegistryCallbacks.PoiTypeCallbacks`), `SavedData.Factory` nullable DataFixTypes, `SimpleTier` (Tier interface), Sword/Pickaxe/Axe
  patches, Camera/PanoramaRenderer/LevelRenderer/TitleScreen patches, `PayloadRegistrar` duplicate-id rule.
- mappings.dev (1.21.1): Item, Item.Properties, BlockBehaviour.Properties, ArmorMaterial, EntityType, LivingEntity, Entity, AbstractArrow,
  FriendlyByteBuf, UUIDUtil, DimensionTransition, ChunkStatusTasks, WorldGenContext, SynchedEntityData, Minecraft, JigsawPlacement$Placer.
- GeckoLib 4.9.3 (1.21.1) source for the GeckoLib mixins and `GeoRenderProvider`.
- From memory of 1.21.1 (not separately verified, likely correct): ctor orders of Button/PressurePlate/Door/TrapDoor/FenceGate/
  ColoredFallingBlock/DropExperienceBlock/TreeGrower; `TransparentBlock`; `ItemCost`; `EnchantmentTags.TREASURE`;
  `EnchantmentHelper.getEnchantmentsForCrafting/setEnchantments(stack, enchantments)`; `Ingredient.CODEC_NONEMPTY/CONTENTS_STREAM_CODEC`;
  `ItemStack(Holder<Item>, int, DataComponentPatch)`; `ByteBufCodecs.optional/list`; `EntityPredicate.ADVANCEMENT_CODEC`; `ItemPredicate.CODEC`;
  `LootContext.EntityTarget.CODEC`; `PlayerSkin` record order.
- Old worlds: our block entities' item NBT (`Items`, `FuelItem`) is not upgraded by DFU (custom BE types) – 1.20.1 machine contents
  (`Count`/`tag` format) may not load. Not fixed (would need a manual NBT upgrade); flagging for phase 2.

## Requests to other owners
**build**
- `ModTriggers.register()` is now called from `FossilMod.init()`; the extra call from common setup is a harmless no-op (can be removed).
- Deletion of `AncientHelmetItemImpl` / `CustomRecordItemImpl` is intended. No other `@ExpectPlatform` was added/removed/changed
  (AncientChest/AnuStatue/AnubiteStatue/Sarcophagus BlockItem `get(Block, Properties)`, TarBlock, FossilLeavesBlock, FlammableRotatedPillarBlock,
  ModBlockSetTypes, ModWoodTypes, ModCapabilities, FossilConfig, Version, FossilMixinPlugin unchanged).
- Datagen: enchantments/jukebox songs via `RegistrySetBuilder` + `ModEnchantments::bootstrap` / `ModJukeboxSongs::bootstrap`; loot `enchant(...)` needs
  holders from `ModEnchantments.ARCHEOLOGY` keys; advancement factories return `Criterion<>`; recipe builders implement `save(RecipeOutput)`.
- `CommonEnergyStorage` signatures unchanged (`load(Tag)`, `save()`).
- Tests (`RecipeTests`): `getAllRecipesFor` → `RecipeHolder`, use `.value()`; recipes have no `getId()`.

**entity-a**
- Bucket data: use `CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> ...)` in `saveToBucketTag` (API.md §1.3).
- `Prehistoric.java:1062`: `getAdvancements().get(DinosaurEgg.GOLDEN_EGG_ADV)` → `AdvancementHolder`.

**entity-b**
- `Javelin#setKnockback(int)` must exist (AbstractArrow lost it) and apply the knockback; JavelinItem calls it.
- `AnuBoss.AnuLair`: keep public no-arg ctor + `isAnuKilled()`; core uses `new SavedData.Factory<>(AnuLair::new, (tag, reg) -> new AnuLair(), null)`.
- `Javelin` pickup: consider removing the `DAMAGE` component at 0 damage so returned javelins stack with fresh ones.

**world**
- Keep `ModDimensions.teleportToAnuLair(MinecraftServer, Entity)`; `HomePortal` no longer calls `ModDimensions.changeDimension`.
- `world/feature/tree/*TreeGrower` are replaced by `block/ModTreeGrowers` (uses `ModConfiguredFeatures.*_TREE_KEY`, keep those names).
- `AnuCastleStructure` uses `JigsawPlacement$Placer#placing` (now `SequencedPriorityIterator`); ask core for AW entries if needed.
- `TreasureChunkGenerator`/`AnuLairChunkGenerator` are referenced by `ChunkStatusMixin` (instanceof + `createStructures`) – keep class names.

**compat**
- `AnimationProcessorOverride`: `AnimatableManagerAccessor#isFirstTick()` renamed to `fossil$isFirstTick()` (or call `manager.isFirstTick()`).
- `AnimationControllerOverride.fixTransitions` / `AnimationProcessorOverride.tickAdditiveAnimations` receive `Map<String, GeoBone>` / `GeoModel<T>`.
- Recipes: `getAllRecipesFor` returns `RecipeHolder`s; recipe ids from `holder.id()`; `getWeightedOutputs/getInput/getFuel/getDuration/getFuelDuration` unchanged.

**client**
- `client/renderer/armor/AncientHelmetRenderer` must keep a public no-arg ctor (created from `AncientHelmetItem#createGeoRenderer`).
- `ModParticles.BUBBLE/TAR_BUBBLE/LASER_PARTICLE` (used by core blocks/fluids/items) keep their names.
- Debug code called from core mixins keeps its signatures: `PathingRenderer.render(PoseStack, BufferSource, float, long)`,
  `InstructionRenderer.render(..., Frustum)`, `FAPlayerCapes.*_TEXTURE`, `DebugScreen/InstructionTab/PathingDebug` statics.

**data**
- Done on my side: codecs follow data.md. `C_WATER_BUCKETS` = `c:buckets/entity_water`; `C_SWORDS…C_HOES` alias vanilla tool tags → the generated
  `c:axes/hoes/pickaxes/shovels/swords` files can be dropped.
