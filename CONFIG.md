# Configuration

Unplugged AFK: Paper Edition keeps a single JSON file at:

```
plugins/unplugged-afk-paper-edition/unplugged-afk-paper-edition.json
```

On first start it adopts an existing config from an older location — the
plugin's previous `plugins/UnpluggedAFK/` folder, or `unplugged_afk.json` in the
server root where the Fabric mod kept it — and logs the move.

Edit it with `/unplugged-admin set <option> <value>` (which saves and reloads
for you), or by hand followed by `/unplugged-admin reload`.

## Options

### `main`

| Option | Type | Default | Description |
|:---|:---|:---|:---|
| `unpluggedAfkEnabled` | boolean | `true` | Master switch. When off, `/unplug` and `/afk` are not registered; `/unplugged-admin` still is, so you can turn it back on. |
| `debugMode` | boolean | `false` | Verbose logging of every state transition. Turn this on before reporting a bug. |
| `reducedListDebugInfo` | boolean | `true` | Trims the output of the `list` and `info` commands. |
| `advancedAdminOptions` | boolean | `false` | Unlocks `info`, `list players\|unplugged\|all` and `set`. |

### `commands`

| Option | Type | Default | Description |
|:---|:---|:---|:---|
| `unplugCommandPermissions` | int | `0` | Operator level required for `/unplug`, used only when no Bukkit permission node is set. |
| `unpluggedAdminCommandPermissions` | int | `4` | Operator level required for `/unplugged-admin`. |
| `afkCommandPermissions` | int | `0` | Operator level required for `/afk`. |
| `enableUnplugCommand` | boolean | `true` | Registers `/unplug`. |
| `enableAfkCommand` | boolean | `false` | Registers `/afk` as an alias. Skipped automatically if another plugin already owns `/afk`. |

Command registration happens once at startup, so changing these needs a restart.
The plugin logs a warning reminding you.

### `unplugged`

| Option | Type | Default | Description |
|:---|:---|:---|:---|
| `defaultUnpluggedTimeout` | int (minutes) | `129600` | Session length when `/unplug` is used with no argument. 90 days. Also the ceiling for players with no `maxtime` permission. |
| `resetHealthUponDeath` | boolean | `false` | The bot survives death: it is revived on the spot with full health and the session continues. Repeated deaths in quick succession end the session anyway, so a bot in lava cannot loop. |
| `unpluggedDisableDamage` | boolean | `false` | The bot takes no damage at all. |
| `unpluggedHidePlayer` | boolean | `false` | Hides the bot from the tab list and the locator bar. |
| `unpluggedHideFromOps` | boolean | `false` | Extends that hiding to operators. |

### `messages`

Four toggles, then the message strings.

| Option | Type | Default | Description |
|:---|:---|:---|:---|
| `broadcastMessages` | boolean | `false` | Announce sessions starting and ending to the server. |
| `hideUnpluggedJoin` | boolean | `false` | Suppress the join/leave lines caused by bots appearing and disappearing. |
| `displayDuration` | boolean | `false` | Include durations in those messages. |
| `displayReturnFeedback` | boolean | `false` | Tell a player why their previous session ended, next time they log in. |

The remaining string options are the message text itself. They use legacy
section codes (`§a`, `§r`, …). When setting them through
`/unplugged-admin set`, write `&` instead of `§` — it is translated for you.

`duration` and `timeDate` control formatting:

| Field | Values |
|:---|:---|
| `duration.option` | `PRETTY`, `REGULAR`, `ISO_EXTENDED`, `FORMATTED` |
| `timeDate.option` | `RFC1123`, `REGULAR`, `FORMATTED`, `ISO_LOCAL`, `ISO_OFFSET`, `TIME_ONLY`, `DATE_ONLY` |
| `*.customFormat` | Pattern used by the `FORMATTED` option; ignored otherwise. |

### `players`

Written by the plugin, one entry per tracked player. Hand-editing is tolerated
but not encouraged — invalid values are repaired on load.

| Field | Meaning |
|:---|:---|
| `uuid` / `name` | Who the entry belongs to. |
| `state.status` | `ACTIVE`, `INACTIVE`, `EXPIRED`, `INTERRUPTED` or `TERMINATED`. |
| `state.time` | Configured session length, in minutes. |
| `state.timeout` | Time remaining, in milliseconds. |
| `state.startTime` | When the session began, epoch milliseconds. |
| `state.reason` | Why it ended; replayed if `displayReturnFeedback` is on. |
| `pos` | Dimension and block position the bot stands at. |
| `game` | Game mode, and whether the player was flying. |

`last_start` and `last_stop` record server uptime. The gap between them is
subtracted from every stored timeout at boot, so a session does not keep
running while the server is down.

## Default file

```json
{
    "___comment": "Unplugged AFK: Paper Edition Config",
    "config_date": "Tue, 18 Aug 2026 12:00:00 GMT",
    "last_start": 1787040000000,
    "last_stop": 1787039940000,
    "main": {
        "unpluggedAfkEnabled": true,
        "debugMode": false,
        "reducedListDebugInfo": true,
        "advancedAdminOptions": false
    },
    "commands": {
        "unplugCommandPermissions": 0,
        "unpluggedAdminCommandPermissions": 4,
        "afkCommandPermissions": 0,
        "enableUnplugCommand": true,
        "enableAfkCommand": false
    },
    "unplugged": {
        "defaultUnpluggedTimeout": 129600,
        "resetHealthUponDeath": false,
        "unpluggedDisableDamage": false,
        "unpluggedHidePlayer": false,
        "unpluggedHideFromOps": false
    },
    "messages": {
        "broadcastMessages": false,
        "hideUnpluggedJoin": false,
        "displayDuration": false,
        "displayReturnFeedback": false,
        "defaultUnpluggedReason": "",
        "unpluggedPlayerPrefix": "§e",
        "unpluggedPlayerSuffix": "§r",
        "unpluggedKickMessage": "§6Your player will be AFK§r",
        "unpluggedExpiredReason": "§eTimeout expired§r",
        "unpluggedStarted": " §ehas been unplugged§r",
        "unpluggedPunctuation": "§e,§r ",
        "unpluggedReplaced": "§6Replaced by player§r",
        "unpluggedTerminated": "§cAFK session terminated§r",
        "unpluggedUnsuccessful": "§eYour AFK session was interrupted§r",
        "unpluggedUnsuccessfulPrefix": " §eafter:§a ",
        "unpluggedUnsuccessfulPunctuation": "\n §7- For:§r ",
        "unpluggedSuccessful": "§eYour Session was successful.§r",
        "unpluggedSuccessfulPrefix": "§eYour §a",
        "unpluggedSuccessfulSuffix": " §eSession was successful.§r",
        "unpluggedSuccessfulPunctuation": "\n §7- For:§r ",
        "whenUnpluggedReturned": " §ehas returned§r",
        "whenUnpluggedExpired": " §eAFK session expired§r",
        "whenUnpluggedInterrupted": " §eAFK session interrupted§r",
        "whenUnpluggedTerminated": " §eAFK session terminated§r",
        "whenUnpluggedDurationPrefix": " §6for: §a",
        "whenUnpluggedDurationSuffix": "§7 minutes)",
        "whenReturnDurationPrefix": " §7(Gone for: §a",
        "whenReturnDurationSuffix": "§7)§r",
        "duration": {
            "option": "PRETTY",
            "customFormat": ""
        },
        "timeDate": {
            "option": "RFC1123",
            "customFormat": ""
        }
    },
    "players": []
}
```
