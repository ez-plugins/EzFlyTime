---
title: Permissions
nav_order: 4
---

## Declared permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `ezflytime.fly` | `true` | Use `/fly` to toggle flight |
| `ezflytime.flytime` | `true` | Use `/flytime` to view remaining time |
| `ezflytime.top` | `true` | View the fly-time leaderboard with `/flytime top` |
| `ezflytime.give` | `op` | Give vouchers to players with `/flyvoucher give` |
| `ezflytime.reload` | `op` | Reload configuration with `/flytime reload` or `/ezflytime reload` |
| `ezflytime.admin` | `op` | Access all `/ezflytime` administration subcommands |
| `ezflytime.maxsingle.manage` | `op` | Manage the per-session bypass with `/ezflytime maxsingle` |
| `ezflytime.maxsingle.bypass` | `false` | Bypass the `max-single-flight-seconds` limit |
| `ezflytime.bypass` | `false` | Keep flight active even when fly time runs out |
| `ezflytime.bypass.creative` | `false` | Unlimited flight while in Creative mode (when `allow-creative-bypass: true`) |
| `ezflytime.bypass.spectator` | `false` | Unlimited flight while in Spectator mode (when `allow-spectator-bypass: true`) |
| `ezflytime.notify` | `op` | Receive voucher duplicate-redemption alerts |
| `ezflytime.update` | `op` | Receive in-game update notifications |

## Auto-flight reward permissions

Assign these to permission groups or players to control how much time they receive
from the periodic auto-reward system:

| Permission | Reward |
| :--- | :--- |
| `ezflytime.autoreward.minute` | 60 seconds per reward cycle |

Additional reward nodes can be defined under `auto-flight-rewards.permission-nodes` in `config.yml`.

## Particle permissions

Each particle entry in `particle-shop-gui.yml` can require a permission before the player
can purchase or equip it. By convention, these follow the pattern `ezflytime.particles.<id>`.

## Usage notes

- `ezflytime.bypass` lets a player keep flight indefinitely when their timer hits zero.
  When `bypass-grants-unlimited: true` (the default) the boss bar is hidden for bypass players.
- `ezflytime.bypass.creative` and `ezflytime.bypass.spectator` only take effect when the
  corresponding `allow-creative-bypass` / `allow-spectator-bypass` settings are enabled in
  `config.yml`.
- `ezflytime.maxsingle.bypass` exempts a player from the `max-single-flight-seconds` cap.
  Operators can also toggle this per-player at runtime with `/ezflytime maxsingle <player>`.
