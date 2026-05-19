# EzFlyTime

Timed flight for Minecraft servers - voucher shop, boss bar, particles, and economy
integration for Bukkit, Spigot, and Paper.

[![Modrinth](https://img.shields.io/modrinth/dt/ezflytime?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/ezflytime)
[![License](https://img.shields.io/github/license/ez-plugins/EzFlyTime)](LICENSE)

## Download EzFlyTime on Modrinth

- Modrinth: <https://modrinth.com/plugin/ezflytime>
- GitHub (releases & docs): <https://github.com/ez-plugins/EzFlyTime>

## Documentation

Full documentation is available at **<https://ez-plugins.github.io/EzFlyTime/>**

Documentation at a glance:

- [Getting Started](docs/overview-installation.md)
- [Commands](docs/commands.md)
- [Permissions](docs/permissions.md)
- [Configuration Reference](docs/config/config.md)
- [Integrations](docs/integrations/vault-economy.md)
- [Messages & Localization](docs/messages-localization.md)
- [Operations & Troubleshooting](docs/operations-troubleshooting.md)

## Key Features

- Timed flight with a real-time boss bar countdown and speed meter.
- Physical voucher items with PDC-backed identity and duplicate detection.
- In-game GUI shop for buying vouchers with Vault economy integration.
- Configurable particle trails per player (with an unlockable shop).
- Auto-flight rewards: flat, permission-based, or tied to mcMMO skill levels.
- Fuel display mode - show remaining time as a percentage instead of a countdown.
- YAML or MySQL storage.
- PlaceholderAPI placeholders for scoreboards, chat formats, and GUIs.
- Optional [TeamsAPI](https://modrinth.com/plugin/teams-api) integration: `/f fly` team subcommand and claimed-chunks flight restriction.
- Multilingual messages: English, Dutch, Spanish, French, Russian, Turkish, Chinese.

## Compatibility

- **Java:** 17+
- **Server software:** Bukkit, Spigot, Paper
- **Plugin API baseline:** 1.13+

### Optional integrations

- [Vault](https://www.spigotmc.org/resources/vault.34315/) / [VaultUnlocked](https://modrinth.com/plugin/vaultunlocked) - economy hooks for voucher purchasing
- [EzEconomy](https://modrinth.com/plugin/ezeconomy) - lightweight economy plugin tested with EzFlyTime
- [PlaceholderAPI](https://modrinth.com/mod/placeholderapi) - fly-time placeholders
- [mcMMO](https://modrinth.com/plugin/mcmmo) - skill-level auto-reward tiers
- [TeamsAPI](https://modrinth.com/plugin/teams-api) - `/f fly` team subcommand and claimed-chunks flight restriction

EzFlyTime runs safely even when optional integrations are missing.

## Installation

1. Download the EzFlyTime release jar.
2. Place it in your server's `plugins/` directory.
3. Start the server once to generate configuration files in `plugins/EzFlyTime/`.
4. Review `config.yml` and adjust vouchers, storage, and flight behavior.
5. Run `/ezflytime reload` after edits (full restart required only when changing `storage.type`).

### First-start checklist

- Set voucher prices and durations under `vouchers:` in `config.yml`.
- Set `storage.type: mysql` if you need cross-server persistence.
- Install Vault + an economy plugin if you want players to buy vouchers in-game.
- Enable `auto-flight-rewards` if you want players to accumulate time passively.

## Commands

### Player commands

- `/fly` - Toggle flight.
- `/flytime` - Show remaining fly time.
- `/flyparticles` - Manage particle trails.
- `/flyvoucher buy <id> [amount]` - Purchase a voucher with server currency.

### Admin commands

- `/flytime give <player> <time>` - Add flight time (e.g. `10m`, `1h`).
- `/flytime set <player> <time>` - Set flight time exactly.
- `/flytime remove <player> <time>` - Remove flight time.
- `/flyvoucher give <player> <id> [amount]` - Give vouchers to a player.
- `/ezflytime reload` - Reload all configuration files.

## Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `ezflytime.fly` | `true` | Use `/fly` |
| `ezflytime.flytime` | `true` | Use `/flytime` |
| `ezflytime.bypass` | `false` | Keep flight when time runs out |
| `ezflytime.give` | `op` | Give vouchers with `/flyvoucher give` |
| `ezflytime.reload` | `op` | Reload configuration |
| `ezflytime.admin` | `op` | All `/ezflytime` admin commands |
| `ezflytime.teams.fly.bypass` | `false` | Exempt from TeamsAPI claimed-chunks restriction |

## Configuration Files

| File | Purpose |
| :--- | :--- |
| `config.yml` | Core settings: storage, display, flight, boss bar, vouchers, auto-rewards |
| `particles.yml` | Particle trail definitions and speed thresholds |
| `particle-shop-gui.yml` | Particle shop GUI layout |
| `voucher-gui.yml` | Voucher shop GUI layout |
| `mcmmo.yml` | mcMMO skill reward tiers |
| `messages/<lang>.yml` | Translated message strings |

## Build from Source

```bash
# Compile
mvn -q -DskipTests compile

# Run tests
mvn -q test

# Full build
mvn -q -DskipTests package
```

### Build requirements

- Java 17
- Maven 3.8+

## Contributing

Contributions are welcome.

Suggested local verification flow:

1. `mvn -q -DskipTests compile`
2. `mvn -q test`
3. Include tests for non-trivial behavior changes when practical.
4. Keep changes focused and document any config or message key impacts.

## Support

If you run into issues, please open a [GitHub issue](https://github.com/ez-plugins/EzFlyTime/issues) with:

- Server platform and version (Paper / Spigot / Bukkit)
- Java version
- EzFlyTime version
- Relevant config snippets and console output

## License

EzFlyTime is licensed under the MIT License. See [LICENSE](LICENSE) for the full text.
