# entity-b notes (1.20.1 → 1.21.1 NeoForge)

Scope: `common/src/main/java/com/github/teamfossilsarcheology/fossil/entity/**` except `entity/prehistoric/**`.
All paths below are relative to `common/src/main/java/com/github/teamfossilsarcheology/fossil/`.
All 35 changed files pass a `javac` parse-only check (`-XDshould-stop.ifNoError=PARSE`). Symbols were not resolved.

## What changed (per area)

**Synched data / spawn / packets (root + monster)**
- `defineSynchedData()` was changed to `defineSynchedData(SynchedEntityData.Builder)` in Javelin, ToyBall, ToyTetheredLog,
  ToyScratchingPost, PrehistoricSkeleton, Failuresaurus and AnuBoss. StoneTablet now has an empty override, because
  HangingEntity/BlockAttachedEntity no longer implement it.
- `finalizeSpawn` no longer takes the `CompoundTag` parameter. This applies to the overrides in Failuresaurus, FriendlyPiglin,
  SentryPiglin and AnuBoss, and to the calls in ThrownBirdEgg.
- `getAddEntityPacket()` became `getAddEntityPacket(ServerEntity)`:
  - ToyBase uses `new ClientboundAddEntityPacket(this, serverEntity)`.
  - StoneTablet and PrehistoricSkeleton use Architectury 13 `NetworkManager.createAddEntityPacket(this, serverEntity)`.
  - `EntitySpawnExtension` is unchanged.
- `setSecondsOnFire` became `igniteForSeconds` in TarSlime and FriendlyPiglin.
- ToyBall: `lerpTo` lost its `boolean` parameter.
- GeckoLib 4.9.3 package flattening (`core.animation.*` → `animation.*`, `core.object.PlayState` → `animation.PlayState`,
  `core.keyframe.BoneAnimation` → `animation.keyframe.BoneAnimation`, `core.animatable.instance` → `animatable.instance`).
  This affects Failuresaurus, PrehistoricSkeleton and all of `animation/*`. The API used (AnimationController, AnimationState,
  RawAnimation, Animation record, LoopType, GeckoLibCache, BakedAnimations, `adjustTick`, `tickOffset`) was checked against
  refs/geckolib.
- `ai/anu/AnuMoveControl`: mclib's `MathHelper.wrapDegrees` became `Mth.wrapDegrees`. mclib is no longer on the classpath.

