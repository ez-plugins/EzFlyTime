# EzFlyTime - Flight Vouchers & Bossbar for Spigot/Paper servers

EzFlyTime provides a production-ready flight-time system, a customizable voucher shop GUI, and optional bossbar/fuel displays. Built for Vault and PlaceholderAPI compatibility with robust voucher protection (PDC + legacy fallback).

![Voucher Shop GUI](https://i.ibb.co/MxWXXnDr/image.png)

## Why use EzFlyTime?

- Lightweight and configurable (YAML or MySQL storage)
- Voucher shop with Vault integration for purchases
- Persistent, secure voucher metadata with legacy fallback
- Bossbar & particle integrations for a polished player experience

![Advanced Particles](https://i.ibb.co/DPbhQcPv/ezflytime-advanced-particles-1.png)
![Bossbar with flight speed meter and flight time left over](https://i.ibb.co/JWTh3b4f/image.png)

## Quick Install

1. Place the `EzFlyTime` jar into your `plugins/` folder and start the server.
2. Review `plugins/EzFlyTime/config.yml` and adjust `vouchers:` and `bossbar:`.
3. (Optional) Install Vault plus an economy plugin (e.g., EzEconomy: https://modrinth.com/plugin/ezeconomy) to enable `/flyvoucher buy` and economy features.
4. Run `/flytime reload` after edits or restart the server.

## Commands

- `/fly` — Toggle flight (players).
- `/flytime` — Show remaining time; run with no args to open the voucher shop GUI (players with `ezflytime.buy`).
- `/flytime reload` — Reload configuration and recreate managers.
- `/flyvoucher gui` — Open the voucher shop GUI.
- `/flyvoucher give <player> <voucherId> [amount]` — Give vouchers.
- `/flyvoucher buy <voucherId> [amount]` — Buy vouchers via economy.

## Permissions

- `ezflytime.fly` — Toggle flight (default: true)
- `ezflytime.flytime` — View remaining time (default: true)
- `ezflytime.buy` — Access purchases & GUI (default: true)
- `ezflytime.give` — Admin: give vouchers (default: OP)
- `ezflytime.reload` — Admin: reload config (default: OP)

## Language Settings
Determines the language used for plugin messages.

- Supported codes: `en` (English), `nl` (Dutch), `es` (Spanish), `fr` (French), `ru` (Russian), `tr` (Turkish), `zh` (Chinese)
- Message files located at `plugins/EzFlyTime/messages/` after first run.

To add support for an additional language, add `messages/messages_{code}.yml` to the `src/main/resources/messages/` directory and provide the translated keys.

## Configuration Highlights

Vouchers live under the `vouchers:` section in `config.yml`.
Flight per-session limit:

```yaml
# under `flight:`
# Maximum seconds allowed for a single continuous flight session.
# Set to 0 to disable per-session limits (default).
max-single-flight-seconds: 0
```

Example voucher:

```yaml
vouchers:
  basic:
    material: PAPER
    name: '&aBasic Fly Voucher'
    lore:
      - '&7Redeem for 5 minutes of flight.'
    duration-seconds: 300
    # Prices support compact notation: 1k, 2.5m, 3b, 4t
    price: 1k
```

`voucher-gui.yml` controls the shop layout and materials. EzFlyTime uses a resolver to support both legacy and modern `Material` names, but verify after major server updates.

Bossbar example:

```yaml
bossbar:
  enabled: true
  title: '&aFlight Time: {time}'
  color: GREEN
  style: SEGMENTED_10
```

## PlaceholderAPI

- `%ezflytime_time_remaining%` / `%ezflytime_formatted%` — formatted time
- `%ezflytime_time_remaining_raw%` / `%ezflytime_seconds%` — raw seconds
- `%ezflytime_minutes%` — whole minutes

## Integrations
- **EzEconomy** — lightweight economy plugin tested with EzFlyTime: [EzEconomy on Modrinth](https://modrinth.com/plugin/ezeconomy)
- **PlaceholderAPI** — register placeholders to expose flight time and formatted values to chat, scoreboards and GUIs.
- **mcMMO** — supported as a soft-dependency for servers using mcMMO mechanics.

Tips:
- Install Vault and an economy plugin (EzEconomy or another supported provider) to enable purchase flows and price/cost features.
- PlaceholderAPI placeholders require the PAPI plugin and the EzFlyTime expansion (if available) to be registered.
- Integrations are optional; EzFlyTime runs in a degraded mode if external plugins are absent.

## Troubleshooting & Best Practices

- Enable `debug` in `config.yml` to log voucher item meta and PDC reads if vouchers fail to redeem.
- Test vouchers on a clean server when using anti-dupe or serialization plugins — some external plugins strip metadata; EzFlyTime falls back to legacy lore.

## Particles configuration

EzFlyTime supports built-in particle effects for flight visuals. There is a companion `EzFlyTimeAnimations` module mentioned historically, however that module is not released and will not be released. Use the built-in particle configuration below or request example templates. Particle effects are configured in `particles.yml` (in the plugin resources or `plugins/EzFlyTime/` after first run). Below are the supported keys and a complete example.

Configuration reference (keys and descriptions):
- `enabled` (boolean): globally enable/disable all plugin particle effects.
- `mode` (string): where particles appear; common values: `trail`, `wing`, `ambient`, `on-toggle`.
- `particle` (string): the `Particle` enum name used by the server (e.g., `REDSTONE`, `CLOUD`, `SPELL`, `FLAME`, `TOTEM`, `SMOKE_NORMAL`). Use names available for your Minecraft server version.
- `count` (int): number of particles spawned per spawn event. Lower values are cheaper.
- `speed` (float): particle speed parameter passed to the server API.
- `offset` (map): `{ x: 0.1, y: 0.2, z: 0.1 }` — positional random offset.
- `frequency` (int): ticks between each particle spawn (20 ticks = 1 second). Increase this value to lower CPU usage.
- `perPlayer` (boolean): whether particles are spawned per-player (true) or globally (false). Per-player is more private but more CPU intensive.
- `onlyWhenFlying` (boolean): spawn effects only while the player is flying.
- `distanceLimit` (double): do not spawn for players farther than this value from viewers (saves performance on large servers).
- `sound` (map): optional sound to play with particle; keys: `name` (Sound enum), `volume`, `pitch`.
Example `particles.yml`:
```yaml
enabled: true
default:
  mode: trail
  particle: REDSTONE
  count: 6
  speed: 0.02
  offset:
    y: 0.1
  color:
    r: 0.1
    g: 0.8
    b: 1.0
  frequency: 4            # spawn every 4 ticks (5 times per second)
  perPlayer: true
  onlyWhenFlying: true
  distanceLimit: 48.0
  sound:
    name: ENTITY_PLAYER_LEVELUP
    volume: 0.5
    pitch: 1.0

wing-effect:
  mode: wing
  particle: CLOUD
  count: 3
  speed: 0.01
  radius: 0.7
  frequency: 6
  perPlayer: true
  onlyWhenFlying: true

ambient:
  enabled: false
  mode: ambient
  particle: SPELL_WITCH
  count: 1
  frequency: 40
```

Notes & best practices:

- Particle names and parameters are server-version dependent. If the configured `particle` is invalid on your server version the plugin will log a warning and skip that effect. Use the built-in resolver or consult your server's `org.bukkit.Particle` for valid names.
- For large servers prefer `perPlayer: false`, higher `frequency` and lower `count` values. Example low-cost settings: `count: 1`, `frequency: 20`.
- Use `distanceLimit` to reduce client-side spam for viewers far away.
-- Colored `REDSTONE` particles require the server to accept dust/colour parameters; if color is ignored on your server, switch to simpler particle types (e.g., `SPELL`, `FLAME`). The previously referenced `EzFlyTimeAnimations` module is not released and will not be released.
- If you rely on advanced animations (wing shapes, lerped trails), test on a staging server first and enable `debug` to log spawn-frequency and any color-mapping fallbacks.

Troubleshooting particles:

- No particles visible: check `enabled`, `onlyWhenFlying`, and the player's client settings (particles reduced/ambient settings). Also confirm server and client versions are compatible with the particle type.
- High CPU or lag: reduce `count`, increase `frequency`, disable `perPlayer` or lower `distanceLimit`.
- Color not applied for REDSTONE: your server version may require a different dust/color format; try using normalized 0–1 floats or integer RGB depending on server logs.

For advanced custom effects, request an example wing/trail pattern and I'll add ready-to-copy templates for `particles.yml`.

## Full configuration checklist

- `config.yml`: vouchers, bossbar, storage, debug and dupe-detection toggles.
- `voucher-gui.yml`: GUI layout, materials, slots, close/confirm buttons.
- `particles.yml`: particle effects for flights and cosmetics (see above).
- `messages_*.yml`: translation files for messages; edit safely and run `/flytime reload`.

## Voucher examples & troubleshooting

### Quick voucher example (buyable)

```yaml
vouchers:
  basic:
    material: PAPER
    name: '&aBasic Fly Voucher'
    lore:
      - '&7Redeem for &e5 minutes&7 of flight.'
      - '&6Price: &e{price} {currency}'
    duration-seconds: 300
    price: 1k # supports k/m/b/t (e.g. 1k = 1000, 2.5m = 2500000)
```

### GUI purchase example (voucher-gui.yml snippet)

```yaml
vouchers:
  basic:
    slot: 10
    material: PAPER
    name: '&aBasic Voucher'
    lore:
      - '&7Gives you &e5 minutes &7of flight time'
      - '&6Price: &e{price} {currency}'
    glow: false
```

Placeholders available in GUI names and lore: `{price}`, `{currency}`, `{minutes}`, `{seconds}`.

### Common issues & fixes

- Voucher not redeeming: enable `debug: true` and check server logs for PDC reads or legacy lore lines. If metadata is stripped by another plugin, consider disabling `detect-voucher-dupes` or using the legacy lore fallback.
- Purchases failing with economy: ensure Vault + an economy plugin (e.g., EzEconomy) are installed and configured. Check `voucher-gui.yml` `economy` section for currency display settings.

- Price display formatting: `voucher-gui.yml` now has `economy.show-formatted-price` (default `true`). When enabled the GUI shows compact prices like `1k`, `2.5m`, `3b`. When disabled the GUI shows raw numeric values (e.g., `1000` or `2500.50`).
- Duplicate detection false positives: set `detect-voucher-dupes: false` in `config.yml` to disable, or inspect `target/surefire-reports` during testing to see how UUIDs are applied.

### Advanced: PDC vs Legacy lore

- PDC (preferred) stores secure fields (`ezflytime.voucher.id`, `ezflytime.voucher.duration`, `ezflytime.voucher.unique_id`, `ezflytime.voucher.server_uuid`, `ezflytime.voucher.created_at`) and enforces server-UUID and age checks for security.
- Legacy lore fallback appends a dark-gray lore line `EzFlyTime:<id>:<duration>:<uuid>` for compatibility with older servers or when PDC is unavailable.

### Migration & storage notes

- Consumed voucher UUIDs are persisted by `VoucherStorage` (yaml or MySQL). When moving servers, export/import the consumed UUID list to avoid re-using consumed vouchers.
- If you change storage to MySQL, confirm `storage.type: mysql` and set the `mysql` object in `config.yml` with credentials and `table-prefix`.


## Contact & support

For support and feature requests join the official Discord: https://discord.gg/yWP95XfmBS

[![Try the other Minecraft plugins in the EzPlugins series](https://i.ibb.co/PzfjNjh0/ezplugins-try-other-plugins.png)](https://modrinth.com/collection/Q98Ov6dA)