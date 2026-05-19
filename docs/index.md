---
title: Home
layout: home
nav_order: 1
---

EzFlyTime is a production-ready temporary flight plugin for Minecraft servers running
Bukkit, Spigot, or Paper. It gives players limited, timed flight powered by
vouchers, periodic rewards, and economy integration - with a polished boss bar,
particle trails, and a configurable GUI shop.

---

## Highlights

- **Timed flight** - players fly for a configured duration; time counts down live in a boss bar.
- **Voucher system** - physical items with PDC-backed identity, shop GUI, and duplicate detection.
- **Economy integration** - Vault-compatible purchasing via `/flyvoucher buy` or the GUI.
- **Particle trails** - configurable per-player particle effects with a lockable shop.
- **PlaceholderAPI support** - expose fly time and fuel values to scoreboards, chat, and GUIs.
- **Auto-flight rewards** - grant time on a timer, by permission group, or tied to mcMMO skill levels.
- **Fuel display mode** - optionally show remaining flight as a percentage instead of a countdown.
- **YAML or MySQL storage** - choose local file or database-backed persistence.
- **Multilingual messages** - English, Dutch, Spanish, French, Russian, Turkish, and Chinese included.

---

## For Server Owners & Admins

| | |
| :--- | :--- |
| [Getting Started](overview-installation) | Install EzFlyTime, verify your setup |
| [Commands](commands) | Player and admin command reference |
| [Permissions](permissions) | Permission nodes and recommended role assignments |
| [Messages & Localization](messages-localization) | Customize messages, add languages |
| [Operations & Troubleshooting](operations-troubleshooting) | Common issues and tuning tips |

## Configuration Reference

| | |
| :--- | :--- |
| [config.yml](config/config) | Core settings: storage, display, flight, rewards, bossbar, vouchers |
| [particles.yml](config/particles) | Particle trail definitions and shop entries |
| [particle-shop-gui.yml](config/particle-shop-gui) | Particle shop GUI layout |
| [voucher-gui.yml](config/voucher-gui) | Voucher shop GUI layout |
| [mcmmo.yml](config/mcmmo) | mcMMO skill-reward configuration |
| [messages/](messages-localization) | Localized message files |

## Integrations

| | |
| :--- | :--- |
| [Vault / Economy](integrations/vault-economy) | Charge players for voucher purchases |
| [PlaceholderAPI](integrations/placeholderapi) | Expose fly time in chat, scoreboards, GUIs |
| [mcMMO](integrations/mcmmo) | Skill-based auto-flight reward tiers |
| [EzEconomy](integrations/vault-economy#ezeconomy) | Lightweight economy tested with EzFlyTime |

## For Developers

| | |
| :--- | :--- |
| [Development Guide](development-collaboration) | Architecture, build workflow, contributor conventions |
