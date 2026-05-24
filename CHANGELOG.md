# Changelog

All notable changes to EzFlyTime are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [3.0.3] - 2026-05-24

### Changed

- **Split unlimited-flight disabled message** - Show a different message when a player disables flight while they have unlimited flight. Added the `flight-disabled-unlimited` message key and updated the command and teams subcommand to use it when appropriate.

## [3.0.2] - 2026-05-20

### Changed

- **[TeamsAPI](https://modrinth.com/plugin/teams-api) dependency bumped 1.8.0 → 2.0.0** - the 2.0.0 release adds
  `TeamRelation.MEMBER` and adjusts the default colours of `ALLY` and `TRUCE`.
  EzFlyTime does not use `TeamRelation`, so no code changes were required.

### Added

- **TeamsAPI integration smoke tests** - 112 parameterized tests across all
  compatible server software (Paper, Spigot, Purpur, Folia) and Minecraft
  versions (1.16.5 – 1.21.5) guard against regressions of the
  `NoClassDefFoundError` crash fixed in 3.0.1.

### Fixed

- **Spurious `particle-shop-gui.yml` save warning** - `StartupBootstrap` called
  `saveResource("particle-shop-gui.yml", false)` unconditionally on every startup,
  producing `Could not save … because … already exists` after the first run.
  The call was redundant: `ParticleShopGUI` already guards the same `saveResource`
  behind an existence check, so the `StartupBootstrap` call has been removed.

- **[TeamsAPI](https://modrinth.com/plugin/teams-api) soft-dependency crash** - `NoClassDefFoundError: com/skyblockexp/teamsapi/api/TeamsSubcommand`
  was thrown on startup whenever TeamsAPI was absent. Bukkit's class loader
  eagerly loads field-type classes in the same jar, so `FlySubcommand` (and
  transitively its `TeamsSubcommand` interface) was resolved the moment
  `TeamsIntegration` was instantiated, before any availability guard ran.
  The `flySubcommand` field is now typed as `Object`; all TeamsAPI references
  remain in method bodies and are resolved lazily at execution time.

## [3.0.1] - 2026-05-20

### Fixed

- **[TeamsAPI](https://modrinth.com/plugin/teams-api) soft-dependency crash** - `NoClassDefFoundError: com/skyblockexp/teamsapi/api/TeamsSubcommand`
  was thrown on startup whenever TeamsAPI was absent. The JVM's bytecode
  verifier resolves `FlySubcommand` (and transitively `TeamsSubcommand`) when
  `TeamsIntegration` is first class-loaded or its methods are first invoked —
  before any runtime `getPlugin("TeamsAPI")` guard inside the class can run.
  `StartupBootstrap` now guards the `new TeamsIntegration(…)` call behind a
  `getPlugin("TeamsAPI") != null` check so the class (and its TeamsAPI
  dependencies) are never loaded when TeamsAPI is absent. A `Throwable` catch
  around the block also handles servers where TeamsAPI is installed but at an
  incompatible API version.

### Added

- **`{time}` placeholder in `flight-disabled` message** - the remaining flight
  time is now substituted into `flight-disabled` messages (e.g.
  `&cFlight disabled. Remaining time: {time}.`). Updated all 8 bundled locales
  (en, nl, de, fr, es, ru, tr, zh) to include the placeholder by default.

---

## [3.0.0] - 2026-05-19

This release is a major overhaul of EzFlyTime.  Nearly every system has been
rewritten or significantly extended.  **Existing `config.yml` and message files
will need to be updated** - delete them and let the plugin regenerate defaults,
then re-apply your customisations.

### Added

- **TeamsAPI integration** (optional soft dependency) - integrates with
  [TeamsAPI](https://modrinth.com/plugin/teams-api) when the plugin is present:
  - Registers a `/f fly` subcommand in the team plugin's command tree so players
    can toggle EzFlyTime flight from within their team commands.
  - New `teams.claimed-chunks-only` config option (default `false`): when enabled,
    EzFlyTime-managed flight is restricted to the player's own team-claimed chunks.
    Entering an unclaimed or enemy chunk mid-flight automatically lands the player.
  - New `teams.enabled` config toggle to disable the integration entirely.
  - New permission `ezflytime.teams.fly.bypass` (default `false`): exempts a player
    from the claimed-chunks-only restriction.
  - New message keys: `teams-no-fly-unclaimed`, `teams-fly-zone-left`,
    `teams-not-in-team` (all 8 locales).
- **Timed flight system** - players now have a flight-time balance instead of
  unlimited flight.  The remaining time counts down live and is shown in a
  boss bar above the screen.
- **Physical flight vouchers** - voucher items are backed by Persistent Data
  Container (PDC) metadata and carry a unique identifier, making each item
  traceable and preventing duplication exploits.
- **In-game voucher shop GUI** - players open the shop with `/flytime` and
  purchase vouchers directly using Vault economy currency.  The GUI is fully
  configurable in `voucher-gui.yml` (title, slots, items, sounds, prices).
- **Configurable commands on voucher buy / use** - add `on-buy-commands` and
  `on-use-commands` to any voucher in `config.yml` to run console or player
  commands automatically.  Supports placeholders: `{player}`, `{voucher}`,
  `{voucher_name}`, `{duration_seconds}`, `{amount}`.
- **Particle trail shop** - players can unlock and equip particle effects
  during flight.  The shop and selection GUI are configured in
  `particle-shop-gui.yml` and `particles.yml`.
- **Auto-flight reward system** - grant flight time automatically on a timer.
  Rewards can be flat, permission-based (rank groups), or tied to mcMMO skill
  level thresholds.
- **Fuel display mode** - optionally show remaining flight as a percentage
  fuel bar instead of a time value (`display.flytime-mode: fuel`).
- **MySQL storage backend** - store flight-time data in a MySQL database for
  multi-server / BungeeCord setups alongside the default YAML backend.
- **PlaceholderAPI expansion** - use `%ezflytime_time%`, `%ezflytime_seconds%`,
  `%ezflytime_minutes%`, and `%ezflytime_fuel%` in any PlaceholderAPI-compatible
  plugin.
- **Multilingual messages** - built-in translations for English, Dutch, Spanish,
  French, Russian, Turkish, Chinese, and German.  Set `language:` in
  `config.yml` to switch instantly.
- **Tab-complete permission gating** - `/flyvoucher` tab completions now respect
  permissions: `give` is only suggested to players with `ezflytime.give`, and
  `buy` only to players with `ezflytime.buy`.
- **`/flytime top`** leaderboard command (permission `ezflytime.top`,
  default: true).
- **Per-session flight cap** - limit how long a single uninterrupted flight
  session can last (`max-single-flight-seconds`).  A per-player bypass
  permission is available.
- **Flight preservation across death** - optionally keep or clear a player's
  remaining flight time when they die (`preserve-on-death`).
- **Creative / Spectator bypass** - players in Creative or Spectator mode can
  be configured to fly freely without consuming time.
- **SpigotMC update checker** - notifies online operators/admins when a new
  version is available.  Configurable and can be disabled entirely.
- **bStats metrics** - anonymous usage statistics to help guide future
  development (opt-out supported via bStats global config).
- **GitHub Pages documentation** at <https://ez-plugins.github.io/EzFlyTime/>.

### Changed

- `/flytime` with no arguments now opens the voucher shop GUI for all players
  who have the `ezflytime.buy` permission (default: true).  Previously only
  server operators saw the shop; regular players received only a "remaining
  time" message.
- `ezflytime.buy` is now declared in `plugin.yml` with `default: true`, making
  the shop accessible to all players out of the box.  Restrict it via your
  permission manager if needed.
- `/fly` and `/flytime` no longer operate independently of each other - both
  draw from and update the same flight-time balance.
- Admin time-management commands (`give`, `set`, `remove`) no longer send a
  duplicate notification to the target player.  The command handler sends one
  message; `FlyTimeManager` is told `notify=false`.

### Fixed

- Modrinth release workflow now lists `vaultunlocked` instead of `vault` as an optional
  dependency; Vault has no Modrinth listing whereas
  [VaultUnlocked](https://modrinth.com/plugin/vaultunlocked) is the actively maintained fork.
- `plugin.yml` now includes both `Vault` and `VaultUnlocked` in `softdepend` so EzFlyTime
  loads correctly regardless of which economy provider is installed.
- `/flyvoucher` tab completions are now gated on sender permissions: `give` is only suggested
  to players with `ezflytime.give` and `buy` only to players with `ezflytime.buy`.
- Players without `ezflytime.buy` could never open the voucher shop GUI because
  the permission was used in code but not registered in `plugin.yml`, causing
  it to silently default to OP-only.
- Tab-complete for `/flyvoucher` suggested `give` and `buy` to every sender
  regardless of what they were actually allowed to do.
- Right-clicking a voucher during the 1-second anti-spam cooldown did not
  cancel the event, allowing the click to propagate to the server.
- Granting, setting, or removing flight time via admin commands sent two
  notifications to the target player (one from the command, one from
  `FlyTimeManager`).
