# entity-a notes (1.20.1 → 1.21.1 NeoForge)

Scope: `common/src/main/java/com/github/teamfossilsarcheology/fossil/entity/prehistoric/**` (91 files, 33 changed).
Paths below are relative to that directory.

Nothing was compiled. `javac -XDshould-stop.ifError=PARSE` over all 91 files reports only missing-package and missing-symbol
errors (there is no classpath), so there are no syntax errors.

**References used.** NeoForge 1.21.1 patches, Architectury 13 and GeckoLib 1.21.1 from `refs/`. I also used decompiled
**vanilla 1.21.1 and 1.20.1 sources**, from the `client1.21.1` and `client1.20.1` branches of
`github.com/extremeheat/extracted_minecraft_data` (sparse clone, Mojang names, read only; I put them in `/tmp/ea-vanilla*`). This
gives exact vanilla 1.21.1 bodies and is worth using in phase 2. I also used **More Hitboxes 1.21.1 source**, which is public at
`github.com/DarkPred/morehitboxes` on branch `1.21.1` (1.9.4-alpha). Its API (`GeckoLibMultiPartEntity`, `MultiPartEntity`,
`EntityHitboxData`, `EntityHitboxDataFactory.create(entity)`, `AnchorData#getAnchorPos`, `MultiPart#getEntity`) matches our
usage exactly, so no PORT-TODO was needed for it.

## Changes by category

**SynchedEntityData** (14 files): `defineSynchedData(SynchedEntityData.Builder builder)` and `builder.define(...)`. Default
values are unchanged.

**finalizeSpawn** (Prehistoric, PrehistoricFish, Mammoth, Elasmotherium, plus the calls in Prehistoric and DinosaurEgg): the
`CompoundTag` parameter was removed.
- `NbtCondition` variants used the dataTag in 1.20.1, which was the spawn egg's root item tag. 1.21.1 `EntityType#create`
  calls `finalizeSpawn` first and then applies the egg's `ENTITY_DATA` through `Entity#load`. So `Prehistoric` sets the transient
  flag `spawnFinalized` in `finalizeSpawn` and tests the NBT variants in `load(CompoundTag)`. The flag is cleared on the first
  tick.
- Random spawn bonus: `AttributeModifier(Mob.RANDOM_SPAWN_BONUS_ID, gaussian * 0.05, ADD_MULTIPLIED_BASE)` is applied with
  `addOrReplacePermanentModifier`. Adding the same id twice throws in 1.21. In 1.20.1 a second call stacked a second modifier
  with a random UUID.
- As in 1.20.1, `Prehistoric#finalizeSpawn` does not call super. NeoForge's `Mob#getSpawnType()` therefore stays null for
  prehistoric mobs, which matches Forge 1.20.1.

**Dimensions / eye height.** `LivingEntity#getDimensions(Pose)` is **final** in 1.21.1. It returns `SLEEPING_DIMENSIONS`
(0.2×0.2) for `Pose.SLEEPING` and otherwise `getDefaultDimensions(pose).scale(getScale())`.
- All 15 subclass `getDimensions` overrides became `getDefaultDimensions` overrides. The pose-specific `.scale(w, h)` is
  commutative with the scale, so the result is identical.
- `Prehistoric#getDefaultDimensions` returns the unscaled type dimensions. The new `Prehistoric#getPrehistoricDimensions(pose)`
  is the 1.20.1 `getDimensions`: `getDefaultDimensions(pose).scale(getScale())` with the 1.20.1 eye height (0.2 when sleeping,
  0.85·h otherwise). The custom `refreshDimensions`, the `DIMENSION_VER` client fallback and `push` all use it, so sleeping
  dinosaurs keep their real hitbox.
- `Prehistoric#getAgeScale()` returns 1. The 1.20.1 baby 0.5 factor lived in `getScale`, which Prehistoric overrides.
- `PrehistoricFish` gets `getScale() = isBaby ? 0.5 : 1` and `getAgeScale() = 1`, so `getScale` keeps its 1.20.1 meaning.
- `EntityDimensions` record accessors: `width()`, `height()`.

**Riding.** The `getPassengersRidingOffset` and `getMyRidingOffset` overrides were removed in 1.20.2.
- `Prehistoric#getPassengersRidingOffset()` is kept as a plain method. The Mammoth, Parasaurolophus, Dilophosaurus and
  Megaloceros overrides still override it.
- New `Prehistoric#getMyRidingOffset()` (overridden by `PrehistoricLeaping`, -0.025) is exposed to vanilla vehicles through
  the `getVehicleAttachmentPoint` override.
- New `Prehistoric.getLegacyRidingOffset(passenger, vehicle)` returns -0.35 for players (the 1.20.1 value). For other
  passengers it returns minus the vehicle attachment point.
- `positionRider` (Prehistoric, PrehistoricSwimming grab) uses this helper. The behaviour is the same as 1.20.1.
- `getPassengerAttachmentPoint` is overridden to `(0, getPassengersRidingOffset(), 0)` so that vanilla
  `getPassengerRidingPosition` users (such as dismounting) agree.

