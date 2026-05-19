---
title: particles.yml
parent: Configuration
nav_order: 2
---

Defines particle trail effects shown during flight. Located at
`plugins/EzFlyTime/particles.yml`.

---

## Global toggle

```yaml
enabled: true
```

Set to `false` to disable all particle trails globally.

---

## Particle entry structure

```yaml
particles:
  primary:
    enabled: true
    type: CLOUD
    count: 2
    speed: 0.0
    offset:
      x: 0.0
      y: -1.0
      z: 0.0
    directional: false
    spread: 0.0
    size: 1.0
    rainbow: false
    speed-responsive: false
    min-speed: 0.0
    trail:
      enabled: false
```

| Key | Description |
| :--- | :--- |
| `type` | Bukkit `Particle` enum name (e.g. `CLOUD`, `CRIT_MAGIC`, `FIREWORKS_SPARK`). |
| `count` | Number of particles spawned per tick. |
| `speed` | Particle velocity magnitude. |
| `offset.x/y/z` | Spawn offset relative to the player's feet. |
| `directional` | When `true`, particles are fired in the player's movement direction. |
| `spread` | Random spread radius around the spawn point. |
| `size` | Scale multiplier (not supported by all particle types). |
| `rainbow` | Cycle through hue colors each spawn. |
| `speed-responsive` | Increase particle count proportional to player speed. |
| `min-speed` | Minimum player speed (blocks/s) required to spawn this effect. |
| `trail.enabled` | Draw a persistent trail behind the player. |

---

## Shop metadata

Particles that should appear in the particle shop require a `shop:` block:

```yaml
particles:
  vip:
    type: CRIT_MAGIC
    # ... other keys ...
    shop:
      price: 1000
      material: NETHER_STAR
      name: "VIP Flight"
```

- `price` — cost in economy currency.
- `material` — icon material in the shop GUI.
- `name` — display name in the shop GUI.

The corresponding slot in `particle-shop-gui.yml` must reference the same particle ID.

---

## Built-in speed effects

EzFlyTime ships with three default entries:

| Entry | Trigger | Effect |
| :--- | :--- | :--- |
| `primary` | Always | Clouds directly beneath feet |
| `extra` | Speed > 10 b/s | Firework sparks |
| `sonic-boom` | Speed > 20 b/s | Large explosion behind player |
