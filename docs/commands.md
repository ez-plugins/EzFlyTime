---
title: Commands
nav_order: 3
---

# Commands

## Player commands

### `/fly`

Toggle flight on or off.

- Requires fly time to be available (or a bypass permission).
- Shows remaining time or fuel percentage on enable, depending on `display.flytime-mode`.

### `/flytime`

Show remaining fly time.

- No arguments: prints your current remaining time (or fuel percentage).
- Opens the voucher shop GUI when called without arguments if the player has `ezflytime.buy`.

### `/flyparticles`

Manage in-flight particle trails.

- `/flyparticles select` — Open the particle selection GUI.
- `/flyparticles shop` — Open the particle shop to purchase new trail styles.
- `/flyparticles toggle-autoequip` — Toggle whether newly purchased particles are automatically equipped.

### `/flyvoucher`

Voucher-related commands.

- `/flyvoucher buy <voucherId> [amount]` — Purchase a voucher using server currency.
- `/flyvoucher gui` — Open the voucher shop GUI.

---

## Admin commands

### `/flytime <subcommand>`

Manage player flight time.

| Subcommand | Description |
| :--- | :--- |
| `give <player> <time>` | Add flight time to a player (e.g. `30s`, `5m`, `1h`). |
| `set <player> <time>` | Set a player's flight time to an exact value. |
| `remove <player> <time>` | Remove flight time from a player. |
| `top` | Show the top fly-time leaderboard. |
| `reload` | Reload all configuration files. |

**Time format:** `30s` (seconds), `5m` (minutes), `1h` (hours), or a bare number (treated as minutes).

### `/flyvoucher give`

```
/flyvoucher give <player> <voucherId> [amount]
```

Give one or more vouchers to an online player.

- `voucherId` must match a key under `vouchers:` in `config.yml`.
- `amount` defaults to `1` when omitted.

### `/ezflytime`

Plugin administration.

| Subcommand | Description |
| :--- | :--- |
| `reload` | Reload all configuration files and recreate managers. |
| `maxsingle <player> [on\|off\|toggle]` | Grant or revoke the per-session max-flight bypass for a player. |
| `help` | Show the admin help message. |

---

## Quick admin cheatsheet

```
/flytime give Steve 10m        — give Steve 10 minutes
/flytime set Steve 1h          — set Steve's time to 1 hour
/flytime remove Steve 5m       — remove 5 minutes from Steve
/flytime top                   — show leaderboard
/ezflytime reload               — reload config
/flyvoucher give Steve basic 3 — give Steve 3 basic vouchers
/ezflytime maxsingle Steve on  — exempt Steve from per-session limit
```
