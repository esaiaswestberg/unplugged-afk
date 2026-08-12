# Adding Paper support to Unplugged-AFK

> **Status:** design document / proposal. No implementation has started.
> Stage 0 is a go/no-go spike that gates Stages 2–5.

## Context

Unplugged-AFK is currently a Fabric-only, server-side mod: a player runs `/unplug`, gets
kicked, and a fake `ServerPlayer` bot is left standing in their place (default 90 days) so
their AFK farms keep running. Bots are restored on server restart from a JSON config.

The goal is to make the mod usable on Paper as well as Fabric. Per decisions taken during
planning:

- **Paper ships as a drop-in plugin** (`plugins/*.jar`), *not* as a mixin mod requiring a
  launcher swap. Feature loss is accepted in exchange for a normal install story.
- **Folia is dropped.** (It was in the original ask; see "Folia" below for why it was cut
  and what it would take if that changes.)
- **corelib is abstracted away from inside this mod**; corelib itself is not ported.
- **A chunk-loading spike gates all other work.**
- Separate jars per platform are acceptable; a shared codebase is preferred.

The good news the exploration turned up: this port is far more tractable than it looks.
Direct Fabric coupling is only **5 imports across 4 files**, 22 source files contain no
`net.minecraft` at all, and — decisively — **Paper has run Mojang-mapped NMS since 1.20.5**,
the same mappings this mod is written against. `UnpluggedServerPlayer`, `UnpluggedConnection`,
`UnpluggedGamePacketListener`, `PlayerManager` and the `*Wrap` helpers compile against Paper
essentially unchanged via `paperweight-userdev`.

The two things that genuinely do not port are **the 13 mixins** and **corelib**.

---

## Feasibility verdict

| Platform | Verdict |
|---|---|
| Fabric | Works today. Every stage below leaves it working or improves it. |
| Paper | Feasible as a drop-in plugin **if** the Stage 0 spike passes, with the feature losses below. |
| Folia | Dropped. |

### What is lost on Paper

Five mixins intercept vanilla mechanics that have **no Bukkit equivalent and no reflection
workaround**. Without a mixin loader these are simply gone:

| Mixin | Lost behaviour | Severity |
|---|---|---|
| `MixinPistonMovingBlockEntity` | Bots riding **slime-block flying machines** | **High** — flagship AFK-farm use case |
| `MixinPlayer` | Knockback suppression (bots shoved off machines) | Medium |
| `MixinEntity` | `isControlledByLocalInstance` — bots in boats/minecarts | Medium |
| `MixinServerPlayer_fakeMovement` | Fake client movement (1.21.11+ only) | Low |
| `MixinTickRateManager` | `/tick freeze` treating bots as fake (1.21.11+ only) | Low |

The piston loss is the one worth weighing before committing. A partial approximation exists —
listen to `BlockPistonExtendEvent`, find bots in the push path, apply velocity manually — but
it will not faithfully reproduce vanilla slime-block physics. Treat it as a stretch goal
(Stage 6), not a promise.

The other eight mixins **are** replaceable — see the mapping table in Stage 3.

### What else to expect on Paper

- Paper's roadmap explicitly warns **"avoid directly storing player (`ServerPlayer`)
  instances"**; Paper intends to stop reusing them across world changes. `UnpluggedEntry` /
  `UnpluggedEntryList` hold them for up to 90 days. This is a *scheduled* future break —
  design Stage 5 to look up by UUID and hold `ServerPlayer` only for the current tick.
- `UnpluggedGamePacketListener.disconnect(Component)` will silently stop being called: Paper
  routes through `disconnect(Component, PlayerKickEvent.Cause)`. Needs a Paper-specific override.
- `getRemoteAddress()` returning a synthetic `127.0.0.1:65535` from an `EmbeddedChannel` will
  confuse other plugins (LuckPerms, EssentialsX, anti-cheats). Budget a compat-shim tax.
