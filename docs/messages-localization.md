---
title: Messages & Localization
nav_order: 5
---

# Messages & Localization

## Bundled languages

EzFlyTime ships with these message files out of the box:

| Code | Language |
| :--- | :--- |
| `en` | English (default) |
| `nl` | Dutch |
| `es` | Spanish |
| `fr` | French |
| `ru` | Russian |
| `tr` | Turkish |
| `zh` | Chinese |

## Changing the language

Set `language` in `config.yml` to the desired language code:

```yaml
language: en
```

Restart the server or run `/ezflytime reload` after changing this value.

## Message files

Message files are stored in `plugins/EzFlyTime/messages/messages_<code>.yml`.
EzFlyTime copies defaults from the plugin jar on first start; editing the files in the
`plugins/EzFlyTime/messages/` directory overrides them permanently.

## Key message keys

| Key | When used |
| :--- | :--- |
| `flight-enabled` | Shown when a player activates flight (time mode) |
| `flight-enabled-fuel` | Shown when a player activates flight (fuel mode) |
| `flight-enabled-unlimited` | Shown when a bypass player activates flight |
| `flight-ended` | Shown when flight time runs out |
| `flight-no-time` | Shown when a player tries to fly with no time remaining |
| `flight-time-remaining` | Shown by `/flytime` (time mode) |
| `flight-fuel-remaining` | Shown by `/flytime` (fuel mode) |
| `flight-session-max-reached` | Shown when the per-session cap is hit |
| `voucher-redeemed` | Shown when a voucher is consumed |
| `voucher-duplicate` | Shown when a duplicate voucher redemption is blocked |
| `auto-flight-reward` | Shown when the auto-reward system grants time |
| `update-available` | Shown to ops when a new version is detected |

## Supported placeholders in messages

Most messages support color codes using the `&` prefix. Specific messages also
accept the following placeholders:

| Placeholder | Description |
| :--- | :--- |
| `{time}` | Formatted remaining fly time (e.g. `1h 2m 3s`) |
| `{fuel}` | Remaining fuel as a percentage (0–100) |
| `{player}` | Target player name |
| `{voucher}` | Voucher display name |
| `{amount}` | Voucher quantity |
| `{minutes}` | Minutes granted by a voucher |
| `{seconds}` | Seconds granted by auto-reward |
| `{price}` | Voucher price |
| `{currency}` | Economy currency name |
| `{rank}` | Leaderboard rank number |
| `{name}` | Leaderboard player name |
| `{current}` | Current plugin version |
| `{latest}` | Latest available plugin version |
| `{url}` | Download URL for the update |

## Adding a new language

1. Copy `messages_en.yml` from `plugins/EzFlyTime/messages/`.
2. Rename it to `messages_<code>.yml` (e.g. `messages_de.yml` for German).
3. Translate all values.
4. Set `language: de` in `config.yml`.
5. Run `/ezflytime reload`.

To contribute a translation back to the project, open a pull request adding your
file under `src/main/resources/messages/`.
