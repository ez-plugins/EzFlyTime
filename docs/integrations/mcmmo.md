---
title: mcMMO
parent: Integrations
nav_order: 3
---

## Overview

When [mcMMO](https://modrinth.com/plugin/mcmmo) is installed, EzFlyTime can grant
flight time based on players' skill levels as part of the auto-flight reward cycle.

Without mcMMO, the skill-based reward tier is silently skipped and all other
reward types (flat, permission-based) continue to work normally.

---

## Setup

1. Install mcMMO.
2. Ensure `auto-flight-rewards.enabled: true` in `config.yml`.
3. Configure skill thresholds in `plugins/EzFlyTime/mcmmo.yml`:

```yaml
enabled: true
global-multiplier: 1.0
skills:
  MINING:
    multiplier: 1.0
    thresholds:
      100: 30   # 30 s per cycle at Mining 100+
      500: 90   # +90 s per cycle at Mining 500+
```

4. Restart the server or run `/ezflytime reload`.

---

## How rewards are calculated

For each online player, EzFlyTime iterates over all configured skills.
For each skill, it checks the player's current level against every threshold:

- All thresholds the player **meets or exceeds** are summed.
- The per-skill `multiplier` and the `global-multiplier` are applied.
- The final amount is added to that reward cycle's total for the player.

**Example:** a player with Mining level 600 in the configuration above receives
`(30 + 90) × 1.0 × 1.0 = 120` seconds per reward cycle from the Mining skill alone.

---

## Configuration reference

See [mcmmo.yml](../config/mcmmo) for the full configuration reference.