- Paper coverage will be a **subset** of Fabric's 16 MC versions — start with one.

---

## Stage 0 — De-risking spike (3–5 days). Do this first, alone.

[Paper issue #9750](https://github.com/PaperMC/Paper/issues/9750) reports NMS fake players do
**not hold ticking chunks** on Paper, and was closed as "not planned". Chunk loading is this
mod's entire reason to exist. Everything else is wasted effort if this is red.

Build a throwaway ~250-line Paper plugin (not the real mod) against the newest MC version the
mod already supports. With a hopper/redstone counter at the bot's position and **zero real
players online**:

| # | Test | Why |
|---|---|---|
| T1 | `PlayerList.placeNewPlayer(new UnpluggedConnection(...), bot, cookie)` completes | Paper fires `PlayerJoinEvent`, builds a `CraftPlayer`, reads the channel address |
| T2 | Bot alive after 10 min | Keepalive / read-timeout / auto-save |
| T3 | Chunks **loaded** | baseline |
| T4 | Chunks **entity-ticking** — counter advances, cactus grows | **the actual #9750 question**; loaded ≠ ticking |
| T5 | Paper's fake-player flag on `ServerPlayer` set true, re-run T3/T4 | If T4 only passes with this, that's still a pass — record the dependency |
| T6 | Bot counts for mob spawning / mobcaps (cross-check `setAffectsSpawning`) | Most farms need this |
| T7 | Random ticks, redstone, observers in bot's chunks | |
| T8 | Reflection swap of `ServerPlayer.connection` and `Connection.channel` works | Validates the no-mixin approach |
| T9 | Survives restart through Paper's `PlayerDataStorage` | |
| T10 | LuckPerms + EssentialsX installed; iterate `getOnlinePlayers()`, call `getAddress()` | Interop tax |

**Go criteria — all must hold:** T3 **and** T4 pass with no real player for ≥10 min; T6 passes
or is reachable via `setAffectsSpawning`; T1/T2/T8/T9 pass with no per-tick hacks.

**Stop signals:** chunks only tick near a real player and T5 doesn't fix it → **do not port**;
or fixing it needs runtime patching of Paper's chunk system.

**Deliverable:** `docs/paper-spike.md` with numbers and an explicit go/no-go.

> If red, stop — but **still do Stage 1**, which fixes a live Fabric bug.

---

## Stage 1 — Threading + platform seam (Fabric-only, ~1.5–2.5 wk)

**Do this regardless of the spike outcome.** It fixes real bugs shipping today.

### Bugs found during exploration

1. **Live data race.** `UnpluggedServerPlayer.java:256` and `:272` — on MC ≥ 1.20.2,
   `fetchGameProfile(...).whenCompleteAsync(...)` / `.thenAccept(...)` call
   `createFromConfigPhase2`, which calls `placeNewPlayer`, `connection.teleport` and
   `entityData.set` **on the profile-resolution thread with no hop back to the server thread**.
   This fires on every restart with ≥1 saved bot. Route the continuation through the new
   scheduler seam.
2. `PlayerEventsHandler.java:53` — `shouldHideJoin` is a plain `ArrayList` written from mixin,
   tick and command threads → `ConcurrentHashMap.newKeySet()`.
3. `ServerEventsHandler.java:47-53` — non-volatile booleans toggled across threads → `volatile`.
4. Blocking config I/O on the tick thread (`PlayerManager.java:761,773,797,955`) → snapshot
   then write async.
5. `PlayerList.remove(this)` from inside the bot's own `tick()` (`UnpluggedServerPlayer.java:747`,
   `:845`) — defer to end-of-tick.

### New package `impl/platform/`

A single `PlatformServices.get()` holder, set once at entrypoint. This extends the convention
the codebase **already uses** (`ConfigWrap`, `PermsWrap`, `InitWrap`, `GameWrap`, `PosWrap`,
`ProfileWrap`) rather than inventing a new one.

