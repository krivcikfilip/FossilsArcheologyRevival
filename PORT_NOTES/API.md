# Shared API changes made by core (1.20.1 → 1.21.1 NeoForge)

Read this first. Everything here is in `common/src/main/java/com/github/teamfossilsarcheology/fossil/**`, which core owns (except
`entity/`, `client/`, `world/`, `compat/`). Package names are unchanged. "unchanged" means call sites in your package keep compiling.

---------------------------------------------------------------------------------------------------------------------------------

## 1. Item data components (replaces ItemStack NBT)

### 1.1 Inventory of every ItemStack NBT use in the 1.20.1 repo (common + forge)

| Where (owner) | Old NBT | 1.21.1 replacement |
|---|---|---|
| `item/LaserPointerItem` (core) | `LaserEntityUUID` (UUID), `UseStartTime` (long) | `ModDataComponents.LASER_ENTITY`, `ModDataComponents.LASER_USE_START_TIME` (done) |
| `item/StoneTabletItem` (core) | `EntityType.updateCustomEntityTag(level, player, entity, stack.getTag())` | `DataComponents.ENTITY_DATA` (`CustomData`) (done) |
| `item/ArtificialHoneycombItem` (core) | `finalizeSpawn(..., stack.getTag())` | `finalizeSpawn` lost its `CompoundTag` param (done) |
| `recipe/WithFuelRecipe#assemble` (core) | `result.setTag(input.getTag().copy())` | `result.applyComponents(input.getComponentsPatch())` (done) |
| `block/entity/WorktableBlockEntity` (core) | `EnchantmentHelper.setEnchantments(getEnchantments(input), out)` | `EnchantmentHelper.setEnchantments(out, EnchantmentHelper.getEnchantmentsForCrafting(input))` (done) |
| `block/entity/CultureVatBlockEntity` (core) | `ItemStack.of(tag)` / `stack.save(new CompoundTag())` | `ItemStack.parseOptional(registries, tag)` / `stack.saveOptional(registries)` (done) |
| `block/custom_blocks/FossilBlock`, `item/JavelinItem` (core) | `EnchantmentHelper.getEnchantments(...)`, `getItemEnchantmentLevel(Enchantment, stack)` | `ModEnchantments.getItemEnchantmentLevel(registries, key, stack)` (done) |
| `entity/prehistoric/base/PrehistoricSwimmingBucketable#saveToBucketTag` (entity-a) | `bucket.getOrCreateTag().put...` | **vanilla** `DataComponents.BUCKET_ENTITY_DATA`, see 1.3 |
| `entity/prehistoric/base/PrehistoricFish#saveToBucketTag` (entity-a) | `bucket.getOrCreateTag().put...` | **vanilla** `DataComponents.BUCKET_ENTITY_DATA`, see 1.3 |
| `entity/Javelin` (entity-b) | `stack.setDamageValue(...)` (Damage NBT) | unchanged API, now the `DAMAGE` component (see 1.4) |
| `client/ClientInit` item property `using` (client) | none (reads `entity.getUseItem()`) | unchanged, only `new ResourceLocation("using")` → `ResourceLocation.withDefaultNamespace("using")` |
| `client/gui/filters/*`, `compat/emi/*` | `getTag()` there is `HolderSet#getTag`/`CreativeModeTab` – not ItemStack NBT | – |

No other ItemStack NBT exists in the repo (checked `getTag/getOrCreateTag/hasTag/setTag/getTagElement/addTagElement/removeTagKey/ItemStack.of/save(CompoundTag)/BlockEntityTag/EntityTag`).

### 1.2 `com.github.teamfossilsarcheology.fossil.item.component.ModDataComponents`

Architectury `DeferredRegister<DataComponentType<?>>` on `Registries.DATA_COMPONENT_TYPE`, registered from `FossilMod.init()` before `ModItems`.

| Field | Id | Type | Codec / StreamCodec | Replaces |
|---|---|---|---|---|
| `LASER_ENTITY` | `fossil:laser_entity` | `RegistrySupplier<DataComponentType<UUID>>` | `UUIDUtil.CODEC` / `UUIDUtil.STREAM_CODEC` | NBT `LaserEntityUUID` |
| `LASER_USE_START_TIME` | `fossil:laser_use_start_time` | `RegistrySupplier<DataComponentType<Long>>` | `Codec.LONG` / `ByteBufCodecs.VAR_LONG` | NBT `UseStartTime` |