**Projectiles**
- `Javelin`:
  - Uses the 1.21 constructor `AbstractArrow(type, owner, level, pickupStack, weapon=null)`.
  - Implements the new abstract `getDefaultPickupItem()`. It returns a wooden javelin and is only used by vanilla before
    our data is known.
  - Keeps overriding `getPickupItem()` with the old tier/ancient/damage logic, now in `createPickupItem`.
  - The `DAMAGE` component is only set when the damage is above 0, so returned undamaged javelins stack with fresh ones
    (core's suggestion).
  - Core's request: `setKnockback(int)`/`getKnockback()` were added. They are applied in an override of
    `doKnockback(LivingEntity, DamageSource)` with the exact 1.20.1 formula, after vanilla's weapon-enchantment knockback
    (no weapon is passed, so that part is 0). Knockback is also saved as the "Knockback" byte. 1.20.1 did not persist it.
  - It still calls `setPierceLevel((byte) 16)`, which is **private** in 1.21. See the request to core below.
- `ThrownBirdEgg`: `AbstractProjectileDispenseBehavior` was removed in 1.20.5. `getProjectile(info, cultivated)` now returns
  a `DispenseItemBehavior` (a `DefaultDispenseItemBehavior`) with the exact 1.20.1 logic: power 1.1, uncertainty 6,
  `stack.shrink(1)` and level event 1002. `BlockSource` is a record. The call sites in core's `ModEvents` need no change.

**Monsters**
- `AnuBoss`:
  - `BlockPathTypes` became `PathType`.
  - `canChangeDimensions()` became `canUsePortal(boolean)` and `canChangeDimensions(Level, Level)`; both return false.
  - `dropCustomDeathLoot(ServerLevel, DamageSource, boolean)`.
  - `new LargeFireball(level, this, new Vec3(x, y, z), 2)`.
  - `AnuLair`:
    - Has an explicit public no-arg constructor and keeps `isAnuKilled()`.
    - `load(CompoundTag, HolderLookup.Provider)` and `save(CompoundTag, HolderLookup.Provider)`.
    - The new constant `AnuLair.FACTORY = new SavedData.Factory<>(AnuLair::new, AnuLair::load, null)` is used by AnuBoss.
- `FriendlyPiglin`:
  - `getMobType()`/`MobType.UNDEAD` were removed. This needs the `minecraft:undead` entity type tag (request below).
  - `FollowOwnerGoal` lost its `canFly` parameter (was false).
  - Added `isFood(stack)` (now abstract in Animal) returning `stack.is(Items.WHEAT)`, the 1.20.1 default.
- `SentryPiglin`: `populateDefaultEquipmentEnchantments(level, random, difficulty)`.
- `TarSlime`:
  - The `remove()` override (which replaces Slime's split) now also calls `triggerOnDeathMobEffects` and
    `getBrain().clearMemories()`, like 1.21 `LivingEntity#remove`.
  - The `ModEntities` size changed, see below.
- `Failuresaurus`:
  - `jumpFromGround()` is public now.
  - `getCurrentSwingDuration()` is private in vanilla. It stays overridable through core's existing access widener
    `extendable method` entry.

**Root entities**
- `AnuDead`: `Entity#portalTime` is gone, so the double-teleport guard uses `isOnPortalCooldown()`/`setPortalCooldown(3)`.
  1.20.1 set portalTime to 10, which decayed by 4 per tick, so about 3 ticks.
- `StoneTablet`:
  - The `getWidth()/getHeight()` overrides are gone in HangingEntity. Both are kept as plain public methods for the
    renderer.
  - The new abstract `calculateBoundingBox(BlockPos, Direction)` reproduces the 1.20.1 `recalculateBoundingBox` pixel math
    exactly. 1.21 sets the entity position to the box center, which equals the old computed position.
- `Quagga`: `getPassengersRidingOffset` was removed (see ModEntities).
- `ModEntities`. The 1.20.5 builder moved eye height and riding offsets into the entity type:
  - `QUAGGA`: `.eyeHeight(1.52f)` (1.20.1 AbstractHorse eye height = 0.95 × height) and `.passengerAttachments(1.45f)`.
    The 1.20.1 seat was 0.75×1.6 − 0.25 + 0.25 = 1.2 with the player offset −0.35. In 1.21 the player vehicle attachment
    is 0.6, so 1.45 gives the same 0.85.
  - `TAR_SLIME`: `.sized(0.52f, 0.52f).eyeHeight(0.325f)`. 1.21 `Slime#getDefaultDimensions` scales by `getSize()` instead of
    `0.255 × getSize()`, so the old 2.04 base would be about 4× too large. 0.52 equals 2.04 × 0.255. The eye height is
    0.625 × height, as in 1.20.1.
  - `SENTRY_PIGLIN`: `.eyeHeight(1.74f)` (1.20.1 `PiglinBrute#getStandingEyeHeight`).
  - Prehistoric types are unchanged, as agreed with entity-a. `EntityType.Builder#build(String)` and Architectury
    `EntityAttributeRegistry` are unchanged. Spawn placements are registered by build in `RegisterSpawnPlacementsEvent`.
- `damagesource/ModDamageTypes`: `BootstapContext` became `BootstrapContext`, and `new ResourceLocation` became
  `FossilMod.location`.

**AI / pathfinding (1.20.5 rewrite)**
- `BlockPathTypes` became `PathType` everywhere.
- `NodeEvaluator#level` was replaced by `currentContext` (`PathfindingContext`: `getBlockState(pos)`, `level()`).
- Renames:
  - `getCachedBlockType(mob, x, y, z)` → `getCachedPathType(x, y, z)`
  - `getBlockPathType(BlockGetter, x, y, z)` → `getPathType(PathfindingContext, x, y, z)`
  - `getBlockPathType(BlockGetter, x, y, z, Mob)` → `getPathTypeOfMob(PathfindingContext, x, y, z, Mob)`
  - `getBlockPathTypeRaw` → `getPathTypeFromState`
  - `getBlockPathTypeStatic` → `getPathTypeStatic`
  - `getGoal` → `getTarget`
  - `BlockState#isPathfindable(level, pos, type)` → `isPathfindable(type)`
- `CenteredNodeEvaluator` and `FlightPathNavigation.FlightNodeEvaluator`: the
  `getBlockPathTypes(BlockGetter, x, y, z, EnumSet, BlockPathTypes, BlockPos)` override became
  `getPathTypeWithinMobBB(PathfindingContext, x, y, z)`, which returns the set. In 1.21 vanilla checks the center type
  itself inside `getPathTypeOfMob`.
- `evaluateBlockPathType` was removed from vanilla. Its replacement is `NavUtil.evaluatePathType(evaluator, context, mob, type)`,
  which has the same door/rail logic as 1.21 `getPathTypeWithinMobBB`.
- `NavUtil.isNoCollisionOnPath` and `DinoFollowOwnerGoal.canTeleportTo` create a `PathfindingContext(mob.level(), mob)` for
  `getPathType`.
- `PrehistoricAmphibiousNodeEvaluator` and `TrilobiteNodeEvaluator` were ported mechanically. Trilobite extends NodeEvaluator
  directly, so it implements the 5 new abstract methods.
- `PrehistoricPathFinder`: `getGoal` → `getTarget`. It still uses `Path#setDebug`, which is package-private in 1.21 and covered
  by the existing access widener entry.
- `PrehistoricPathNavigation#getGroundY`: the name is unchanged (it is Mojang's name), only `isPathfindable` changed.
- Unchanged and checked: the goals (DelayedAttackGoal family extends Goal, not MeleeAttackGoal), the `PanicGoal` protected API,
  AvoidEntityGoal, TargetGoal, RandomSwimmingGoal, MoveControl/LookControl/BodyRotationControl, the Anu phases and
  InstructionSystem.

**Not changed (verified unaffected)**: `data/*` (Gson loaders: `SimpleJsonResourceReloadListener(Gson, String)` and
`apply(Map, ResourceManager, ProfilerFiller)` are unchanged in 1.21.1), `variant/*`, `util/*`, `animation/*` apart from
imports, and all network call sites (core's PayloadChannel keeps the API).
More Hitboxes usages were checked against the public `DarkPred/morehitboxes` 1.21.1 branch:
- `HitboxDataLoader.HITBOX_DATA.getHitboxes(rl)`
- `HitboxData#getFrustumWidthRadius/Height`
- `EntityHitboxData#getAttackBoxData().attackBoxEndTime()`, `getAttackBounds()`, `hasCustomParts()`, `getHeadRadius()`

## PORT-TODO list (file:line)
- `entity/Javelin.java:48`, `:109`: `setPierceLevel` is private in 1.21. This does not compile without the access widener
  entry (core request 1).
- `entity/AnuDead.java:50`: portal cooldown instead of portalTime. Verify that interacting with both hands does not
  teleport twice.
- `entity/StoneTablet.java:82`: `calculateBoundingBox` port. Verify placement and hitbox for all sizes.
- `entity/ThrownBirdEgg.java:53`: the dispenser behaviour rewrite. Verify in game.
- `entity/ModEntities.java:84`: quagga eye height and seat height derived from 1.20.1 math. Verify in game.
- `entity/ModEntities.java:112`: tar slime base size 0.52. Verify the hitbox per size.
- `entity/ai/navigation/FlightPathNavigation.java:41`: 1.20.1 FlyNodeEvaluator called `super.getBlockPathTypes`, so the
  centered override was not used there. Verify whether 1.21.1 `FlyNodeEvaluator#getPathTypeOfMob` uses
  `this.getPathTypeWithinMobBB`. If it does, flying mobs now use the centered check.
- `entity/ai/navigation/CenteredNodeEvaluator.java:35`: keeps the 1.20.1 `getStart` "airborne" branch. Vanilla 1.21 scans
  down from y+1.
- `entity/ai/navigation/PrehistoricAmphibiousNodeEvaluator.java:21`: written from mappings.dev signatures and memory of
  1.21 vanilla. Verify amphibious pathing.
- `entity/ai/navigation/TrilobiteNodeEvaluator.java:32`: verify sea-floor pathing. The cache is never cleared, the same
  as in 1.20.1.

## Assumptions / sources
- Verified on mappings.dev (1.21.1, Mojang column):
  - `AbstractArrow`, `ThrowableItemProjectile`, `ProjectileDispenseBehavior`, `DispenserBlock`
  - `HangingEntity`, `BlockAttachedEntity`, `LeashFenceKnotEntity`
  - `Entity`, `LivingEntity` (`getDimensions` is **final**; `getDefaultDimensions`, `jumpFromGround` public,
    `triggerOnDeathMobEffects`, `isAffectedByPotions`)
  - `Mob`, `TamableAnimal`, `FollowOwnerGoal`, `PanicGoal`
  - `AbstractHorse`, `AbstractChestedHorse`, `Slime`, `LargeFireball`, `ClientboundAddEntityPacket`, `EntityType$Builder`
  - `NodeEvaluator`, `WalkNodeEvaluator`, `PathfindingContext`, `Path`, `PathFinder`, `PathNavigation`
- NeoForge patches: Slime (`getDefaultDimensions` = super × size), WalkNodeEvaluator, PathfindingContext, PathNavigation.
- Architectury 13: `NetworkManager.createAddEntityPacket(Entity, ServerEntity)`, `EntitySpawnExtension`,
  `EntityAttributeRegistry`.
- GeckoLib 4.9.3 source in refs.
- From memory, not verified line by line:
  - 1.21 vanilla method bodies of WalkNodeEvaluator/FlyNodeEvaluator (`getPathTypeOfMob`/`getPathTypeWithinMobBB`/
    `getCachedPathType` call chain) and AbstractArrow#onHitEntity/doKnockback.
  - The 1.20.1 eye heights of AbstractHorse (0.95h), Slime (0.625h) and PiglinBrute (1.74), and the 1.20.1
    AbstractChestedHorse riding offset (−0.25).
- `ServerPlayer#teleportTo(ServerLevel, x, y, z, yRot, xRot)` and `Entity#setPortalCooldown/isOnPortalCooldown` still exist
  in 1.21.1.

## Requests to other owners
**core**
1. `fossil.accesswidener`: add `accessible method net/minecraft/world/entity/projectile/AbstractArrow setPierceLevel (B)V`.
   `Javelin` needs it, because the method is private in 1.21. Keep the existing entries that entity-b relies on:
   `extendable method LivingEntity getCurrentSwingDuration ()I` (Failuresaurus), `Path setDebug(...)` (PrehistoricPathFinder),
   `MoveControl$Operation` (controls).
2. `JavelinItem`: nothing to change. `Javelin#setKnockback(int)` exists and the constructor signature is unchanged.
3. `event/ModEvents.java:68` and `mixin/ExplosionMixin.java:33`: you can use `AnuBoss.AnuLair.FACTORY` instead of
   `new SavedData.Factory<>(AnuLair::new, (tag, reg) -> new AnuLair(), null)`.
   - The current lambda (like 1.20.1's `c -> new AnuLair()`) ignores the stored tag.
   - If that factory is the first to load `"anu_lair"`, `DimensionDataStorage` caches an empty instance and `isAnuKilled()`
     and the barriers are lost.
   - Optional, and a behaviour fix rather than a port change.
4. `ThrownBirdEgg.getProjectile(...)` now returns `DispenseItemBehavior`. `DispenserBlock.registerBehavior` in ModEvents
   accepts it unchanged.

**client**
1. `client/gui/debug/navigation/PathingDebug#getPathfindingMalus` and `setPathfindingMalus` must take `PathType` (was
   `BlockPathTypes`). `entity/ai/navigation/PrehistoricAmphibiousNodeEvaluator#getNode` calls it with a `PathType`. The debug
   navigations (WaterPathNavigation, SweepPathNavigation, PlayerPathNavigation, DebugCentered*) need the same pathfinding
   renames as in "AI / pathfinding" above. `NavUtil.evaluatePathType(...)` is public if useful.
2. `StoneTablet#getWidth()/getHeight()` still exist (public, no longer overrides) for `StoneTabletRenderer`.

**data** and **build** (`ModEntityTypeTagsProvider`)
- Add `fossil:friendly_piglin` to `minecraft:undead` (`data/minecraft/tags/entity_type/undead.json`,
  `{"replace": false, "values": ["fossil:friendly_piglin"]}`; datagen: `tag(EntityTypeTags.UNDEAD).add(ModEntities.FRIENDLY_PIGLIN.get())`).
  This replaces the 1.20.1 `getMobType() == MobType.UNDEAD`, which drives Smite, inverted healing/harming and
  regeneration/poison immunity. Vanilla's `sensitive_to_smite` and `inverted_healing_and_harm` include `#minecraft:undead`.

**entity-a**
- No API changes from my side. My code relies on these Prehistoric methods staying as they are: `getScale()`,
  `getEntityHitboxData()`, `attributes().sprintMod()`, `PrehistoricGroupData(int)`, `AISystem#saveAdditional/load(CompoundTag)`
  and `serverTick()`.
- `PrehistoricAmphibiousNodeEvaluator`, `TrilobiteNodeEvaluator` and the navigations were ported in place. Constructors and
  class names are unchanged.
