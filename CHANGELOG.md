# Changelog

All notable changes to EzFlyTime are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [3.0.0]

### Added

- Timed flight system with live boss bar countdown.
- Physical voucher items with PDC-backed unique identity and duplicate detection.
- Voucher shop GUI with Vault economy integration (`/flyvoucher buy`, `/flyvoucher gui`).
- Configurable particle trails during flight with per-player selection GUI and shop.
- Auto-flight reward system: flat, permission-based, and mcMMO skill-level tiers.
- Fuel display mode — show remaining flight as a percentage.
- YAML and MySQL storage backends.
- PlaceholderAPI expansion with time, seconds, minutes, and fuel placeholders.
- Multilingual messages: English, Dutch, Spanish, French, Russian, Turkish, Chinese.
- `/fly` toggle command with bypass permission support.
- `/flytime give|set|remove|top` admin commands.
- `/ezflytime reload|maxsingle` administration commands.
- Per-session flight cap (`max-single-flight-seconds`) with per-player bypass.
- Flight preservation across death and respawn (`preserve-on-death`).
- Creative and Spectator mode bypass options.
- SpigotMC update checker with configurable join notifications.
- bStats metrics integration.
- GitHub Pages documentation at <https://ez-plugins.github.io/EzFlyTime/>.
- GitHub Actions release workflow (GitHub Releases + Modrinth).
- GitHub Actions documentation workflow (Jekyll + Just The Docs).
