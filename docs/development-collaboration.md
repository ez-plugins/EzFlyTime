---
title: Development Guide
nav_order: 7
---

# Development Guide

## Repository layout

```
EzFlyTime/
├── src/main/java/com/ezflytime/   — plugin source
│   ├── EzFlyTimePlugin.java       — plugin entry point
│   ├── bootstrap/                 — startup wiring (commands, services, storage)
│   ├── command/                   — command handlers
│   ├── config/                    — ConfigManager
│   ├── flight/                    — FlyTimeManager, boss bar handler, auto-reward
│   ├── gui/                       — inventory GUIs (voucher shop, particle shop)
│   ├── mcmmo/                     — mcMMO integration config
│   ├── particles/                 — particle config, manager, spawner, trails
│   ├── placeholder/               — PlaceholderAPI expansion
│   ├── storage/                   — storage abstractions (YAML + MySQL)
│   ├── update/                    — update checker
│   ├── util/                      — shared utilities
│   └── voucher/                   — voucher model and redemption logic
├── src/main/resources/            — default configs and plugin.yml
├── src/test/                      — unit tests
├── docs/                          — GitHub Pages documentation (you are here)
└── pom.xml                        — build descriptor
```

## Build workflow

```bash
# Compile only
mvn -q -DskipTests compile

# Full build
mvn -q -DskipTests package

# Run all tests
mvn -q test
```

The shaded plugin jar is placed in `target/EzFlyTime-<version>.jar`.

### Build requirements

- Java 17
- Maven 3.8+

## Suggested local verification flow

1. `mvn -q -DskipTests compile` — fast compile check
2. `mvn -q test` — run the test suite
3. Drop the jar into a local test server and verify your feature manually.
4. Open a pull request — the CI workflow runs compile, test, and builds automatically.

## Contributor conventions

- Keep changes focused; one feature or fix per pull request.
- Include unit tests for non-trivial logic changes when practical.
- Document any config keys or message keys you add or change.
- Run `mvn test` locally before pushing — the CI pipeline must pass before merge.

## Storage abstraction

Flight time, unlocked particles, and voucher state are each backed by an interface
with both YAML and MySQL implementations:

| Interface | YAML impl | MySQL impl |
| :--- | :--- | :--- |
| `FlyTimeStorage` | `YamlFlyTimeStorage` | `mysql/MysqlFlyTimeStorage` |
| `UnlockedParticlesStorage` | `YamlUnlockedParticlesStorage` | `mysql/MysqlUnlockedParticlesStorage` |
| `VoucherStorage` | `YamlVoucherStorage` | `mysql/MysqlVoucherStorage` |

The active implementation is selected at startup based on `storage.type` in `config.yml`.

## Adding a new voucher programmatically

Define the voucher in `config.yml` under `vouchers:`. The plugin loads all
entries at startup; no code changes are required for new voucher types.

## Adding a new particle trail

1. Add an entry to `particles.yml` with the desired particle type and metadata.
2. Add a corresponding slot entry to `particle-shop-gui.yml` if it should appear in the shop.
3. Assign the permission node `ezflytime.particles.<id>` to the appropriate groups.

## Support

For questions about contributing or the internals, open a
[GitHub issue](https://github.com/ez-plugins/EzFlyTime/issues) or start a discussion.
