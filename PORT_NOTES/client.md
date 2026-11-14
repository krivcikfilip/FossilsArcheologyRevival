# client agent notes (1.20.1 → 1.21.1 NeoForge)

Scope: `common/src/main/java/com/github/teamfossilsarcheology/fossil/client/**` (109 files, 56 changed). Nothing compiled
(no Maven); every changed file passes a `javac` syntax-only parse. Vanilla signatures were checked on mappings.dev (1.21.1),
NeoForge patches (refs/NeoForge), GeckoLib 4.9.3 (refs/geckolib), Architectury 13 (refs/architectury-api) and the 1.21.1
assets (refs/mcmeta-assets).

## What changed (per area)

**Registration (`ClientInit`)**
- Menu screens: `MenuScreens.register(...)` in `later()` → `ModMenus.X.listen(type -> MenuRegistry.registerScreenFactory(type, Screen::new))`
  from `immediate()`. NeoForge posts `RegisterMenuScreensEvent` inside the Minecraft constructor (`ClientHooks.initClientHooks`),
  before `FMLClientSetupEvent`, so registering in `later()` would be too late. `RegistrySupplier#listen` fires during `RegisterEvent`,
  which runs earlier (`CommonModLoader.begin` → `GameData.postRegisterEvents`). The AW entry for `MenuScreens#register` is no longer used.
- `ClientGuiEvent.RENDER_HUD` lambdas take `(GuiGraphics, DeltaTracker)`. `new ResourceLocation("using")` → `withDefaultNamespace`.
- Everything else (entity/BE renderers, particles, key mappings, reload listeners, render types, block colors, item properties)
  is unchanged. The Architectury 13 NeoForge impls were checked; they still work from the same call sites (constructor / client setup).
- No `ModelLayerLocation`s are used (all models use `createBodyLayer().bakeRoot()`), so `EntityModelLayerRegistry` is not needed.

**GeckoLib 4.2 → 4.9.3**
- Package flattening: `core.object.Color` → `util.Color`, `core.animatable.GeoAnimatable` → `animatable.GeoAnimatable`,
  `core.animation.*` → `animation.*`, `CoreGeoBone` → `cache.object.GeoBone`, `RenderUtils` → `RenderUtil`.
- Render callbacks take a packed `int colour` instead of `float r, g, b, a` (`preRender`, `actuallyRender`, `renderRecursively`,
  `renderCubesOfBone`, `renderChildBones`).
- **Double scaling (confirmed, entity-a request):** `GeoEntityRenderer#actuallyRender` (4.9) does `poseStack.scale(nativeScale)` with
  `nativeScale = LivingEntity#getScale()`, which `Prehistoric#getScale()` overrides. `withScale(animatable.getScale())` was removed from
  `PrehistoricGeoRenderer` and `PrehistoricFishGeoRenderer` (fish: getScale is the vanilla attribute, 1 by default). The scale is still
  applied before `applyRotations`, as in 1.20.1, so the Dinnerbone translation is unchanged.
  No client code calls `getDimensions` on prehistorics, so nothing else needed aligning.
- `applyRotations(..., partialTick)` is deprecated and no longer called; the overrides use the 6-argument `(..., partialTick, nativeScale)` version.
- `SkeletonRenderer` (custom `GeoRenderer`): new signatures plus GeckoLib's 1.21 `checkAndRefreshBuffer` in `renderRecursively`.

**Vanilla models / renderers**
- `renderToBuffer(PoseStack, VertexConsumer, int, int, int color)` and `ModelPart#render(..., int color)` (ARGB). Callers with the
  white tint use the final 4-argument `renderToBuffer`. Alpha tints: `FastColor.ARGB32.color(a, 255, 255, 255)` (AnuDead, AnuTotem overlay).
- `ToyBallModel`: `DyeColor#getTextureDiffuseColors` (float[]) → `getTextureDiffuseColor` (int); the dye replaces the RGB and the tint alpha is kept.
- Vertex API (AnuBarrier, StoneTablet, AnuTotem beams, OverlayRenderer, debug renderers): `addVertex/setColor/setUv/setOverlay/setLight/setNormal(Pose, ...)`;
  Tesselator: `Tesselator.getInstance().begin(mode, format)` + `BufferUploader.drawWithShader(builder.buildOrThrow())`.
