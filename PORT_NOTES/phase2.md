# Phase 2: build, datagen, run, test (1.21.1 NeoForge)

Environment: macOS, JDK 21 auto-provisioned by Gradle (foojay resolver), Gradle 8.14, NeoForge 21.1.266.
All versions in `gradle.properties` resolved as written; no coordinate had to change.

## Fixes (by milestone)

### Build (`./gradlew build`)
| File | Problem | Fix |
|---|---|---|
| `settings.gradle` | no local JDK 21 | `org.gradle.toolchains.foojay-resolver-convention` 0.9.0 |
| `entity/ai/anu/AnuDefensePhase.java:108` | `BlockBehaviour#canSurvive` is protected in 1.21.1 | `defaultBlockState().canSurvive(level, pos)` (ObsidianSpikesBlock ignores the state argument) |
| `item/JavelinItem.java`, `entity/Javelin.java` | MAX_DAMAGE + stack 16 is rejected by `ItemStack#validateComponents` (review W1) | **Decision: stack 1** like the trident. `JavelinProperties#lockedDurability(30)` (vanilla durability, locked against TieredItem's `durability(tier.getUses())`). The 16-stack code is kept commented, `stacksToWithDurability` kept unused |

That was the only compile error in 686 files.

### Datagen (`./gradlew :neoforge:runData`)
| File | Problem | Fix |
|---|---|---|
| `block/ModBlocks.java` (anu barrier origin/face) | vanilla 1.21.1 `Blocks.BARRIER` uses `waterloggedMapColor`, which reads `WATERLOGGED`; `ofLegacyCopy` copied it to our blocks without that property and block registration crashed (cascading "Registry Object not present" errors) | `.mapColor(MapColor.NONE)` (the 1.20.1 barrier colour). The other copied vanilla blocks were checked, none has a state-dependent lambda |
| `ModItemTagsProvider` | `C_SWORDS..C_HOES` alias vanilla tags now, so the tool items were added twice | duplicate adds commented out |
| static `data/fossil/{enchantment,jukebox_song}` | duplicates of datagen output | deleted (datagen output is byte-for-byte identical in content) |
| Create crushing recipes (7), Prehistoric Fauna tags (24) | their providers are stubs (no Create-6 datagen port / no PF for 1.21.1), so datagen no longer emits them | moved from `common/src/generated` to `common/src/main/resources` (unchanged content) |

Regenerated JSON compared with the script-converted JSON (`git diff` + semantic JSON compare): all differences are
equivalent — defaults omitted by the codecs (`show_notification`, `sends_telemetry_event`, `display.hidden/show_toast/announce_to_chat/frame`,
`duration` = serializer default), single-entry HolderSets written as a string, `requirements` order. One real difference:
the volcano biome gained `minecraft:ore_diamond_medium` (vanilla 1.21 `BiomeDefaultFeatures#addDefaultOres`). The `c:axes/hoes/...`
files and `analyzer/scute.json` (now `analyzer/turtle_scute.json`, same content) are gone as intended.

**Known tooling issue:** the data run finishes ("All providers took …") but the JVM does not exit: architectury-transformer's
runtime (`TinyRemapper`, `ReadClasspathProviderImpl`) leaves non-daemon thread pools. Kill the process after the
"Caching: total files" line. Not mod code.

### Server / client / game tests
| File | Problem | Fix |
|---|---|---|
| `forge/ForgeFossilMod.java` | NeoForge error "entities have not registered to RegisterSpawnPlacementsEvent: fossil:sentry_piglin" | registered with exactly the vanilla defaults of an unregistered type (NO_RESTRICTIONS, MOTION_BLOCKING_NO_LEAVES, always true) – no behaviour change |
| `forge/ForgeFossilMod.java` | **REI crash** (`TextureAtlas#getSprite(null)` for the `fossil:tar` fluid entry): Architectury 13.0.8 creates the tar `FluidType` but never registers it in `NeoForgeRegistries.FLUID_TYPES`, so NeoForge never calls its `initializeClient` → no still/flowing texture (also affects tar rendering in the world) | `RegisterClientExtensionsEvent` listener registers the extensions from `FluidType#initializeClient` (skipped if already present) |
| `client/ClientInit.java` | GeckoLib 4.9's molang parser rejects the unknown identifiers `rider_rot_x/z` (mclib auto-created them as 0) → quetzalcoatlus `fly`/`dive` animations failed to load | `MathParser.registerVariable(new Variable(..., 0))` before resources load |
| `assets/fossil/animations/entity/postosuchus.animation.json` | malformed `math.cos(query.anim_time*360/0.5+)*-2` → `animation.postosuchus.run` failed to load | dangling `+` removed |
| `assets/fossil/sounds.json` | `subtitle.fossil.ichthyosaurus_outside` has no lang entry (1.21 logs an error) | points to the existing `subtitle.fossil.ichthyosaurus_ambient_outside` |
| `neoforge/build.gradle` | no way to run the game tests headless | `gameTestServer` run (`./gradlew :neoforge:runGameTestServer`) |
| `data/fossil/structure/worktable.nbt` (game test template) | contained an **analyzer** (upstream 1.20.1 bug, so the worktable tests never ran) | block + block entity id changed to `fossil:worktable` (NBT string replace) |
| `forge/tests/RecipeTests.java` | – | failure message of `worktableRecipes` shows recipe id and machine state |
| `entity/ai/navigation/FlightPathNavigation.java` | the centered `getPathTypeWithinMobBB` override was dead code in 1.20.1 (1.20.1 `FlyNodeEvaluator` called it with `invokespecial`, checked in the 1.20.1 bytecode) but 1.21.1 calls it virtually → flying pathing would change | renamed (no longer overrides) to keep the 1.20.1 behaviour; rename back to enable |

### Game tests: 27 / 29 pass
- `worktablerecipes` fails: **pre-existing gameplay bug**, not a port regression. `WorktableBlockEntity#setItem` resets the progress on
  an input change but does not recompute `cookingTotalTime`; while the same fuel keeps burning, the next item inherits the total
  time of the previous recipe (here 3000 instead of 300). Same code in 1.20.1. Fix would be one line (recompute `cookingTotalTime`
  in `setItem`, like the vanilla furnace) – left as is (behaviour change, outside the port).
- `ensurealldinoshavebio` fails on the dedicated game-test server by design: it reads the client-side `DinopediaBioLoader`.
  Run it in the client with `/test runthese` to check it.

### Logs (server + client joined to it)
No mixin errors (all mixins apply; CarryOn's target is missing because CarryOn is not installed – `require = 0`), no recipe /
loot / advancement / tag parse errors (1940 recipes, 1866 advancements). Remaining noise is not ours or harmless:
GeckoLib warns about `animations/entity/data/*.json` (our own animation category data inside the folder GeckoLib scans; it skips
them), config `version` 0 → 1 correction (default 0 outside the range [1,1], same as 1.20.1), nightconfig `NoSuchFileException`
for `fossil-client.new.tmp.toml` (file-watcher race in NeoForge config saving), REI reload warnings, vanilla goat horn sounds.

## PORT-TODOs
Verified by compile / datagen / run / sources and changed to `PORT(1.21.1, verified phase 2)`: all build coordinates,
More Hitboxes/EMI/GeckoLib artifacts, the access widener → AT (`setPierceLevel`, `eyeHeight`, `getEntryAtPosition` public-f),
AbstractFish/ServerEntity/JigsawPlacement/SyncedEntityData/NodeEvaluator/WalkNodeEvaluator/Explosion mixins (against the decompiled
1.21.1 sources), ModJukeboxSongs, ModStructureType, the leash hook, CenteredNodeEvaluator (1.20.1 logic kept on purpose),
DinopediaScreen authlib, ModDimensionsImpl, EMI impl, RecipeTests, all datagen providers.

Still open (need the game, a dependency release or a decision): see `grep -rn "PORT-TODO(1.21.1)"`:
in-game checks (quagga/tar slime/stone tablet sizes, AnuDead double teleport, bird egg dispenser, NBT variants from spawn eggs,
rider height, trilobite/amphibious pathing, anu lair structures + teleport, title screen blur, machine screens, dino render scale,
anu totem beams, dinopedia preview, REI scrollbar, debug renderers), FD add-ons / Alex's Mobs / Prehistoric Fauna / Create datagen
(no verified 1.21.1 jars), 1.20.1 ForgeCaps → attachments migration, `initializeClient` deprecation (still called by NeoForge 21.1).