**Attributes**
- Step height: `setMaxUpStep(x)` became `getAttribute(Attributes.STEP_HEIGHT).setBaseValue(x)` (Prehistoric, Gallimimus).
- Sprint modifier: the UUID was replaced by the id `minecraft:sprinting`.
- `Attributes.*` are already Holders, and the `GENERIC_` prefix is not used in 1.21.1, so call sites are unchanged.

**MobType removed (1.20.5)**
- New `Prehistoric#isWaterMobType()` (PrehistoricSwimming returns true; Megalograptus and Trilobite return false, because they
  were ARTHROPOD) keeps the constructor's water malus and canFloat logic.
- The ARTHROPOD overrides in Arthropleura and Meganeura were removed with a comment.
- Vanilla effects (Impaling, Bane of Arthropods) now come from entity type tags, which need a data change (see Requests).

**canBreatheUnderwater is final and tag-based** (`minecraft:can_breathe_under_water`)
- The PrehistoricSwimming override (always true) was removed. Its `handleAirSupply` already refills air in water, so drowning
  behaviour is unchanged. The tag is still needed for other callers such as `FoodMappings`.
- Meganeura (1.20.1: `isBaby()`) is not put in the tag. Instead, larvae get `setAirSupply(getMaxAirSupply())` in `baseTick`.

**Pathfinding**: `BlockPathTypes` became `PathType` (Prehistoric, PrehistoricSwimming, Trilobite).

**Taming / XP**
- `setTame(b)` became `setTame(b, true)`, which applies side effects like the 1.20.1 version did.
- `reassessTameGoals()` became `applyTamingSideEffects()`.
- `getExperienceReward()` became `getBaseExperienceReward()` (protected). The vanilla final wrapper adds enchantment effects.

**Leash**: `canBeLeashed(Player)` no longer exists (1.21 `Leashable`), and vanilla never passes the player. The owner check was
kept as `Prehistoric#canBeLeashedBy(Player)` and needs a hook (see Requests). Until that hook exists, any player can leash a
tame dinosaur.

**Items / data components**
- `PrehistoricSwimmingBucketable#saveToBucketTag` and `PrehistoricFish#saveToBucketTag` write with
  `CustomData.update(DataComponents.BUCKET_ENTITY_DATA, …)`, using the same keys.
- Bucket items get `.component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)` (`PrehistoricEntityInfo`).
- `hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand))` in Mammoth, Elasmotherium and Therizinosaurus.

**Spawn packets**: `getAddEntityPacket(ServerEntity)` calls `NetworkManager.createAddEntityPacket(this, serverEntity)`
(Architectury 13), in Prehistoric and DinosaurEgg. `EntitySpawnExtension` is unchanged.

**Advancements**: `getAdvancements().get(id)` returns an `AdvancementHolder`; `getOrStartProgress(holder)`.

**Spawn predicates**: `PrehistoricFish.canSpawn` and `Coelacanth.canCoelacanthSpawn` now take `ServerLevelAccessor`, to match
`SpawnPredicate` (build request).

**NBT**: `MeganeuraAttachSystem` reads `AttachPos` from both the 1.20.1 compound format `{X,Y,Z}` and the 1.21 int array through
`NbtUtils.readBlockPos(tag, key)`.

**GeckoLib 4.x (1.21.1)**: package moves only.
- `geckolib.core.animatable.instance` → `geckolib.animatable.instance`
- `geckolib.core.animation` → `geckolib.animation`
- `core.object.PlayState` → `animation.PlayState`

`Animation.LoopType`, the `AnimationState` API, keyframe handlers, `ParticleKeyframeData#getEffect` and
`SoundKeyframeData#getSound` were checked in refs and are unchanged.

**Unchanged after verification against vanilla 1.21.1**: `MobEffects.*` (Holders, call sites fine), `GameEvent.SHEAR`,
`spawnAtLocation`, `Biome#warmEnoughToRain`, `Boat#getVariant().getPlanks()`, `TemptGoal` (Ingredient is a `Predicate`),
`InteractionResult.sidedSuccess`, `kill()`, `killedEntity(ServerLevel, …)`, `causeFallDamage`, `checkFallDamage`,
`tickHeadTurn`, `findFreePosition`, `ClientboundSoundPacket`, `Mob#canAttackType`, `Shearable`. I compared every overridden
method name against vanilla. Other than `canBreatheUnderwater` and `getDimensions`, no overridden method is final in 1.21.1.

## PORT-TODO list
- `base/Prehistoric.java:309`, `:465`: the `Entity#eyeHeight` access widener entry (see Requests/core). This does not compile
  without it.
- `base/Prehistoric.java:439`: NbtCondition variants are now read from the spawn egg's `minecraft:entity_data`. Verify in game.
  No shipped variant uses `"type": "nbt"` (grep of `data/fossil/variants`).
- `base/Prehistoric.java:620`: the owner-only leash check needs a hook (core request).
- `base/Prehistoric.java:643`: rider seat height (player -0.35 kept). Check in game.
- `base/PrehistoricSwimming.java:235`: NeoForge calls `jumpInFluid(FluidType)`, not `jumpInLiquid`, so the override is dead on
  NeoForge. It was also dead on Forge 1.20.1 and was kept as is.