- `AnuTotemRenderer`: `setupRotations(..., float scale)` (1.20.5). The beams use `RenderType.dragonRays()` (TRIANGLES) instead of `lightning()`
  (QUADS). Vanilla's dragon death rays, which this code copies, made the same switch in 1.20.5; the 9 vertices per beam are 3 triangles.
- `PlantModelBakery`: `TextureAtlasSprite#getU/getV` take 0..1 since 1.20.2 → `/ 16f` (as vanilla `FaceBakery`). Without it the plant
  UVs would be 16× off.
- `ResourceLocation` construction (FriendlyPiglin, LaserPoint, DinopediaBioLoader, InstructionsList).

**Screens**
- Container screens: removed the explicit `renderBackground(g)` before `super.render`. `Screen#render` renders the background since 1.20.2,
  and a second blur per frame throws in 1.21. `AbstractContainerScreen#renderBackground` still draws the transparent gradient + `renderBg`,
  so it looks the same as in 1.20.1.
- `DinopediaScreen`: the book and entity are drawn in an overridden `renderBackground` (order kept: background → book/entity → widgets →
  text). In-world it uses `renderTransparentBackground`, the exact 1.20.1 gradient, instead of the 1.21 blur, which would also wreck the
  3D entity preview's depth. `RenderSystem.getModelViewStack()` is a `Matrix4fStack`. Usernames: authlib 6 `fetchProfile(uuid, true)` → `ProfileResult`.
- `DebugScreen`/`PathingScreen`: no-op `renderBackground` (1.20.1 had no background on these; the world must stay visible).
- Selection lists (`DebugSelectionList`, `AbstractAnimationList`, `EntityList`, `SkeletonEditTab.ModelsList`): new ctor `(mc, width, height, y, itemHeight)`
  with height = old y1 − y0; `x0/x1/y0` → `setX/getX/getY`; `setRenderBackground/TopAndBottom(false)` → no-op `renderListBackground`/`renderListSeparators`;
  `renderList` → the extra widgets render in `renderWidget` after the list (outside its scissor); `render` overrides → `renderWidget` (`AbstractWidget#render` is final).
- `EggTab`/`InfoTab` slider `render` overrides → `renderWidget`.
- `InstructionsList`: `resource_packs.png` → sprites `transferable_list/move_up|move_down` via `blitSprite` (same sub-regions as the old blits);
  `Screen.BACKGROUND_LOCATION` (removed texture) → `Screen.MENU_BACKGROUND`.
- `Instruction` (also used by entity/network): `NbtUtils.writeBlockPos/readBlockPos` changed format (int array / Optional). It now uses private helpers
  that keep writing the 1.20.1 `{X,Y,Z}` compound and read both formats, so instructions saved in entity NBT stay compatible.
- Debug world rendering (`InstructionRenderUtil`, `PathingRenderUtil`): `MultiBufferSource.immediate(ByteBufferBuilder)` (own static builder),
  `PoseStack#mulPoseMatrix` → `mulPose(Matrix4f)`.
- Debug pathfinding (`gui/debug/navigation`): `BlockPathTypes` → `PathType`, `isPathfindable(level, pos, type)` → `isPathfindable(type)`.

Unchanged after checking: particles (providers / `SimpleParticleType`; no custom `ParticleOptions` exist), block entity renderers other than
AnuBarrier, `CustomItemRenderer` (`BlockEntityRenderDispatcher#renderItem` still exists), filters (`ClientGuiEvent` signatures still fit),
loaders, `FailuresaurusModel`, `SkeletonModel`, `QuaggaModel`, piglin models, `AncientHelmetRenderer` (public no-arg ctor kept).

