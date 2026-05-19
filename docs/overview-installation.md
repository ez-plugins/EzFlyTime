---
title: Getting Started
nav_order: 2
---

## What EzFlyTime does

EzFlyTime adds timed, voucher-powered flight to your server. Players do not get
permanent flight - they spend vouchers or receive automatic rewards to accumulate
flight time, and the plugin counts it down in real time via a boss bar.

Key capabilities:

- `/fly` toggle with a live countdown boss bar
- Voucher items that grant flight time on right-click
- In-game GUI shop for buying vouchers with server currency
- Periodic auto-flight rewards (flat, permission-based, or mcMMO-tied)
- Cosmetic particle trails during flight
- YAML or MySQL persistence
- PlaceholderAPI placeholders for time and fuel values

## Compatibility and requirements

- **Java:** 17+
- **Server software:** Bukkit, Spigot, Paper
- **Plugin API baseline:** 1.13+

Optional soft dependencies (EzFlyTime runs safely without any of these):

- [Vault](https://www.spigotmc.org/resources/vault.34315/) / [VaultUnlocked](https://modrinth.com/plugin/vaultunlocked) - enables economy features (voucher buying)
- [EzEconomy](https://modrinth.com/plugin/ezeconomy) - lightweight economy plugin tested with EzFlyTime
- [PlaceholderAPI](https://modrinth.com/mod/placeholderapi) - exposes fly-time placeholders
- [mcMMO](https://modrinth.com/plugin/mcmmo) - unlocks skill-level auto-reward tiers

---

## Installation

### Install steps

1. Stop the server.
2. Drop `EzFlyTime-<version>.jar` into your server's `plugins/` directory.
3. Start the server once to generate configuration files in `plugins/EzFlyTime/`.
4. Stop the server.
5. Edit `plugins/EzFlyTime/config.yml` to configure vouchers, storage, and flight behavior.
6. Start the server and run `/ezflytime reload` (or restart) after configuration changes.

### First-start checklist

- Review `vouchers:` in `config.yml` - adjust prices and durations for your economy.
- Set `storage.type` to `mysql` if you run a multi-instance or high-traffic setup.
- Install Vault (and an economy provider) if you want players to purchase vouchers in-game.
- Enable `auto-flight-rewards` if you want players to accumulate time passively.
- Configure `bossbar:` colors and title template to match your server's style.

---

## File layout

On first start, EzFlyTime generates these files under `plugins/EzFlyTime/`:

| File | Purpose |
| :--- | :--- |
| `config.yml` | Core plugin settings |
| `particles.yml` | Particle trail definitions and shop entries |
| `particle-shop-gui.yml` | Particle shop GUI layout |
| `voucher-gui.yml` | Voucher shop GUI layout |
| `mcmmo.yml` | mcMMO skill reward tiers |
| `messages/<lang>.yml` | Translated message strings |
| `data/` | YAML storage files (if `storage.type: yaml`) |

---

## Building from source

```bash
# Compile and package
mvn -q -DskipTests package

# Run tests
mvn -q test
```

The shaded jar is output to `target/EzFlyTime-<version>.jar`.

### Build requirements

- Java 17
- Maven 3.8+
