# compat agent notes (1.20.1 -> 1.21.1 NeoForge)

Scope: `common/src/main/java/com/github/teamfossilsarcheology/fossil/compat/**` (jei, rei, emi, geckolib, carryon, farmers).
No other files edited. Reference sources cloned to `/home/claude/port/refs/`:
`JEI` (branch 1.21.1, specificationVersion 19.70.0), `REI` (16.x-1.21), `EMI` (1.21, mod_version 1.1.24), `FarmersDelight` (1.21, 1.3.4),
`CarryOn` (1.21.1, 2.2.6), `ClothConfig` (v15, for REI's ScrollingContainer); GeckoLib from the existing `refs/geckolib` (4.9.3).
These are the versions the build agent pinned in `gradle.properties`.

Check: `javac -XDshould-stop.ifError=PARSE` parse of all 32 compat files is clean. No type check was possible because there are no
Minecraft or mod jars here.

## Changes

### Recipes (all three recipe viewers)
- `RecipeManager#getAllRecipesFor` returns `List<RecipeHolder<T>>`. Recipes no longer have `getId()`, so ids come from `RecipeHolder#id()`.
- `getResultItem(null)` -> `WithFuelRecipe#getResult()` (core's new accessor, `.copy()`), so the result keeps its components.

### JEI 19 (`compat/jei`)
- `FossilJEIPlugin`: the recipe types are now `RecipeType<RecipeHolder<X>>`, created with `RecipeType.createRecipeHolderType(FossilMod.location(..))`.
  The uids are unchanged (`fossil:analyzer|culture_vat|sifter|worktable`). I did not use `createFromVanilla`, because it looks up the
  registry during class init. JEI's default `getRegistryName` / `getCodec` use the holder id for free (bookmarks, recipe ids).
- `MultiOutputCategory`, `WithFuelCategory` and the 4 concrete categories implement `IRecipeCategory<RecipeHolder<T>>`. `setRecipe` and `draw`
  unwrap `holder.value()`. The probability cache is still keyed by the recipe object.
- `MultiOutputCategory`: `Ingredient.of(Stream<ItemStack>)` -> `addItemStacks(list)`, filtering empty stacks just like `Ingredient.of` did.
  `MultiBufferSource.immediate(Tesselator.getBuilder())` -> `guiGraphics.bufferSource()`.
- `getBackground()` is kept. It is deprecated (forRemoval) since 19.19, but it still exists in 19.70, the last 1.21.1 JEI, and keeping it
  also supports JEI 19.0–19.18. It could be replaced by `getWidth/getHeight` plus drawing the background in `draw()` later.

### REI 16 (`compat/rei`)
- The REI 16 `registerRecipeFiller` passes `RecipeHolder<T>` to the filler (`DisplayRegistry`, verified). The display ctors now take
  `RecipeHolder<…>`: `WorktableDisplay(RecipeHolder<WorktableRecipe>)`, `CultureVatDisplay(RecipeHolder<CultureVatRecipe>)`,
  `AnalyzerDisplay/SifterDisplay(RecipeHolder<? extends MultiOutputAndSlotsRecipe>)`, and the abstract `MultiOutputDisplay` / `WithFuelDisplay`.
  The `registerRecipeFiller(..., XDisplay::new)` call sites are unchanged.
- Serializers: `new ResourceLocation(str)` -> `ResourceLocation.parse(str)`.
- `MultiOutputDisplay.Serializer`: fixed a **pre-existing 1.20.1 bug**. The output ingredients were written into the `"Weights"` double list, which throws
  `UnsupportedOperationException`, and `"Items"` stayed empty. `read` also summed weights that were already cumulative and dropped the
  "nothing" entries. Now save writes `Items`, a null location (from tag displays) is skipped, and read restores the cumulative keys 1:1.
  REI uses these serializers for display favourites and server display sync.
- `MultiOutputCategory.ScrollableSlotsWidget`: `mouseScrolled(x, y, delta)` -> `mouseScrolled(x, y, amountX, amountY)` (since 1.20.2).
  The custom z=10 scrollbar (raw Tesselator/BufferBuilder, removed API) is now 3 `GuiGraphics#fill` quads in a pose translated to z=10.
  The colours are the same as before, and it is the same approach as cloth-config 15 `ScrollingContainer#renderScrollBar`.
  Probability text uses `guiGraphics.bufferSource()`.
- Verified unchanged: `CategoryRegistry.add/addWorkstations(EntryStack…)`, `ScreenRegistry.registerContainerClickArea`, `ExclusionZones.register`,
  `DisplaySerializerRegistry.register`, `DisplaySerializer` (save/read CompoundTag), `EntryIngredient.read/saveIngredient`,
  `EntryStack.read/saveStack`, `Widgets.*`, `Slot.*`, `ConfigObject.isUsingDarkTheme`, cloth `ScrollingContainer` methods, and `PointHelper.ofMouse`.

### EMI 1.1.24 (`compat/emi`)
- `FossilEmiPlugin`: loops over `RecipeHolder<X>`. `WithFuelEmiRecipe`, `MultiOutputEmiRecipe` and the 4 concrete recipes take `RecipeHolder<X>`.
  `getId()` returns `holder.id()`, so EMI's default `getBackingRecipe()` (`ProxyRecipeManager.getRecipeEntry(getId())`) resolves again.
- `MultiOutputEmiRecipe` probability text: `guiGraphics.bufferSource()`.
- The `@ExpectPlatform` signatures `values(TagKey<Item>)` / `toIngredient(List<WeightedItem>)` and the `WeightedItem(probability, stack, tagKey)` record
  are unchanged, so build's `MultiOutputEmiRecipeImpl` still matches. Also verified: `EmiTagKey.of/stream`, `EmiTags.getRawValues(EmiTagKey)`,
  `ListEmiIngredient`, `ItemEmiStack(ItemStack,long)`, `TagEmiIngredient(TagKey,long)`, and `@EmiEntrypoint` (all exist in the EMI 1.21 branch).
- The tag-based analyzer showcase recipes still have a `null` id, as in 1.20.1.

### GeckoLib 4.9.3 (`compat/geckolib`)
- `AnimationControllerOverride`: `com.eliotlash.mclib.math.{Constant,IValue}` -> `software.bernie.geckolib.loading.math.{MathValue, value.Constant}`
  (`Keyframe<MathValue>`). `core.*` packages are flattened. `CoreGeoBone` -> `GeoBone`. `fixTransitions(Map<String, GeoBone>, …)`.
- `AnimationProcessorOverride`: `CoreGeoModel/CoreGeoBone` -> `GeoModel/GeoBone`. `((AnimatableManagerAccessor) manager).isFirstTick()` ->
  `manager.isFirstTick()` (public in 4.9.3; core renamed the accessor). The compat package no longer uses `AnimatableManagerAccessor`.
- Compared with the 4.9.3 `AnimationProcessor#tickAnimation` / `AnimationController#process`: the per-controller loop, `BoneAnimationQueue`
  (now a record with `bone()`), the `BoneSnapshot` methods, `EasingType.lerpWithOverride` (interface static), `forceAnimationReset`, `getBoneAnimationQueues`,
  `AnimationState#withController`, `AnimationProcessor.QueuedAnimation`, `Animation#boneAnimations` and `BoneAnimation#boneName` all exist with
  the same signatures. The parameters match core's mixin calls (`AnimationProcessorMixin`, `AnimationControllerMixin`) exactly.

### Carry On 2.2.6 (`compat/carryon`)
- `CarryOnCompat` is unchanged. `overridePickUp(LocalRef<UUID>, Entity, ServerPlayer)` is still what build's `PickupHandlerMixin` calls. The CarryOn 1.21.1
  `PickupHandler#tryPickupEntity` has the `UUID owner` local before the first `getGameProfile()`. `MinecraftServer#getProfilePermissions(GameProfile)` /
  `getOperatorUserPermissionLevel()` exist in 1.21.1 (REI 16 and JEI 1.21.1 use them).

### Farmer's Delight (`compat/farmers`)
- `CommonFarmersDelightCompat.removeConflictingRecipes`: `getRecipes()` is now an immutable `Collection<RecipeHolder<?>>`, so it is copied to an `ArrayList`.
  `recipe.getId()` -> `recipe.id()`, then `replaceRecipes(list)`. Both signatures were checked against JEI 1.21.1's own usage.
  **This class is not called anywhere** (it was already unused in 1.20.1, see commit "Remove farmers delight recipes for now"). The egg-recipe
  conflict is handled in data by `neoforge:conditions` (`fa_cookable_eggs_to_cooked_egg*` = not farmersdelight, `fa_cookable_eggs_to_fried_egg*` =
  farmersdelight). It is kept, ported, so nothing is lost.
- No FD Java API is used in common. FD `ModItems`/`ModBlocks`/`PieBlock` are only used in build's `neoforge/.../FarmersDelightCompat` (verified by build).
  FD add-on compat and Alex's Mobs stay disabled in the neoforge module (build's decision). Nothing in common needed to change for that.

## PORT-TODO list
- `compat/rei/MultiOutputCategory.java:226`: check in-game that the REI scrollbar of the analyzer/sifter output list still renders above the slots
  (z=10 via the pose instead of raw vertices).

No other PORT-TODOs. Every other API used was checked against the cloned sources listed above.

## Assumptions
- `GuiGraphics#bufferSource()`, `Font#drawInBatch(String, …, DisplayMode, int, int)`, `FastColor.ARGB32.color` and `ClientTooltipComponent.create`
  exist in 1.21.1. REI 16, cloth-config 15 and EMI 1.21 call them.
- `RecipeHolder` instances from `getAllRecipesFor` are stable while JEI/REI/EMI are loaded. The recipe-keyed caches in the JEI categories behave
  as in 1.20.1.
- REI's common compile classpath gets cloth-config through `RoughlyEnoughItems-api` (`modApi cloth-config` in REI's `api/build.gradle`).
  `rei/MultiOutputCategory` imports `me.shedaniel.clothconfig2.*` and `me.shedaniel.math.impl.PointHelper`.

