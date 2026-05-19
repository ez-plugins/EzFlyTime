---
title: Vault / Economy
parent: Integrations
nav_order: 1
---

## Overview

EzFlyTime uses [Vault](https://www.spigotmc.org/resources/vault.34315/) as an
economy abstraction layer. When Vault and a compatible economy plugin are present,
players can purchase vouchers using server currency.

Without Vault, the plugin disables `/flyvoucher buy`, the GUI purchase flow, and
any voucher `price` checks. All other features continue to work normally.

---

## Setup

1. Install [Vault](https://www.spigotmc.org/resources/vault.34315/).
2. Install a Vault-compatible economy plugin. [EzEconomy](https://modrinth.com/plugin/ezeconomy)
   is a lightweight option tested with EzFlyTime.
3. Start or restart the server. EzFlyTime detects Vault at startup and logs
   `[EzFlyTime] Vault economy hooked` when successful.

---

## Voucher pricing

Set the price for each voucher in `config.yml`:

```yaml
vouchers:
  basic:
    price: 1000   # 0 disables purchasing for this voucher
  premium:
    price: 2500
```

Setting `price: 0` marks a voucher as give-only — it will not appear in the buy
flow or GUI purchase actions.

---

## EzEconomy {#ezeconomy}

[EzEconomy](https://modrinth.com/plugin/ezeconomy) is a Vault-compatible economy
plugin developed by the same team. It integrates seamlessly with EzFlyTime's
voucher purchase system.

Install and configure EzEconomy the same way as any Vault provider — no special
configuration is needed on the EzFlyTime side.