```
Path gameDir() / configDir()          // replaces Reference.GAME_DIR/CONFIG_DIR
boolean isModLoaded(String id)        // replaces FabricLoader.getAllMods() in CommandRegister
Permissions permissions()             // PermsWrap delegates here
Scheduler scheduler()                 // onServerThread / async / registerTickHandler
CommandRegistrar commands()
ModEventBus events()
TextHandler text()
String platformName()
```

In this stage only the Fabric impl exists. Bug #1 above is fixed *through* `scheduler()`.

---

## Stage 2 — Decouple from corelib (~1.5–3 wk)

corelib is a Fabric mod (fabric-loader, plus its own mixins for commands and events) and
cannot load on Paper. 38 usages across 20 files. The mod is already interface-shaped against
it (`IConfigDispatch`, `IServerCommand`, `IServerEventsDispatch`, `IPlayerEventsDispatch`,
`ITextHandler`), which makes this mechanical rather than inventive.

Two different treatments, by subsystem:

- **Mixin-backed subsystems → mod-owned interfaces** (`ICommandRegistrar`, `IServerEvents`,
  `IPlayerEvents`, `IConfigStore`). On Fabric, thin adapters delegate to corelib exactly as
  today. On Paper, self-contained impls.
- **Pure-logic classes → vendor/copy.** `api/time/*` (`TimeFormat`, `DurationFormat`,
  `DurationOption`, `TimeDateOption`), `api/log/AnsiLogger`, `impl/text/BuiltinTextHandler`.
  No platform coupling; copying is cheaper than abstracting.

Also: `MixinTickRateManager` and `MixinServerWaypointManager` import corelib's `MixinDummy`
purely as a conditional-mixin no-op target — replace with a mod-local dummy (~10 lines).

**Do not port corelib itself.** It is a separate repo with its own multi-version matrix; doing
so doubles the port and puts this schedule on a second project's critical path. Keep the Fabric
adapters thin so corelib stays the shared library for the author's other mods.

Estimated new/moved code: **~900–1400 lines.**

### Public API break (fold in here)

`api/UnpluggedAfkEvents.java` exposes three `net.fabricmc.fabric.api.event.Event<UnpluggedEvent>`
fields. Fabric API cannot exist on Paper. Replace with a hand-rolled `UnpluggedEventBus`
(~40 lines, same `.register()` shape). `UnpluggedAfkAPI` is UUID-based and ports unchanged.
Ship a deprecated Fabric-only forwarding shim for one release; bump to **0.3.0**.

---

## Stage 3 — Paper runtime port, one MC version (~3–5 wk)

The bulk of the mod compiles against Paper as-is. The work is replacing the eight replaceable
mixins and writing the Paper `PlatformServices` impl.

### Mixin → Paper mapping

| Mixin | Paper replacement |
|---|---|
| `MixinMinecraftServer` (`tickServer` TAIL) | Bukkit repeating task, 1-tick period |
| `MixinServerPlayer_core` (`tick` TAIL, all players) | Same repeating task, iterate online players |
| `MixinPlayerList_messageSuppress` | `PlayerJoinEvent#joinMessage(null)` |
| `MixinServerPlayer_messageSuppress` | `PlayerQuitEvent#quitMessage(null)` |
| `MixinServerLoginPacketListenerImpl` | `AsyncPlayerPreLoginEvent` / `PlayerLoginEvent` — kill the bot before the real player logs in |
| `MixinConnection` (`@Accessor setChannel`) | Reflection on `Connection.channel` |
| `MixinPlayerList_core` — listener swap in `placeNewPlayer` | Call `placeNewPlayer`, then reflect `ServerPlayer.connection` to `UnpluggedGamePacketListener` |
| `MixinPlayerList_core` — `ServerPlayer` swap in `respawn` | `PlayerRespawnEvent` + explicit re-spawn of the bot |
| `MixinPlayerList_core` — position re-apply in `load` | Set position explicitly after `placeNewPlayer` |
| `MixinServerWaypointManager` | Direct calls to the waypoint manager (no `@Invoker` needed — Paper is Mojang-mapped) |