## Requests to other owners
**build**
- Nothing is required. For information: the `@ExpectPlatform` signatures in `MultiOutputEmiRecipe`, `CarryOnCompat.overridePickUp`, and the class names
  `FossilREIClientPlugin` / `FossilREICommonPlugin` / `FossilEmiPlugin` / `FossilJEIPlugin` are unchanged. `me.shedaniel.rei.forge.REIPluginClient/REIPluginCommon`
  exist in REI 16's neoforge module. JEI needs no neoforge entrypoint (`@JeiPlugin` is found by annotation scan of the merged jar).
- If Loom does not put cloth-config on `:common`'s compile classpath through the REI api, add
  `modCompileOnly "me.shedaniel.cloth:cloth-config:15.0.x"` to `common/build.gradle`.

**core**
- `mixin/AnimatableManagerAccessor` is no longer used by compat. Keep it or remove it as you like.
- Used from core and must keep its name/signature: `WithFuelRecipe#getResult/getInput/getFuel/getDuration/getFuelDuration`,
  `MultiOutputAndSlotsRecipe#getInput/getWeightedOutputs`, `ModRecipes.*_TYPE`, `ModTabs.FA_MOB_ITEM_TAB/FA_BLOCK_TAB`, `ModItemTags.{ALL_BONES, BONES_DNA,
  UNCOOKED_MEAT, MEAT_DNA, DINO_EGGS, DINO_DNA, FISH_EGGS, FISH_DNA, EMBRYOS, EMBRYO_DNA}`, `CultureVatMenu.CULTIVATION_DURATION`, and the AW entry
  `CreativeModeInventoryScreen.selectedTab`.
- `AnimationProcessorMixin` / `AnimationControllerMixin` call `tickAdditiveAnimations(T, GeoModel<T>, double, AnimationState<T>, boolean,
  Map<String, GeoBone>, boolean, AnimatableManager<T>, Map<String, BoneSnapshot>)` and `fixTransitions(Map<String, GeoBone>, QueuedAnimation,
  Map<String, BoneSnapshot>, double, boolean, Map<String, BoneSnapshot>, double, Map<String, BoneAnimationQueue>)`. These now match exactly.

**client**
- Used from client and must keep its name: `AnalyzerScreen`, `SifterScreen`, `CultureVatScreen`, `WorktableScreen` (JEI/REI click areas) and their
  `FUEL_WIDTH/FUEL_HEIGHT/PROGRESS_WIDTH/PROGRESS_HEIGHT` constants.

**data**
- The lang keys `emi.category.fossil.{analyzer,culture_vat,sifter,worktable}` are also used as JEI/REI category titles. Keep them. GUI textures
  `textures/gui/{multi_output,culture_vat,culture_vat_dark,workbench,workbench_dark}.png` are referenced.
