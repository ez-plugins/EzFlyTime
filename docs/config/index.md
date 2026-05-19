---
title: Configuration
nav_order: 8
has_children: true
---

# Configuration

EzFlyTime splits its configuration across focused files for maintainability.
All files are generated with defaults on first server start and live under
`plugins/EzFlyTime/`.

| File | Purpose |
| :--- | :--- |
| [`config.yml`](config) | Core settings: language, storage, flight, rewards, boss bar, vouchers |
| [`particles.yml`](particles) | Particle trail definitions and speed thresholds |
| [`particle-shop-gui.yml`](particle-shop-gui) | Particle shop GUI slot layout |
| [`voucher-gui.yml`](voucher-gui) | Voucher shop GUI slot layout and purchase settings |
| [`mcmmo.yml`](mcmmo) | mcMMO skill-level reward tiers |
| `messages/<lang>.yml` | Translated message strings (see [Messages](../messages-localization)) |
