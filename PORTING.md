# Port: Fossils & Archeology Revival 1.20.1 (Forge/Fabric) → 1.21.1 NeoForge

Branch: `port/1.21.1-neoforge`. Upstream: TeamFossilsArcheology/FossilsArcheologyRevival @ 1.20.1.
Code license MIT, assets ARR (allowed when redistributed as the whole project, e.g. in a modpack).

## Target
- Minecraft **1.21.1**, **NeoForge 21.1.x**, **Java 21**, Architectury API **13.x** (1.21), Architectury Loom.
- Keep the Architectury layout: `common/` + **`neoforge/`** (new, replaces `forge/`). The **`fabric/` module is dropped from the build**
  (leave the folder on disk untouched; just do not include it in `settings.gradle` / `enabled_platforms`).
- Mappings: official Mojang + Parchment (1.21.1). Mod id stays `fossil`. Package names stay as they are.
- GeckoLib 4.x for 1.21.1, TerraBlender 1.21.1, More Hitboxes 1.21.1 (no public source – see "Unknowns").

## Hard constraint of this phase
There is **no network to Maven / Mojang** in this environment, so nothing can be compiled or run yet.
Compile + run + in-game testing happens in phase 2 (another environment). Therefore:
1. Verify every API you use against the reference sources below — do not guess from memory when a reference exists.
2. Where you cannot verify, still write your best code **and** mark it: `// PORT-TODO(1.21.1): <what to verify>`.
3. Never delete functionality to "make it simpler". If something cannot be ported now, keep the old code in a block comment
   with a PORT-TODO explaining why.
4. Keep diffs minimal and mechanical; do not refactor or reformat unrelated code.

## Reference material (read-only) in `/home/claude/port/refs`
- `neoforged-github/primers/1.20/`, `1.20.5/`, `1.20.6/`, `1.21/`, `1.21.1/` — official porting primers (most important: **1.20.5** and **1.21**).
  1.20.2–1.20.4 changes are not in primers; see NeoForge sources + https://neoforged.net/news/ (WebFetch allowed) for 20.2 networking,
  20.3 capabilities and 20.4 changes.
- `NeoForge/` — NeoForge 1.21.1 source (incl. `patches/` with vanilla context, `src/main/java/net/neoforged/...`).
- `architectury-api/` — Architectury API 1.21 (13.x) source: networking (`NetworkManager`, payloads), registries, events.
- `geckolib/` — GeckoLib 1.21.1 branch source.
- `TerraBlender/` — TerraBlender 1.21.1 source.
- Original 1.20.1 code: `git show 1.20.1:<path>` or `git diff 1.20.1 -- <path>`.

## Ownership (agents edit ONLY their own paths; no git commits — the coordinator commits)
| Agent | Owns |
|---|---|
| build | `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `forge/` → new `neoforge/` (all Java + resources there, incl. datagen providers, mods.toml → neoforge.mods.toml, events, capabilities → data attachments, Jade compat in that module) |
| data | `common/src/main/resources/**` and `common/src/generated/**` EXCEPT `fossil.accesswidener` and `*.mixins.json` |
| core | all of `common/src/main/java/**` EXCEPT `entity/`, `client/`, `world/`, `compat/`; plus `fossil.accesswidener`, `fossil_common.mixins.json` |
| entity-a | `common/.../fossil/entity/prehistoric/**` |
| entity-b | `common/.../fossil/entity/**` except `prehistoric/` |
| client | `common/.../fossil/client/**` |
| world | `common/.../fossil/world/**` |
| compat | `common/.../fossil/compat/**` |

If you need a change in a file you do not own, write it as a request into your notes file (section "Requests to other owners");
do not edit it.

## Shared conventions (everyone)
- `ResourceLocation`: never `new ResourceLocation(...)`. Use `FossilMod.location(path)` for our namespace,
  `ResourceLocation.withDefaultNamespace(path)` for minecraft, `ResourceLocation.fromNamespaceAndPath(ns, path)` otherwise,
  `ResourceLocation.parse(str)` for "ns:path" strings.
- Item stack NBT is gone (1.20.5): use **Data Components**. All of our custom components live in
  `com.github.teamfossilsarcheology.fossil.item.component.ModDataComponents` (owned by **core**). Generic leftovers may use
  `DataComponents.CUSTOM_DATA` via `CustomData`, but prefer typed components.
- Networking: Architectury 13 payload style (`CustomPacketPayload` + `StreamCodec`, `NetworkManager.registerReceiver` /
  `NetworkManager.sendToPlayer` / `sendToServer`). Pattern + helper defined by **core** in `network/`.
- Enchantments are data-driven `Holder<Enchantment>` (1.21); look them up via registry access, not static fields.
- Attribute modifiers use `ResourceLocation` ids (1.21).
- Entity NBT (`addAdditionalSaveData`/`readAdditionalSaveData`) still uses `CompoundTag` — keep, but ItemStack save/load changed
  (`ItemStack.save(HolderLookup.Provider)`, `ItemStack.parseOptional(provider, tag)`).
- `SynchedEntityData`: `defineSynchedData(SynchedEntityData.Builder builder)` (1.20.5).
- Vertex building (1.21): `addVertex(...).setColor(...).setUv(...).setOverlay(...).setLight(...).setNormal(...)`; no `endVertex()`.
- Recipes (1.20.2+/1.21): `RecipeHolder<T>`, `RecipeInput` instead of `Container`, `MapCodec`/`StreamCodec` serializers.
- Java 21 language level is allowed but do not modernize code that does not need it.

## Notes files
Each agent writes `PORT_NOTES/<agent>.md` with: what changed (per area), every PORT-TODO with file:line, assumptions,
and "Requests to other owners". Core additionally writes `PORT_NOTES/API.md` documenting every new/changed shared API
(component names + types, network helper usage, renamed methods) — phase-2 agents read it first.

## Unknowns to settle in phase 2
- More Hitboxes 1.21.1 API (no public source here). Keep calls as-is with PORT-TODO where signatures may differ.
- Exact Parchment version / Loom version numbers (put best-known values; phase 2 fixes on first Gradle run).
- Datagen must be re-run in phase 2; `data` agent converts generated JSON by script as a fallback so the mod also works before that.
