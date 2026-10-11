# KoolChatFilter

KoolChatFilter checks player-submitted text against administrator-configured
regular expressions. Matching text is blocked where possible and the selected
filter punishment is applied. This plugin is intended for Paper servers.

## What is checked

The filter checks:

- Player chat
- Commands typed by players
- Sign text when a sign is edited
- Book pages when a book is edited
- Item names prepared by an anvil

The filter checks submitted text, not existing server messages or text generated
by other plugins. A chat mute blocks player chat; it does not automatically
block commands, signs, books, or anvil use.

## Install and configure

1. Build the plugin (see [Building](#building)).
2. Put the generated `KoolChatFilter` JAR in the server's `plugins` folder and
   start the server once to create its configuration (if somehow it fails, running `/koolchatfilter reload` fixes it.
3. Edit `plugins/KoolChatFilter/config.yml`, then restart the server or use
   `/koolchatfilter reload` to reload the plugin configuration and filter rules.

### Filter rules

Add regular expressions under `filter.rules`. Each non-empty list item is
compiled as a case-insensitive Unicode regular expression. A rule matches
anywhere in the submitted text; it does not need to match the whole line.

```yaml
filter:
  rules:
    - "badword"
    - "bad phrase"
    - "word[0-9]+"
```

Before checking, text is normalized to remove accents and zero-width formatting
characters. For example, a rule for `cafe` can match `café`. Regex syntax is
active, so characters such as `.`, `*`, `?`, `[` and `(` have their usual
regular-expression meaning. Escape them when you want to match them literally.
Invalid expressions are logged and skipped rather than preventing the other
rules from loading.

## Filter punishments

`filter.punishment` selects the action for a filter match:

| Value           | Behavior                                                                                                             |
|-----------------|----------------------------------------------------------------------------------------------------------------------|
| `PERMANENT_BAN` | Permanently blocks login using KoolChatFilter's local SQLite database, unless an external ban command is configured. |
| `TEMPORARY_BAN` | Blocks login until the configured duration expires, unless an external temporary-ban command is configured.          |
| `MUTE`          | Prevents local chat until the duration expires, unless an external mute command is configured.                       |
| `KICK`          | Kicks the player without storing a ban or mute.                                                                      |
| `WARNING`       | Sends a warning without storing a punishment.                                                                        |
| `LOG_ONLY`      | Records and reports the match without a player-facing punishment.                                                    |

`filter.reason` is the reason saved with a local ban/mute or passed to an
external punishment command.

`filter.punishment-duration` is used for temporary bans and mutes. It must be a
positive integer followed by one unit: `s` (seconds), `m` (minutes), `h`
(hours), or `d` (days). Examples: `30s`, `15m`, `12h`, `7d`. If it is invalid,
the plugin logs a warning and uses `7d`.

### Local punishment storage

Local ban and mute records are stored in `plugins/KoolChatFilter/punishments.db`
(SQLite). Ban and mute lookups use an in-memory index, and expired temporary
records are removed when the index is loaded/refreshed. Existing permanent bans
in `bans.yml` are imported into SQLite once; the YAML file is left untouched as
a backup. Back up `punishments.db` while the server is stopped to preserve all
local punishment records.

Local permanent and temporary bans are enforced during login. Local mutes are
enforced for chat. These local checks only apply when the matching external
command setting is empty.

### External punishment plugins

An optional command can delegate each applicable punishment to a separate
plugin (for example LiteBans or AdvancedBan). KoolChatFilter does not depend on
or inspect a particular punishment plugin; configure commands using the syntax
supported by the plugin installed on your server. Commands run as the console.
The available placeholders are:

| Placeholder  | Replaced with                                                     |
|--------------|-------------------------------------------------------------------|
| `{player}`   | Player name                                                       |
| `{uuid}`     | Player UUID                                                       |
| `{ip}`       | Player IP address, if available                                   |
| `{reason}`   | Configured filter reason                                          |
| `{duration}` | `permanent` for permanent bans, otherwise a duration such as `7d` |

Example only; confirm your plugin's actual command syntax before using it:

```yaml
filter:
  external-commands:
    ban: "ban {player} {reason}"
    tempban: "tempban {player} {duration} {reason}"
    mute: "mute {player} {duration} {reason}"
    unban: "unban {player}"
```

Leave an entry empty to use local storage and enforcement for that punishment.
Configure `unban` if you want `/permbans remove ...` to also invoke the external
plugin. External-plugin records are not included in the local `/permbans` count.

## Commands

| Command                                      | Purpose                                                                    |
|----------------------------------------------|----------------------------------------------------------------------------|
| `/koolchatfilter`                            | Displays information about the plugin.                                     |
| `/koolchatfilter reload`                     | Reloads configuration and filter rules.                                    |
| `/permbans`                                  | Shows the number of local permanent/temporary bans.                        |
| `/permbans reload`                           | Refreshes local punishment data from SQLite.                               |
| `/permbans remove name <name>`               | Removes a local punishment by name.                                        |
| `/permbans remove uuid <uuid>`               | Removes a local punishment by UUID.                                        |
| `/permbans remove ip <ip>`                   | Removes a local punishment by IP.                                          |
| `/obliterate <player> [reason]` (`/execute`) | Permanently bans the player, this ban is always going to be stored locally |
| `/crash [player]`                            | Runs the client crash effect.                                              |

Each command has a `kfc.<command>` permission. Administrative
subcommands such as `/permbans remove` additionally require `kfc.admin`.
Default permission groups and their command permissions are declared in
`paper-plugin.yml`.

## Building

Use the included Gradle wrapper:

### Windows
Use the `gradlew.bat` file. For example: `gradlew.bat clean build`

### macOS/Linux
Use the `gradlew` file. For example: `./gradlew clean build`

The distributable shaded JAR is written to `build/libs/`. It includes the
SQLite JDBC driver required by local punishment storage.
