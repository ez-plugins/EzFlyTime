---
title: particle-shop-gui.yml
parent: Configuration
nav_order: 3
---

Controls the layout of the particle shop inventory GUI. Located at
`plugins/EzFlyTime/particle-shop-gui.yml`.

---

## Top-level settings

```yaml
title: "Particle Shop"
rows: 3
background-item:
  material: "BLACK_STAINED_GLASS_PANE"
  display-name: " "
```

- `title` - inventory title shown at the top of the GUI.
- `rows` - number of rows (1–6, must accommodate all configured slots).
- `background-item` - material used to fill empty slots.

---

## Slot entries

```yaml
slots:
  10:
    id: "basic"
    material: "FEATHER"
    display-name: "Basic Flight"
    price: 100
    permission: "ezflytime.particles.basic"
```

Each top-level key is the **slot number** (0–53 for a 6-row chest).

| Key | Description |
| :--- | :--- |
| `id` | Particle ID - must match a key in `particles.yml`. |
| `material` | Icon item material. |
| `display-name` | Text shown on the item. |
| `price` | Economy cost to purchase. `0` makes the particle free. |
| `permission` | Permission required to purchase this particle. Leave empty to allow all. |

---

## Layout tips

- Slot `0` is the top-left corner of the inventory.
- For a 3-row (27-slot) GUI, valid slots are `0`–`26`.
- Leave a gap of at least one slot between entries for visual clarity.
- Use `rows: 4` or higher if you have many particles.
