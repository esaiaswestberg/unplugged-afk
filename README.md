# Unplugged AFK: Paper Edition

Stay AFK without leaving your computer on. `/unplug` disconnects you and leaves
a bot standing exactly where you were — same skin, same gamemode, in the tab
list — so your farms keep running while your machine is off. Log back in and
you take its place.

A Paper plugin port of [sakura-ryoko's Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk)
Fabric mod.

## Requirements

* **Paper 26.3** (or a fork of it). Not Spigot or Bukkit — this uses Paper-only
  internals.
* **Java 25**, which Paper 26.3 requires anyway.

Drop the jar in `plugins/` and restart. There are no other dependencies.

> This plugin removes players from the player list and rewrites their `.dat`
> files — that is how it works, not a side effect. Try it on a test server or
> take a backup first.

## How it works

`/unplug` kicks you, then places a real server-side player in your position on
the next tick. It is a genuine `ServerPlayer`, so it keeps chunks loaded, ticks
farms, shows up in the tab list, and can be hurt or killed.

Sessions survive a restart: bots are written to the config on shutdown and
respawned on boot, staggered a few seconds apart, with the downtime subtracted
from their remaining time.

## Commands

### Players

| Command | Description |
|:---|:---|
| `/unplug [<minutes>] [<reason>]` | Disconnect and leave a bot behind. |
| `/afk [<minutes>] [<reason>]` | Identical alias, off by default. Skipped automatically if another plugin owns `/afk`. |

### Administration

`/unplugged-admin` — operator by default.

| Subcommand | Description |
|:---|:---|
| `save` / `reload` | Write or re-read the config. |
| `list [players\|unplugged\|all]` | Show tracked players and live bots. |
| `info [<player>]` | Detailed state for one player. |
| `purge` | Drop all tracked players and resync. |
| `spawn <player> [<minutes>] [<reason>]` | Spawn a bot for an offline player. |
| `kick <target>` | Remove a bot and end its session. |
| `set <option> <value>` | Change any config option, then save and reload. |

The subcommands `info`, `list players|unplugged|all` and `set` require
`advancedAdminOptions` to be enabled in the config.

## Permissions

| Node | Default | Grants |
|:---|:---|:---|
| `unplugged_afk.unplug` | everyone | `/unplug` |
| `unplugged_afk.afk` | everyone | `/afk` |
| `unplugged_afk.unplugged-admin` | operator | `/unplugged-admin` |
| `unplugged_afk.unplugged-admin.<subcommand>` | operator | One subcommand. |

Where no node is set, the plugin falls back to the operator level configured in
`commands`, matching how the Fabric mod behaved.

### Limiting session length

| Node | Grants |
|:---|:---|
| `unplugged_afk.maxtime.<minutes>` | A ceiling, in minutes. |
| `unplugged_afk.maxtime.unlimited` | No ceiling. |

The highest node a player holds wins, so someone in several groups gets the most
generous one. With no node granted, the ceiling is `defaultUnpluggedTimeout`.

The ceiling applies to the default as well as to an explicit argument, so a
player with `maxtime.60` gets 60 minutes from a bare `/unplug`. Asking for more
clamps and says so.

```yaml
# LuckPerms: let regulars idle for a day, donors indefinitely
/lp group default permission set unplugged_afk.maxtime.1440 true
/lp group donor permission set unplugged_afk.maxtime.unlimited true
```

## Configuration

See [CONFIG.md](CONFIG.md). The file lives at
`plugins/unplugged-afk-paper-edition/unplugged-afk-paper-edition.json`, and a
config from the Fabric mod or an earlier build is adopted automatically.

## For developers

`UnpluggedAfkAPI` exposes session state, and three Bukkit events fire as
sessions begin, respawn and end.

```java
if (UnpluggedAfkAPI.isUnplugged(player.getUniqueId()))
{
    // that "player" is a bot
}

@EventHandler
public void onUnplugged(UnpluggedStartEvent event)
{
    getLogger().info(event.getPlayerId() + " went AFK");
}
```

## Differences from the Fabric mod

Behaviour is otherwise the same. Known differences:

* **Bots appear a few ticks later.** Paper defers disconnects by a tick, so the
  bot is placed after the player is fully gone rather than instantly.
* **Slime-block flying machines are approximate.** The push is applied from the
  bot's own tick instead of from inside the piston's movement code, so fast
  machines may behave differently.
* **Escaping the End is a plain teleport.** The original substituted the player
  during respawn, which Paper does not allow.
* **`resetHealthUponDeath` actually keeps the session alive.** In the Fabric mod
  it healed the bot and ended the session anyway.
* **Single version.** This targets Paper 26.3 only, where the mod supported
  1.19.2 through 26.3.

Other plugins see bots as ordinary players joining and quitting, from
`127.0.0.1`. Anticheats in particular may object to a player that moves without
a client; if you hit that, exempt the bots or turn on `unpluggedHidePlayer`.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.

The build needs a JDK 25 toolchain, which Gradle will use for the plugin itself
and for the server internals it compiles against.

## Releasing

Pushing a `v*` tag builds the plugin, creates a GitHub release with the jar
attached, and publishes the same jar to Modrinth.

The Modrinth step is skipped unless both of these repository secrets are set
under *Settings → Secrets and variables → Actions*:

| Secret | Value |
|:---|:---|
| `MODRINTH_ID` | The Modrinth project ID or slug. |
| `MODRINTH_TOKEN` | A Modrinth PAT with the *Create versions* and *Write projects* scopes. |

The Modrinth version number and the game version it is listed under are read
from `gradle.properties`, so bump `plugin_version` and `minecraft_version`
there rather than in the workflow.

## Licence

LGPL-3.0, inherited from the original mod. Copyright remains with sakura-ryoko
and contributors; see the file headers.