Usage:
```java
stack.set(ModDataComponents.LASER_ENTITY.get(), uuid);
UUID uuid = stack.get(ModDataComponents.LASER_ENTITY.get());   // null if absent
stack.remove(ModDataComponents.LASER_USE_START_TIME.get());
```
Need a new component? Ask core (write it in your notes "Requests to other owners"); do not put typed data in `CUSTOM_DATA`
unless it is an opaque NBT blob.

### 1.3 Bucketed fish (entity-a): vanilla `BUCKET_ENTITY_DATA`
`Bucketable.saveDefaultDataToBucketTag(mob, bucket)` already writes into `DataComponents.BUCKET_ENTITY_DATA`. Write extra keys with
```java
CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
    tag.putInt("Age", getAge());
    ...
});
```
`loadFromBucketTag(CompoundTag)` keeps its signature (vanilla passes `customData.copyTag()`). The NBT keys stay the same.
Mob buckets need `new Item.Properties().component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)` like vanilla (who creates the bucket items: entity-a/`PrehistoricEntityInfo`).

### 1.4 Other component facts you will need
- Durability: `Item.Properties#durability` sets `MAX_DAMAGE`, `MAX_STACK_SIZE=1`, `DAMAGE=0`. `TieredItem` calls `properties.durability(tier.getUses())`.
  Javelins keep stack size 16 + 30 durability via `JavelinItem.JavelinProperties` (no default `DAMAGE`; the component appears on first
  `setDamageValue`). `Javelin` (entity-b) keeps using `setDamageValue/getMaxDamage`; to keep fresh and returned javelins stackable,
  `remove(DataComponents.DAMAGE)` when the damage is 0 (suggestion).
- Food: `item.components().get(DataComponents.FOOD)` (`FoodProperties` record: `nutrition()`, `saturation()`); `Item#getFoodProperties()` is gone.
  Builder: `.saturationModifier(f)` (was `saturationMod`).
- Enchantments on stacks: `stack.getEnchantments()` / `EnchantmentHelper.getItemEnchantmentLevel(Holder<Enchantment>, stack)`.
- ItemStack (de)serialization in entity NBT: `stack.save(registries)` (non-empty only!) / `stack.saveOptional(registries)`,
  `ItemStack.parse(registries, tag)` / `ItemStack.parseOptional(registries, tag)`; `registries = level().registryAccess()`.
  `SimpleContainer#createTag(registries)` / `fromTag(list, registries)`; `ContainerHelper.saveAllItems(tag, items, registries)`.
  Network: `ItemStack.OPTIONAL_STREAM_CODEC` (no `buf.writeItem` anymore).

---------------------------------------------------------------------------------------------------------------------------------

## 2. Networking (`network/`)

Architectury 13 payloads. `dev.architectury.networking.NetworkChannel` is deprecated-for-removal and broken on a NeoForge
dedicated server, so it was replaced by **`network/PayloadChannel`**, which keeps the old call-site shape.

### 2.1 Sending – call sites are UNCHANGED
```java
MessageHandler.SYNC_CHANNEL.sendToPlayer(serverPlayer, new S2CMusicMessage(sound));
MessageHandler.SYNC_CHANNEL.sendToPlayers(players, new S2CSyncActiveAnimationMessage(entity, controller, info)); // Iterable<ServerPlayer>
MessageHandler.SYNC_CHANNEL.sendToServer(new C2SHitPlayerMessage(this, player));   // client only
MessageHandler.DEBUG_CHANNEL...                                                     // still null unless Version.debugEnabled()
MessageHandler.CAP_CHANNEL.sendToPlayers(players, new S2CMammalCapMessage(animal, progress, embryo));
```
`PayloadChannel` methods: `sendToPlayer(ServerPlayer, T)`, `sendToPlayers(Iterable<ServerPlayer>, T)`, `sendToServer(T)` with
`T extends CustomPacketPayload`. All message constructors used outside `network/` are unchanged:

