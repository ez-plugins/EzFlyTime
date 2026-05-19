# EzFlyTime
Temporary fly time for Minecraft servers

## Building

Run `mvn clean package` to build the plugin. The shaded jar is created under `target/`.

## Configuration Highlights

- **`display.flytime-mode`** – set to `time` (default) to show remaining fly time as a duration, or `fuel` to show it as a percentage ("fuel") in boss bars, messages, and placeholders.
- **`flight.activation-mode`** – choose between traditional creative flight (`NORMAL`) or double-jump triggered Elytra gliding (`DOUBLE_JUMP_ELYTRA`).

### Fly Time Display Modes

You can globally control how remaining fly time is shown to players:

- `display.flytime-mode: time` (default): Shows time left (e.g., `1h 2m 3s`).
- `display.flytime-mode: fuel`: Shows a percentage (e.g., `Flight fuel: 75%`).

This affects boss bars, messages, and placeholders. Message templates for both modes are available in the language files (e.g., `flight-fuel-remaining`, `flight-enabled-fuel`).

## Voucher Redemption

Each fly voucher now carries a unique identifier that is consumed and recorded when a player redeems it. The plugin keeps track of the identifiers it has already processed and warns players if they attempt to redeem the same voucher twice. Administrators can disable duplicate detection with the `detect-voucher-dupes` setting in `config.yml`, but the plugin will continue storing redeemed identifiers so that detection can be safely re-enabled later.
