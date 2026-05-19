# EzFlyTime — Timed Flight, Voucher Shop & Particles for Spigot/Paper

EzFlyTime is a production-ready timed-flight plugin for Minecraft servers.
Players earn or purchase vouchers to gain temporary flight. A live boss bar counts
down remaining time, particle trails add visual flair, and a GUI shop lets players
spend server currency — all configurable with YAML or MySQL storage.

![Voucher Shop GUI](https://i.ibb.co/MxWXXnDr/image.png)

## Key Features

- Timed flight with a real-time boss bar countdown
- Physical voucher items with PDC-backed identity and duplicate detection
- In-game GUI shop for buying vouchers with Vault economy integration
- Configurable particle trails per player (with an unlockable shop)
- Auto-flight rewards: flat, permission-based, or tied to mcMMO skill levels
- Fuel display mode — show remaining time as a percentage instead of a countdown
- YAML or MySQL storage — easy single-server setup or cross-server database
- PlaceholderAPI support for scoreboards, chat formats, and GUIs
- Multilingual messages: English, Dutch, Spanish, French, Russian, Turkish, Chinese

![Advanced Particles](https://i.ibb.co/DPbhQcPv/ezflytime-advanced-particles-1.png)
![Boss bar with flight speed meter](https://i.ibb.co/JWTh3b4f/image.png)

---

## Quick Install

1. Drop `EzFlyTime-<version>.jar` into your `plugins/` directory.
2. Start the server to generate `plugins/EzFlyTime/config.yml` and related files.
3. Adjust `vouchers:`, `bossbar:`, and `storage:` in `config.yml`.
4. (Optional) Install Vault + an economy plugin to enable the voucher buy flow.
5. Run `/ezflytime reload` after any config edits.

---

## Commands

**Player commands**

- `/fly` — Toggle flight on/off.
- `/flytime` — Show remaining fly time (or open the voucher GUI).
- `/flyparticles` — Open the particle select/shop GUI or toggle auto-equip.
- `/flyvoucher buy <voucherId> [amount]` — Purchase vouchers with server currency.
- `/flyvoucher gui` — Open the voucher shop GUI.

**Admin commands**

- `/flytime give <player> <time>` — Add flight time (e.g. `10m`, `1h`).
- `/flytime set <player> <time>` — Set a player's flight time exactly.
- `/flytime remove <player> <time>` — Remove flight time from a player.
- `/flytime top` — Show the fly-time leaderboard.
- `/flyvoucher give <player> <voucherId> [amount]` — Give vouchers to a player.
- `/ezflytime reload` — Reload all configuration files.
- `/ezflytime maxsingle <player> [on|off|toggle]` — Bypass the per-session cap for a player.

---

## Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `ezflytime.fly` | `true` | Toggle flight |
| `ezflytime.flytime` | `true` | View remaining time |
| `ezflytime.top` | `true` | View leaderboard |
| `ezflytime.bypass` | `false` | Keep flight when time runs out |
| `ezflytime.give` | `op` | Give vouchers to players |
| `ezflytime.reload` | `op` | Reload configuration |
| `ezflytime.admin` | `op` | Full admin access |

---

## Configuration highlights

Boss bar customization:

```yaml
bossbar:
  enabled: true
  title: '&aFlight time remaining: {time}'
  color: GREEN
  style: SEGMENTED_10
```

Example voucher:

```yaml
vouchers:
  basic:
    material: PAPER
    name: '&aBasic Fly Voucher'
    lore:
      - '&7Redeem for 5 minutes of flight.'
    duration-seconds: 300
    price: 1000
```

Display mode (time or fuel percentage):

```yaml
display:
  flytime-mode: time  # 'time' or 'fuel'
```

---

## PlaceholderAPI Placeholders

| Placeholder | Description |
| :--- | :--- |
| `%ezflytime_time_remaining%` | Formatted remaining time (e.g. `1h 2m 3s`) |
| `%ezflytime_seconds%` | Raw remaining seconds |
| `%ezflytime_minutes%` | Whole minutes remaining |
| `%ezflytime_fuel%` | Remaining flight as a percentage (0–100) |

---

## Integrations

- **Vault** — economy abstraction for voucher purchasing
- **EzEconomy** — lightweight Vault-compatible economy: <https://modrinth.com/plugin/ezeconomy>
- **PlaceholderAPI** — register fly-time placeholders for scoreboards and GUIs
- **mcMMO** — grant flight time based on skill level thresholds

---

## Language Support

Supported languages: English, Dutch, Spanish, French, Russian, Turkish, Chinese.

Set `language: en` (or another code) in `config.yml`. To add a new language, copy
`messages_en.yml`, rename it to `messages_<code>.yml`, and translate the values.

---

## Links

- Documentation: <https://ez-plugins.github.io/EzFlyTime/>
- GitHub (source & issues): <https://github.com/ez-plugins/EzFlyTime>
- Modrinth: <https://modrinth.com/plugin/ezflytime>