The five vanilla-mechanics mixins are dropped (see "What is lost").

### Commands

The three command classes are **plain brigadier** over `net.minecraft.commands.CommandSourceStack`
(4 / 4 / 14 usages). Two options:

- **Recommended:** register directly into the NMS dispatcher
  (`MinecraftServer.getServer().getCommands().getDispatcher()`), reusing the existing trees
  verbatim. Call `player.updateCommands()` after registration.
- Alternative: Paper's Brigadier API (`io.papermc.paper.command.brigadier`, 1.20.6+), which uses
  a *different* `CommandSourceStack` type and would require adapting every node.

`PermsWrap` already has a vanilla `hasPermission` fallback path; the Paper impl maps to
`Permissible#hasPermission`.

### Other Paper specifics

- Access widener is Fabric-only. The 1.21.8 AW widens exactly one member
  (`SkullBlockEntity.fetchGameProfile`) — use reflection on Paper.
- Override `disconnect(Component, PlayerKickEvent.Cause)`.
- Hold `ServerPlayer` only within a tick; key long-lived state by UUID.

---

## Stage 4 — Build & CI (~1–1.5 wk)

Do **not** start this before Stage 0 returns green.

### Subprojects

`settings.json` grows a platform dimension:

```json
{ "versions": [
    { "mc": "1.19.2",  "platforms": ["fabric"] },
    { "mc": "1.21.11", "platforms": ["fabric", "paper"] }
] }
```

Subproject id `<mc>-<platform>`, dir `versions/<mc>-<platform>/`. This renames all 16 existing
dirs to `-fabric` — do it in one commit; it touches `release.yml`, `summary.py`, and any saved
`target_subproject` values. Half-symmetric naming would bite forever.

### Preprocessor

The preprocessor supports arbitrary `String → Int` vars (`vars.put("PAPER", 1)`), `//#ifdef`,
and `&&`/`||`/`!` — but **no parentheses**, and **no 2-D project graph** (`createNode` +
spanning-tree BFS). Flatten it: keep the existing Fabric chain, then hang each Paper node off
its **same-MC Fabric node** with an identical `mcVersion` int, so the only delta the
preprocessor applies is the `PAPER` var.

Use `//#if PAPER` **only** for platform-only imports and mixin targets. Everything behavioural
goes through `PlatformServices`. `src/main/java` already carries 134 directives and 444 `//$$`
lines — add a CI guard that fails the build above ~200 `//#if`, or the primary source tree
will rot.

Entrypoints live in **separate, non-preprocessed source sets** (`src/fabric/java`,
`src/paper/java`) so `src/main` stays free of platform noise.

### Gradle

Split `common.gradle` → `common-base.gradle` (license, version string, toolchain, publishing,
~60% shared) + `common-fabric.gradle` (loom, `modImplementation`, `include()`, fabric-api, AW,
`fabric.mod.json`) + `common-paper.gradle` (`paperweight-userdev`, shading, `plugin.yml`).

**Jar-name collision:** `fullProjectVersion` is `v<ver>-mc<mcver>`, so `1.21.11-fabric` and
`1.21.11-paper` produce identical filenames, breaking `buildAndGather`, `build.yml`'s collection
glob, and `release.yml`'s "exactly one jar" assertion. Append `-<platform>` in `common-base.gradle`.

### CI

- `matrix.py`: emit `{subproject, mc, platform}`; add a `TARGET_PLATFORM` filter.
- `summary.py`: group rows by platform.
- `release.yml`: `loaders:` becomes per-matrix-entry (`paper` vs `fabric`).

---

## On "the same output JAR"

