---
title: config.yml
parent: Configuration
nav_order: 1
---

Core plugin settings. Located at `plugins/EzFlyTime/config.yml`.

---

## Language

```yaml
language: en
```

Controls which message file is loaded. Supported values: `en`, `nl`, `es`, `fr`,
`ru`, `tr`, `zh`. See [Messages & Localization](../messages-localization) for details.

---

## Debug mode

```yaml
debug: false
```

Enables verbose console logging. Disable in production.

---

## Update checker

```yaml
updates:
  enabled: true
  resource-id: 129745
  notify-on-join: true
```

- `enabled` - whether to check SpigotMC for updates at startup.
- `notify-on-join` - notify players with `ezflytime.update` when they join and an update is available.
- `resource-id` - do not change this value.

---

## Display settings

```yaml
display:
  flytime-mode: time  # time | fuel
```

- `time` (default) - shows remaining flight as a formatted duration (e.g. `1h 2m 3s`).
- `fuel` - shows remaining flight as a percentage (e.g. `Flight fuel: 75%`).

This setting affects boss bars, messages, and PlaceholderAPI placeholders.

---

## Storage

```yaml
storage:
  type: yaml  # yaml | mysql
  mysql:
    host: localhost
    port: 3306
    database: ezflytime
    username: root
    password: password
    use-ssl: false
    table-prefix: ezflytime_
```

Use `yaml` for single-server setups. Use `mysql` for networks or high player counts.
Changing `type` requires a full server restart; a reload is not sufficient.

---

## Auto-save

```yaml
auto-save:
  interval-seconds: 300
```

How often data is flushed to disk (YAML) or the database. Set to `0` to disable
(not recommended - data since the last save will be lost on a hard crash).

---

## Auto-flight rewards

```yaml
auto-flight-rewards:
  enabled: false
  interval-seconds: 600
  base-seconds: 0
  notify-players: true
  ranks:
    example:
      permission: "group.vip"
      seconds: 60
  permission-nodes:
    ezflytime.autoreward.minute: 60
  mcmmo:
    enabled: true
    skills:
      MINING:
        thresholds:
          100: 30
          500: 90
```

When enabled, EzFlyTime runs a periodic task that checks all online players and
grants flight time based on their permissions and (optionally) mcMMO skill levels.

- `base-seconds` - granted to every online player each cycle regardless of permissions.
- `ranks` - named groups: if the player has `permission`, they receive `seconds`.
- `permission-nodes` - map of permission → seconds; any player with the node receives that amount.
- `mcmmo` - see [mcmmo.yml](mcmmo) for the dedicated configuration file; the section here is a
  legacy fallback when `mcmmo.yml` is absent.

---

## Voucher duplicate detection

```yaml
detect-voucher-dupes: true
```

Prevents players from redeeming the same physical voucher item twice. EzFlyTime
embeds a unique identifier in each voucher using the Persistent Data Container.
Disable only if you encounter false positives.

---

## Flight settings

```yaml
flight:
  activation-mode: NORMAL         # NORMAL | DOUBLE_JUMP_ELYTRA
  bypass-grants-unlimited: true
  allow-creative-bypass: true
  allow-spectator-bypass: true
  max-single-flight-seconds: 0
  preserve-on-death: true
```

| Key | Description |
| :--- | :--- |
| `activation-mode` | `NORMAL` - standard Minecraft flight. `DOUBLE_JUMP_ELYTRA` - double-jump activates Elytra glide. |
| `bypass-grants-unlimited` | When `true`, players with `ezflytime.bypass` fly indefinitely without consuming time. |
| `allow-creative-bypass` | When `true`, Creative-mode players are treated as having unlimited flight. Requires `ezflytime.bypass.creative`. |
| `allow-spectator-bypass` | When `true`, Spectator-mode players are treated as having unlimited flight. Requires `ezflytime.bypass.spectator`. |
| `max-single-flight-seconds` | Maximum seconds for a single uninterrupted flight session. `0` disables the cap. Override per-player with `ezflytime.maxsingle.bypass`. |
| `preserve-on-death` | When `true`, flight is restored after a player dies and respawns. |

---

## Boss bar

```yaml
bossbar:
  enabled: true
  title: '&aFlight time remaining: {time} | Speed: {speed} b/s'
  color: GREEN
  style: SOLID
  speed-counter:
    enabled: false
    speed-format: '&bSpeed: {speed} b/s'
```

| Key | Description |
| :--- | :--- |
| `enabled` | Show a boss bar during flight. |
| `title` | Boss bar text. Supports `&` color codes, `{time}`, `{fuel}`, and `{speed}`. |
| `color` | `GREEN`, `BLUE`, `RED`, `PINK`, `PURPLE`, `WHITE`, `YELLOW`. |
| `style` | `SOLID`, `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12`, `SEGMENTED_20`. |
| `speed-counter.enabled` | Show the player's current horizontal speed in the title. |
| `speed-counter.speed-format` | Format string for the speed value; use `{speed}` as the placeholder. |

---

## Vouchers

```yaml
voucher-shop:
  enabled: true

vouchers:
  basic:
    material: PAPER
    name: '&aBasic Fly Voucher'
    lore:
      - '&7Redeem for 5 minutes of flight.'
    duration-seconds: 300
    price: 1000
  premium:
    material: FEATHER
    name: '&bPremium Fly Voucher'
    lore:
      - '&7Redeem for 15 minutes of flight.'
    duration-seconds: 900
    price: 2500
```

- `voucher-shop.enabled` - controls whether the in-game GUI shop is accessible.
- Each voucher key (e.g. `basic`) is its ID used in `/flyvoucher give` and `/flyvoucher buy`.
- `price: 0` disables purchasing for that voucher (give-only).
- Add more vouchers by copying the structure above.

See [voucher-gui.yml](voucher-gui) to configure how vouchers appear in the shop GUI.