## PORT-TODO list (relative to `common/src/main/java/com/github/teamfossilsarcheology/fossil/client/`)
- `ClientInit.java:125`: check in game that all 5 machine screens open (MenuRegistry + `RegistrySupplier#listen` timing).
- `gui/DinopediaScreen.java:66`: authlib 6 `MinecraftSessionService#fetchProfile(UUID, boolean)` / `com.mojang.authlib.yggdrasil.ProfileResult`.
- `gui/DinopediaScreen.java:105`: visual check of the entity preview (modelview `Matrix4fStack`, GUI depth).
- `gui/debug/AbstractAnimationList.java:50`: the `getEntryAtPosition` override needs the AW `extendable` entry converted to an AT on NeoForge.
- `renderer/entity/PrehistoricGeoRenderer.java:38`: the size depends on `Prehistoric#getScale` overriding `LivingEntity#getScale`; check the size in game.
- `renderer/entity/AnuTotemRenderer.java:71`: visual check of the beams (`dragonRays()` instead of `lightning()`).
- `gui/debug/instruction/InstructionsList.java:88,105,234`: visual check of the debug arrows and list background.

## Assumptions
- `FastColor.ARGB32.color(int alpha, int rgb)` / `color(a, r, g, b)` argument order (confirmed by NeoForge patch call sites).
- `GuiGraphics#blitSprite(sprite, texW, texH, u, v, x, y, w, h)` argument order (as used by vanilla's `BossHealthOverlay`).
- AbstractSelectionList: the extra debug widgets (controller buttons above the list) would have been clipped if rendered inside the
  1.21 scissor, so they are rendered after the list instead. This only affects debug builds.
- Debug-only code (all of `gui/debug`) is ported mechanically and has the lowest verification priority.

## Requests to other owners
**core**
- `fossil.accesswidener`: keep `extendable method AbstractSelectionList getEntryAtPosition (DD)L...$Entry;` (still used). The
  `accessible method MenuScreens register` and `accessible class MenuScreens$ScreenConstructor` entries are now unused (client uses
  Architectury `MenuRegistry`), so they can be removed. Keep `CreativeModeInventoryScreen scrollOffs/selectedTab` (filters).
- `DebugLevelRendererMixin`: unchanged calls `PathingRenderer.render(PoseStack, MultiBufferSource, float, long)` and
  `InstructionRenderer.render(PoseStack, MultiBufferSource, float, long, Frustum)` (signatures kept).

**entity-a**
- Keep `Prehistoric#getScale()` as the override of `LivingEntity#getScale()`. GeckoLib 4.9 applies it as the model scale; the renderer no
  longer scales by it. If it is renamed, tell client (then `PrehistoricGeoRenderer#preRender` needs `withScale(...)` again).
- Client still uses: `Prehistoric#getClimbingDirection/prevClimbDirection/climbTick/prevClimbTick`, `textureLocation`/`refreshTexturePath()`,
  `PrehistoricFlying#prevPitch/currentPitch/prevYaw/currentYaw/isFlying/isTakingOff/isFlyingUp/isFlyingDown/setFlyingUp/setFlyingDown`,
  `Arthropleura#isBee`, `Meganeura#getAttachSystem()`, `DinosaurEgg#getScaleOverride/isGoldenEgg/isTooCold/getHatchingTime/getTotalHatchingTime`,
  `PrehistoricAnimatable#getAllAnimations`, `getDebugTag()`, `setScaleOverride`, `data()`, `info()`, `moodSystem`.

**entity-b**
- `PrehistoricSkeleton` must stay a `GeoAnimatable` with public `modelLocation`/`textureLocation` and `getScale()`; `Javelin#getTier()/isAncient()`,
  `ToyBall#getColor()` (DyeColor), `ToyTetheredLog#animationPlaying/animationTick/animationX/animationZ`, `ToyBase#isVanillaWood`, `getWoodTypeName()`.
- `Instruction` NBT keeps the 1.20.1 `{X,Y,Z}` compound for "Target" (`InstructionSystem` saves/loads it). No change needed on your side.
- `PathingDebug.getPathfindingMalus(PathType)` now takes `PathType` (matches your `PrehistoricAmphibiousNodeEvaluator`).

**build**
- No API change for neoforge: `ClientInit.immediate()/later()`, `PlantBlockModel.LOADER`, `PlantModelBakery.bakeFace(...)`, `OverlayRenderer.renderTar(PoseStack)`,
  `CustomItemRenderer.INSTANCE` are unchanged. Menu screens no longer need anything from `later()`.

**compat**
- JEI/REI screen classes (`AnalyzerScreen`, `CultureVatScreen`, `SifterScreen`, `WorktableScreen`) and their constants (`PROGRESS_WIDTH`, ...) are unchanged.
