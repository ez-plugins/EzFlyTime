---
title: mcmmo.yml
parent: Configuration
nav_order: 5
---

Controls how mcMMO skill levels map to auto-flight reward amounts. Located at
`plugins/EzFlyTime/mcmmo.yml`.

This file is only active when mcMMO is installed and
`auto-flight-rewards.mcmmo.enabled` is `true` (or when `mcmmo.yml` exists and
`enabled: true` is set here). If `mcmmo.yml` is absent, EzFlyTime falls back to
the legacy `auto-flight-rewards.mcmmo` section in `config.yml`.

---

## Top-level settings

```yaml
enabled: true
permission-bypass: ezflytime.autoreward.mcmmo.bypass
global-multiplier: 1.0
```

- `enabled` — enable or disable mcMMO-based rewards entirely.
- `permission-bypass` — players with this node are excluded from mcMMO reward calculations.
- `global-multiplier` — multiply all mcMMO-derived reward values by this factor.

---

## Skill entries

```yaml
skills:
  EXCAVATION:
    multiplier: 1.0
    thresholds:
      100: 30
      500: 90
  FISHING:
    multiplier: 1.0
    thresholds:
      100: 10
      500: 60
```

Each key is an mcMMO skill name (uppercase). The `thresholds` map skill levels to
seconds granted per auto-reward cycle.

- `multiplier` — per-skill multiplier applied on top of `global-multiplier`.
- `thresholds` — map of `<level>: <seconds>`. A player at or above a threshold level
  receives that many seconds. Multiple thresholds are cumulative: a player at level 500
  in the example above receives **both** the 100- and 500-level rewards.

---

## Supported skill names

Any valid mcMMO skill name can be used. Common examples:

`ACROBATICS`, `ALCHEMY`, `ARCHERY`, `AXES`, `EXCAVATION`, `FARMING`, `FISHING`,
`HERBALISM`, `MINING`, `REPAIR`, `SALVAGE`, `SMELTING`, `SWORDS`, `TAMING`,
`UNARMED`, `WOODCUTTING`