| Call site (owner) | Message | Change needed |
|---|---|---|
| `FossilMod#syncData` (core) | S2CSyncEntityInfo/FoodMappings/EntityVariants/Config | none |
| `client/ClientInit` (client) | `C2SRiderForceFlyingMessage(int, boolean)`, `C2SVerticalFlightMessage.flyUp/flyDown/stop(int)` | none |
| `client/gui/debug/{AnimationTab,InfoTab,EmbryoTab,EggTab,SkeletonEditTab,InstructionTab,DebugScreen}` (client) | C2SRotation, C2SForceAnimation, C2STame, SyncDebugInfo, InstructionMessage, C2SDisableAI, C2SDiscard, C2SSlow, C2SStructure | none |
| `entity/ai/control/CustomFlightMoveControl`, `entity/ai/DinoPanicGoal` (entity-b) | `S2CMarkMessage(int[], BlockState[], boolean)` | none |
| `entity/util/InstructionSystem` (entity-b) | `InstructionMessage(int, boolean, List<Instruction>)` | none |
| `entity/prehistoric/base/DinosaurEgg` (entity-a) | `S2CMusicMessage(SoundEvent)` | none |
| `entity/prehistoric/base/Prehistoric` (entity-a) | `C2SHitPlayerMessage(Entity, Entity)`, `SyncDebugInfoMessage(...)`, `C2SDisableAIMessage(int, boolean, byte)` | none |
| `entity/animation/AnimationLogic` (entity-b) | `S2CSyncActiveAnimationMessage(Entity, String, ActiveAnimationInfo)`, `S2CCancelAnimationMessage(int, String)` | none |
| `entity/ToyTetheredLog` (entity-b) | `S2CSyncToyAnimationMessage(int, float, float)` | none |
| `entity/ai/DelayedAttackGoal` (entity-b) | `S2CActivateAttackBoxesMessage(Entity, double)` | none (`List.of(player)` is an Iterable) |
| neoforge `ModCapabilitiesImpl`, `ForgeModEvents` (build) | `S2CMammalCapMessage(Animal, int, EntityInfo)` on `CAP_CHANNEL` | none |

Things the messages read on the receiving side (must keep their signatures): `EntityDataLoader.Data::readBuf/writeBuf`,
`Variant::readBuf/writeBuf`, `FoodType/Diet readBuf/writeBuf` (static `(FriendlyByteBuf)` / `(FriendlyByteBuf, T)`),
`Instruction.decodeBuffer(FriendlyByteBuf)` / `encodeBuffer(List, FriendlyByteBuf)`, `EntityInfo.fromNbt(String)`,
`EntityDataLoader.INSTANCE.replaceData`, `EntityVariantLoader.INSTANCE.replaceVariants`, `AnimationLogic.Builder`,
`PrehistoricFlying#setFlying/startTakeOff/hasTakeOffAnimation/setFlyingUp/setFlyingDown`, `Prehistoric#attackTarget`,
`ToyTetheredLog#startAnimation`, `InstructionTab.INSTRUCTIONS/activeEntity/Pair`, `DebugScreen.showPath/entity`.
`FriendlyByteBuf.readMap/writeMap/readList/writeCollection` now take `StreamDecoder/StreamEncoder` (method refs of the
static `(buf)`/`(buf, value)` methods still fit).

### 2.2 Message pattern (for new packets)
```java
public class S2CFooMessage implements CustomPacketPayload {
    public static final Type<S2CFooMessage> TYPE = new Type<>(FossilMod.location("s2c_foo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CFooMessage> STREAM_CODEC = StreamCodec.ofMember(S2CFooMessage::write, S2CFooMessage::new);
    private S2CFooMessage(FriendlyByteBuf buf) { ... }
    private void write(FriendlyByteBuf buf) { ... }
    private void apply(NetworkManager.PacketContext context) { context.queue(() -> ...); }   // was Supplier<PacketContext>
    @Override public Type<S2CFooMessage> type() { return TYPE; }
    public static void register(PayloadChannel channel) { channel.registerS2C(TYPE, STREAM_CODEC, S2CFooMessage::apply); }
}
```
and add `S2CFooMessage.register(SYNC_CHANNEL)` to `MessageHandler.register()` (core file → request it).
- `registerS2C`: receiver on the client, `NetworkManager.registerS2CPayloadType` on a dedicated server.
- `registerC2S`: receiver on both sides.
- NeoForge forbids registering one id for both directions: messages sent both ways implement `PayloadChannel.BidirectionalPayload`
  and use two ids (`s2c_x` + `c2s_x`), see `InstructionMessage`, `SyncDebugInfoMessage`, `C2SDisableAIMessage`.
  `PayloadChannel.send*` picks the right direction automatically.