## Assumptions / known differences
- Old 1.20.1 **fossil bucket items** keep their data in `minecraft:custom_data` after DFU, because the vanilla componentization
  fix only moves bucket data for vanilla bucket items. Such buckets spawn default mobs. A migration would have to read
  `CUSTOM_DATA` in the bucket item (core/item) and was not done.
- On the client, a **sleeping** prehistoric's `getDimensions(Pose.SLEEPING)` called by vanilla code returns 0.2×0.2. The
  actual box and eye height come from our `refreshDimensions`, so nothing in our code depends on it. The vanilla callers are
  `getEyeHeight(Pose)`, which also gives 0.2 (same as 1.20.1), and the player-only pose checks.
- Step height is now a synced attribute base value (saved in "attributes"), where 1.20.1 used a field. The value is
  recomputed every tick.

## Requests to other owners
**core**
1. `fossil.accesswidener`: re-add `accessible field net/minecraft/world/entity/Entity eyeHeight F`. The field exists in 1.21.1
   (`private float eyeHeight;`, assigned in `refreshDimensions`). It was removed in phase 1 as nonexistent.
   `Prehistoric#refreshDimensions` and `onSyncedDataUpdated` write it.
2. Owner-only leashing (1.20.1 `canBeLeashed(Player)`): in `event/ModEvents`, register `InteractionEvent.INTERACT_ENTITY`. When
   `target instanceof Prehistoric p && player.getItemInHand(hand).is(Items.LEAD) && !p.isLeashed() && !p.canBeLeashedBy(player)`,
   return `EventResult.interruptFalse()`. Returning interruptFalse (not pass) keeps the lead and blocks `Entity#interact`.
3. `network/debug/C2SStructureMessage.java:88`: `velociraptor.finalizeSpawn(..., null, null)` still passes 5 arguments. Drop
   the last `null`.
4. FYI: spawn eggs and `ArtificialHoneycombItem` no longer pass item NBT to `finalizeSpawn`. NBT variants are read from
   `ENTITY_DATA` (see above).

**client**
1. GeckoLib 4.9 `GeoEntityRenderer` already applies `LivingEntity#getScale()` itself (`poseStack.scale(nativeScale)`,
   `refs/geckolib/.../renderer/GeoEntityRenderer.java:244`). `PrehistoricGeoRenderer:36` and `PrehistoricFishGeoRenderer:22`
   also call `withScale(animatable.getScale())`, so mobs would render at scale². Drop the `withScale`.
   `Prehistoric#getScale` and `PrehistoricFish#getScale` keep their 1.20.1 meaning (growth scale; 0.5 for baby fish).
2. If client code needs a prehistoric's real hitbox for a given pose, use `Prehistoric#getPrehistoricDimensions(pose)`, not
   `getDimensions(pose)`, which gives 0.2×0.2 for SLEEPING.

**data** (generated fallback JSON) and **build** (`ModEntityTypeTagsProvider`): vanilla entity type tags that replace
MobType/WaterAnimal behaviour, all with `"replace": false`, all ids `fossil:`.
- `minecraft:aquatic` (was MobType.WATER; drives Impaling through `sensitive_to_impaling`): aquilolamna, crassigyrinus,
  diplocaulus, henodus, ichthyosaurus, liopleurodon, megalodon, mosasaurus, plesiosaurus, sarcosuchus, spinosaurus,
  tiktaalik, alligator_gar, coelacanth, nautilus, sturgeon.
- `minecraft:arthropod` (Bane of Arthropods): arthropleura, meganeura, megalograptus, dicranurus, lonchodomas, scotoharpes,
  walliserops.
- `minecraft:can_breathe_under_water` (was the `canBreatheUnderwater()` override or WaterAnimal): every id in the aquatic list
  plus megalograptus, dicranurus, lonchodomas, scotoharpes and walliserops. **Not** meganeura.

**build**
- More Hitboxes has a public 1.21.1 branch: `DarkPred/morehitboxes` branch `1.21.1`, `mod_version=1.9.4-alpha`, built against
  NeoForge 21.1.66 and GeckoLib 4.8.3. Our usage matches its API. Use 1.9.4-alpha or later if the 1.9.3 artifact does not
  exist.
- `PrehistoricFish::canSpawn` and `Coelacanth::canCoelacanthSpawn` now have the `SpawnPredicate` signature with
  `ServerLevelAccessor`.

**entity-b**
- Prehistoric entity types need no `.eyeHeight(...)` or `.passengerAttachments(...)` in `ModEntities`, because Prehistoric
  computes both itself.
- Public API changes in Prehistoric that callers may need:
  - `getDimensions(pose)`: use `getPrehistoricDimensions(pose)` when the sleeping size matters.
  - New `isWaterMobType()`, `canBeLeashedBy(Player)`, `getMyRidingOffset()`, `getLegacyRidingOffset(...)`.
  - `getPassengersRidingOffset()` is no longer a vanilla override.
- Unchanged: `getMatingCooldown()`, `getAge()`, `data()`, `PrehistoricEntityInfo.isMammal(Mob)`.