One jar serving both Fabric and Paper is **not practical**: different loader manifests
(`fabric.mod.json` vs `plugin.yml`), different entrypoint types, different mixin/AW handling.
Recommend two jars.

What *is* achievable, and probably what the wish is really about: **one repo, one source of
truth, ~90% shared code.** The expensive parts — `UnpluggedServerPlayer` (986 lines),
`UnpluggedConnection`, `UnpluggedGamePacketListener`, `PlayerManager`, the config layer — are
literally the same source on both platforms, because both are Mojang-mapped.

Separately: a *single Paper jar covering multiple MC versions* is achievable later
(FakePlayer-CE ships one jar for 1.20.1–26.2 via per-version NMS modules), but that is a
different axis and out of scope for the first release.

---

## Effort & sequencing

| Stage | Content | Effort | Notes |
|---|---|---|---|
| 0 | Paper spike | **3–5 d** | Gate for everything |
| 1 | Threading fixes + `PlatformServices` (Fabric) | 1.5–2.5 wk | **Do regardless of spike** |
| 2 | corelib decoupling + API 0.3.0 | 1.5–3 wk | Highest uncertainty |
| 3 | Paper port, one MC version | 3–5 wk | The real work |
| 4 | Build/CI 2-D matrix | 1–1.5 wk | Not before Stage 0 |
| 5 | Docs, install guide, release | 1–2 wk | Routinely underestimated |
| 6 | *Stretch:* piston approximation via `BlockPistonExtendEvent` | 1–2 wk | Only if users ask |

**Total ≈ 9–14 weeks.**

**Strong scope recommendation:** support Paper on the **latest 1–2 MC versions only**.
16 MC × 2 platforms = 32 subprojects is an unreasonable CI and maintenance surface. Start with one.

---

## Folia (dropped — recorded for the future)

Cut during planning. If it ever comes back, the blockers are structural, not incidental:

- `MinecraftServer#tickServer` is not the tick driver on Folia — `MixinMinecraftServer` has no
  target; work moves to `GlobalRegionScheduler`.
- The mod keeps a global `UnpluggedEntryList` of `ServerPlayer`s across arbitrary regions and
  mutates them from one place. Folia forbids exactly this.
- `PlayerList.remove(this)` from inside the bot's own `tick()` is a cross-region write.
- Per-bot timers must use each bot's `EntityScheduler` (Folia docs call the region scheduler
  "entirely inappropriate" for entities); teleports must use `teleportAsync`.

Stage 1's `Scheduler` seam is deliberately shaped so this stays *possible* later. Precedent
exists (`sjavi4/PlayerDoll` supports Folia), so it is not impossible — just a separate,
4–8 week, high-risk workstream.

---

## Verification

**Stage 0:** as tabled above; result written to `docs/paper-spike.md`.

**Stage 1 (Fabric, no behaviour change intended):**
- `./gradlew build` across all 16 subprojects.
- Manual: `/unplug`, confirm bot spawns; restart the server, confirm bots restore.
- Specifically for bug #1: on MC ≥ 1.21.10 with ≥2 saved bots, restart and confirm no
  `ConcurrentModificationException` / no spawn on a non-server thread. Add a temporary
  assertion that `placeNewPlayer` is only reached on the server thread.

**Stage 2:** config file round-trips byte-identically before/after; all commands still register
and respond; `/unplugged-admin info` unchanged.

**Stage 3 (Paper):**
- `/unplug` → bot spawns, is visible in tab list, survives 10 min with no players online.
- Farm output matches the Stage 0 measurements.
- Real player reconnects → bot is removed cleanly, no duplicate-login kick.
- Restart → bots restore from config.
- Confirm the five dropped mixins' features are absent but nothing throws.
- Run with LuckPerms + EssentialsX; no NPEs from `getAddress()` / player iteration.

**Stage 4:** `./gradlew build` produces distinctly-named jars per platform; CI matrix shows both;
a dry-run release produces correct `loaders:` per artifact.