- Payload ids are now `fossil:<s2c|c2s>_<snake_name>` (old NetworkChannel used hashed ids under `fossil:sync_channel/...`).
- Registration happens in `FossilMod.init()` (mod constructor) – before NeoForge's `RegisterPayloadHandlersEvent`.

---------------------------------------------------------------------------------------------------------------------------------

## 3. Registries / shared helpers (changed signatures)

| API | 1.20.1 | 1.21.1 |
|---|---|---|
| `FossilMod.location(path)` | `new ResourceLocation(..)` | `ResourceLocation.fromNamespaceAndPath` (unchanged signature) |
| `ModItems.MUSIC_DISC_*` | `RegistrySupplier<RecordItem>` | `RegistrySupplier<Item>` (jukebox_playable component) |
| `ModJukeboxSongs` (new, `sounds/`) | – | `ResourceKey<JukeboxSong> BONES/ANU/SCARAB/DISCOVERY` + `bootstrap(BootstrapContext<JukeboxSong>)` for datagen |
| `CustomRecordItem.get(...)` | `@ExpectPlatform get(int, RegistrySupplier<SoundEvent>, Properties, int)` | plain `get(ResourceKey<JukeboxSong>, Item.Properties)` – platform impl deleted (intended) |
| `ModArmorMaterials` | enum implementing `ArmorMaterial` | class, `RegistrySupplier<ArmorMaterial> ANCIENT/BONE` (= `Holder<ArmorMaterial>`), `ANCIENT_DURABILITY_MULTIPLIER=15`, `BONE_DURABILITY_MULTIPLIER=25`, `register()` (called by `ModItems.register()`) |
| `AncientHelmetItem.get(...)` | `@ExpectPlatform get(ArmorMaterial, Type, Properties)` | plain `get(Holder<ArmorMaterial>, Type, Properties)`; renderer via GeckoLib `createGeoRenderer` (→ `client/renderer/armor/AncientHelmetRenderer` no-arg ctor) – platform impl deleted (intended) |
| `ModToolTiers` | `Tier` with `getLevel()` | `Tier` with `getIncorrectBlocksForDrops()` (SCARAB → `INCORRECT_FOR_NETHERITE_TOOL`, TOOTH_DAGGER → `INCORRECT_FOR_DIAMOND_TOOL`) |
| `ModEnchantments.ARCHEOLOGY/PALEONTOLOGY` | `RegistrySupplier<Enchantment>` | **`ResourceKey<Enchantment>`** (data driven). `bootstrap(BootstrapContext<Enchantment>)`, `get(HolderLookup.Provider, key)` → `Optional<Holder.Reference<Enchantment>>`, `getItemEnchantmentLevel(HolderLookup.Provider, key, stack)`. `ArcheologyEnchantment`/`PaleontologyEnchantment` classes deleted (JSON in data). Datagen loot: `enchant(ARCHEOLOGY.get(), 1)` → resolve `registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.ARCHEOLOGY)` |
| `ModTriggers.*_TRIGGER` | `CriteriaTriggers.register(..)` instances | same static instances, registered via `DeferredRegister` on `Registries.TRIGGER_TYPE` in `FossilMod.init()`; `ModTriggers.register()` is idempotent (calling it again from common setup is a no-op, but it must not be the first call) |
| `XTrigger.TriggerInstance.implantEmbryo(Item)`, `incubateEgg(EntityType)`, `OpenSarcophagusTrigger.TriggerInstance.useScarab()`, `ScarabTameTrigger.TriggerInstance.useScarab()` | returned TriggerInstance | return **`Criterion<TriggerInstance>`** (for `Advancement.Builder#addCriterion`) |
| Trigger calls `trigger(ServerPlayer, ...)` | | unchanged |
| `ModLootItemFunctionTypes.CUSTOMIZE_TO_DINOSAUR` | `RegistrySupplier<LootItemFunctionType>` | `RegistrySupplier<LootItemFunctionType<CustomizeToDinoFunction>>`; `CustomizeToDinoFunction.apply(EntityTarget)` unchanged, `CODEC` (MapCodec) added |
| `ModTreeGrowers` (new, `block/`) | `world/feature/tree/*TreeGrower extends AbstractTreeGrower` | `TreeGrower CALAMITES/CORDAITES/MUTANT_TREE/PALM/SIGILLARIA/TEMPSKYA` (uses `ModConfiguredFeatures.*_TREE_KEY`). The old grower classes are unused now (world may delete them) |
| `ModBlocks.DENSE_SAND` | `RegistrySupplier<SandBlock>` | `RegistrySupplier<ColoredFallingBlock>` |
| `ModBlocks` other suppliers | | unchanged types/names |
| `ModBlockEntities`, `ModMenus`, `ModSounds`, `ModFluids`, `ModTabs`, `ModVillagers` | | unchanged |
| `ModItemTags.C_SWORDS/C_PICKAXES/C_AXES/C_SHOVELS/C_HOES` | `c:swords` … | now **alias the vanilla** `ItemTags.SWORDS/...` (names kept) |
| `ModItemTags.C_WATER_BUCKETS` | `c:water_buckets` | `c:buckets/entity_water` |
| `CommonEnergyStorage` | `load(Tag)`, `save()` | unchanged (build's impl keeps working) |
| `ModCapabilities` facade | | unchanged (all `@ExpectPlatform` signatures unchanged) |

### 3.1 Recipes
- `ModRecipes.ANALYZER_TYPE/SIFTER_TYPE/WORKTABLE_TYPE/CULTURE_VAT_TYPE` (`RegistrySupplier<RecipeType<X>>`) and `*_SERIALIZER` – unchanged.
- `RecipeManager#getAllRecipesFor(type)` now returns `List<RecipeHolder<X>>` → use `holder.value()` / `holder.id()`.
  **`getId()` was removed from our recipes** (the id is `RecipeHolder#id()`).
- Helpers `ModRecipes.getSifterRecipeForItem(RecipeInput, Level)`, `getAnalyzerRecipeForItem(RecipeInput, Level)` (were `Container`),
  `getWorktableRecipeForItem(ContainerWithAnyFuel, Level)`, `getCultureVatRecipeForItem(ContainerWithAnyFuel, Level)` still return the
  recipe itself (nullable), not a holder.
- Inputs: `MultiOutputAndSlotsRecipe.MachineRecipeInput.of(ItemStack...)` (replaces `new SimpleContainer(stack)`);
  `WithFuelRecipe.ContainerWithAnyFuel(boolean anyFuel, ItemStack...)` / `(ItemStack input, ItemStack fuel)` is now a `RecipeInput`
  (no longer a `SimpleContainer`).
- Accessors unchanged: `getInput()`, `getFuel()`, `getDuration()`, `getFuelDuration()`, `getWeightedOutputs()` (`NavigableMap<Double, ItemStack>`);
  new: `WithFuelRecipe#getResult()`, `MultiOutputAndSlotsRecipe#getOutputs()` (`List<WeightedOutput(ItemStack stack, double weight)>`),
  `getTotal()` (`Optional<Double>`), `assemble(RandomSource)`.
- `getResultItem(HolderLookup.Provider)`, `assemble(input, HolderLookup.Provider)` (was `RegistryAccess`).
- JSON shapes = PORT_NOTES/data.md (codecs implemented exactly to those shapes).
- Datagen builders (`AnalyzerRecipeBuilder`, `SifterRecipeBuilder`, `WorktableRecipeBuilder`, `CultureVatRecipeBuilder`): same constructors and
  `addOutput/nothing`; implement 1.21 `RecipeBuilder` (`save(RecipeOutput)`, `save(RecipeOutput, String)`, `save(RecipeOutput, ResourceLocation)`,
  `unlockedBy(String, Criterion<?>)`). The `Result`/`FinishedRecipe` inner classes are gone.

### 3.2 Blocks / block entities
- All custom blocks: `use(...)` → `useItemOn(...)` wrapper (see `util/InteractionResults`), so behaviour is identical for both hands.
- Every block with an abstract-codec parent has `codec()`; `CODEC` statics exist on the `(Properties)` blocks.
- Block entities: `loadAdditional(CompoundTag, HolderLookup.Provider)` / `saveAdditional(CompoundTag, HolderLookup.Provider)` /
  `getUpdateTag(HolderLookup.Provider)`. Public BE API (getters/setters/ticks/`getEnergyStorage()`) unchanged.
- `ClearGlassBlock` extends `TransparentBlock` (was `AbstractGlassBlock`).
- `HomePortal` uses vanilla `entity.changeDimension(new DimensionTransition(...))`; `AnuPortal` still calls
  `ModDimensions.teleportToAnuLair(MinecraftServer, Entity)` (world: keep this signature).

### 3.3 Other vanilla facts used across packages (verified)
- `Mob#finalizeSpawn(ServerLevelAccessor, DifficultyInstance, MobSpawnType, SpawnGroupData)` – no `CompoundTag` (overrides too).
- `EntityType#create(ServerLevel, Consumer<T>, BlockPos, MobSpawnType, boolean, boolean)` (no `CompoundTag`).
- `ItemStack#hurtAndBreak(int, LivingEntity, EquipmentSlot)`; `LivingEntity.getSlotForHand(hand)`; `broadcastBreakEvent` → `onEquippedItemBroken(Item, EquipmentSlot)`.
- `Entity#igniteForSeconds(float)` (was `setSecondsOnFire`); `canChangeDimensions()` → `canUsePortal(boolean)` / `canChangeDimensions(Level, Level)`.
- `AbstractArrow#setKnockback` is gone (entity-b: `Javelin#setKnockback(int)` is still called by `JavelinItem`, please provide it, e.g. a field used in `doKnockback`).
- `ServerAdvancementManager#get(ResourceLocation)` → `AdvancementHolder`; `getOrStartProgress(AdvancementHolder)`.
- `DimensionDataStorage#get(SavedData.Factory<T>, String)`; core uses `new SavedData.Factory<>(AnuBoss.AnuLair::new, (tag, registries) -> new AnuBoss.AnuLair(), null)`
  (= the 1.20.1 behaviour `c -> new AnuLair()`), entity-b: keep a public no-arg `AnuLair()` constructor and `isAnuKilled()`; `save` becomes `save(CompoundTag, HolderLookup.Provider)`.
- Attribute modifiers: `new AttributeModifier(ResourceLocation id, double, Operation.ADD_VALUE/ADD_MULTIPLIED_BASE/ADD_MULTIPLIED_TOTAL)`.
- `EntityDimensions` is a record: `.width()`/`.height()`.

---------------------------------------------------------------------------------------------------------------------------------

## 4. Mixins / access widener (core) – things other packages call

- `mixin.AnimatableManagerAccessor#isFirstTick()` was **renamed to `fossil$isFirstTick()`** (GeckoLib 1.21.1 has a public
  `AnimatableManager#isFirstTick()`, the accessor would clash). compat (`AnimationProcessorOverride`): call `manager.isFirstTick()` directly.
- `mixin.AnimationControllerAccessor`: unchanged methods (`setIsJustStarting`, `getOverrideEasingTypeFunction`), GeckoLib packages flattened
  (`software.bernie.geckolib.animation.*`, `animatable.GeoAnimatable`, `cache.object.GeoBone`, `model.GeoModel`, `animation.state.BoneSnapshot`,
  `animation.keyframe.BoneAnimationQueue`, `loading.math.MathParser`).
- `AnimationControllerOverride.fixTransitions(...)` and `AnimationProcessorOverride.tickAdditiveAnimations(...)` (compat) are now called with
  `Map<String, GeoBone>` / `GeoModel<T>` (were `CoreGeoBone` / `CoreGeoModel`) – compat: adjust parameter types.
- `network.SyncedEntityDataHelper` (implemented on `SynchedEntityData` by mixin): unchanged.
- Access widener: removed entries for members that no longer exist (`Item$Properties.maxStackSize`, `Entity.portalTime`,
  `Entity.portalEntrancePos`, `Entity.eyeHeight`). Everything else kept. Need a new entry? Request it from core.
