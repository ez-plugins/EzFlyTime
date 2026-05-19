---
title: voucher-gui.yml
parent: Configuration
nav_order: 4
---

# voucher-gui.yml

Controls the layout and appearance of the voucher shop GUI. Located at
`plugins/EzFlyTime/voucher-gui.yml`.

---

## GUI settings

```yaml
gui:
  title: "&6&lFlight Shop &7- &eBuy Vouchers"
  size: 27
  background:
    material: "GRAY_STAINED_GLASS_PANE"
    name: "&7"
    lore: []
  close-button:
    slot: 26
    material: "BARRIER"
    name: "&cClose Shop"
    lore:
      - "&7Click to close the voucher shop"
  info-button:
    slot: 18
    material: "BOOK"
    name: "&eFlight Information"
    lore:
      - "&7Welcome to the Flight Shop!"
```

- `size` must be a multiple of 9 (9, 18, 27, 36, 45, 54).
- `close-button` and `info-button` accept any valid slot number within the GUI size.

---

## Voucher display entries

```yaml
vouchers:
  basic:
    slot: 10
    material: "PAPER"
    custom-model-data: 0
    name: "&aBasic Flight Voucher"
    lore:
      - "&7Gives you &e5 minutes &7of flight time"
      - "&7"
      - "&6Price: &e{price} {currency}"
      - "&7"
      - "&eClick to purchase!"
      - "&7Hold shift for 5x purchase"
    glow: false
```

Each key under `vouchers:` must match a key defined in the `vouchers:` section of
`config.yml`.

| Key | Description |
| :--- | :--- |
| `slot` | Inventory slot number for this voucher's icon. |
| `material` | Icon item material. |
| `custom-model-data` | Custom model data value (`-1` to disable). |
| `name` | Display name shown in the GUI. |
| `lore` | Tooltip lines. Supports `{price}` and `{currency}` placeholders. |
| `glow` | When `true`, the item appears enchanted (glowing). |

---

## Purchase settings

```yaml
purchase:
  default-amount: 1
  shift-amount: 5
```

- `default-amount` — vouchers purchased per normal click.
- `shift-amount` — vouchers purchased per shift-click.
