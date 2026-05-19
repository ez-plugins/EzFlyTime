---
title: Operations & Troubleshooting
nav_order: 6
---

## Common issues

### Players cannot fly after receiving a voucher

- Check that the player has the `ezflytime.fly` permission.
- Confirm that the voucher was redeemed (right-click the item). The plugin outputs a
  confirmation message on successful redemption.
- If `detect-voucher-dupes: true`, a used voucher will be rejected silently to the
  player and flagged to operators. Check the console and the
  `ezflytime.notify` permission group.

### Flight stops unexpectedly

- Verify that `max-single-flight-seconds` in `config.yml` is not set too low.
  Set it to `0` to disable per-session caps.
- Check whether the player's remaining time reached zero - the boss bar disappears
  and flight is disabled when time runs out.
- If players have `ezflytime.bypass`, confirm that `bypass-grants-unlimited: true`
  is set and the player has the permission node assigned in their group.

### Boss bar does not appear

- Confirm `bossbar.enabled: true` in `config.yml`.
- Verify the server version supports boss bars (1.9+). EzFlyTime uses the Bukkit
  `BossBar` API and gracefully disables the feature when the API is unavailable.

### Economy features are unavailable

- Install [Vault](https://www.spigotmc.org/resources/vault.34315/) and a Vault-compatible
  economy plugin (e.g. [EzEconomy](https://modrinth.com/plugin/ezeconomy)).
- Check the server console at startup for a `[EzFlyTime] Vault economy not found` warning.
- Purchasing vouchers requires both Vault and a registered economy provider.

### PlaceholderAPI placeholders show as raw text

- Confirm PlaceholderAPI is installed and the EzFlyTime expansion is registered.
- Run `/papi list` to verify `ezflytime` appears in the registered expansions.
- Check for errors in the startup log under the PlaceholderAPI section.

### MySQL connection fails

- Verify your `storage.mysql` credentials in `config.yml`.
- Ensure the database server is reachable from your Minecraft host.
- Check that the database and user exist and the user has `CREATE`, `SELECT`,
  `INSERT`, `UPDATE`, and `DELETE` privileges.
- Enable `debug: true` for verbose connection logging.

---

## Debug mode

Set `debug: true` in `config.yml` and reload to enable verbose console output.
Disable it again in production - it significantly increases log volume.

```yaml
debug: true
```

---

## Reloading configuration

```text
/ezflytime reload
```

This reloads `config.yml`, all message files, and recreates storage and manager
instances. A full server restart is only required when changing `storage.type`.

---

## Performance notes

- **Auto-save interval:** `auto-save.interval-seconds` controls how often data is
  written to disk (YAML) or synced to the database. The default (300 s) is safe for
  most servers. Lowering it increases I/O; raising it risks losing up to that many
  seconds of data on a hard crash.
- **Auto-flight rewards interval:** `auto-flight-rewards.interval-seconds` runs a
  task that iterates over online players. Keep this above 60 s on servers with many
  concurrent players.
- **MySQL vs YAML:** Use MySQL for networks with multiple servers sharing flight time
  data or for high player counts where file I/O may become a bottleneck.

---

## Reporting a bug

Open a [GitHub issue](https://github.com/ez-plugins/EzFlyTime/issues) and include:

- Server platform and version (Paper / Spigot / Bukkit)
- Java version
- EzFlyTime version
- Relevant sections from `config.yml`
- Console output (with `debug: true` if applicable)

This helps reproduce the problem quickly and keeps support requests actionable.
