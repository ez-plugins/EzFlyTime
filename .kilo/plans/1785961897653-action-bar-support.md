# Plan: Configurable Action Bar Support

## Goal
Add an optional action bar display alongside the existing boss bar, so server admins can show flight time via text above the hotbar instead of or in addition to the boss bar.

## Design Decision
Both displays run independently and can be enabled simultaneously. Each has its own config section, handler, and lifecycle. This preserves full backward compatibility (existing `bossbar.enabled: true` continues to work unchanged) and allows any combination.

## Files to Create
- `src/main/java/com/ezflytime/flight/FlightActionBarHandler.java` — mirrors `FlightBossBarHandler` structure

## Files to Modify
- `src/main/resources/config.yml` — add `actionbar:` section
- `src/main/java/com/ezflytime/config/ConfigManager.java` — add `isActionBarEnabled()`
- `src/main/java/com/ezflytime/bootstrap/StartupBootstrap.java` — log action bar status at startup
- `src/main/java/com/ezflytime/flight/FlyTimeManager.java` — wire action bar handler into lifecycle
- `src/main/java/com/ezflytime/EzFlyTimePlugin.java` — no changes needed (handler accessed via FlyTimeManager)

## Config Schema
```yaml
actionbar:
  enabled: false
  title: '&aFlight time remaining: {time}'
  speed-counter:
    enabled: false
    speed-format: '&bSpeed: {speed} b/s'
```

## FlightActionBarHandler Design
- Fields: `enabled`, `title`, `showSpeed`, `speedFormat`, `activePlayers` (Set<UUID>), `lastLocations` (Map<UUID, Location>), `lastLocationTimes` (Map<UUID, Long>)
- Lifecycle methods (same signatures as boss bar handler):
  - `markActive(player, remainingSeconds, bypass, unlimitedFlight)` — adds to `activePlayers` only if not bypass/unlimited
  - `markInactive(player)` — removes from `activePlayers`
  - `reset(uuid)` — removes from `activePlayers` and clears location tracking
  - `update(player, remainingSeconds)` — if player is in `activePlayers`, formats title and calls `player.sendActionBar(text)`
  - `clearAll()` — clears all internal maps/sets
  - `isAvailable()` — returns `enabled`
- Title formatting: supports `{time}`, `{seconds}`, `{fuel}`, `{speed}` placeholders (same logic as boss bar, but without progress-bar concept)
- Speed calculation: duplicate the horizontal-speed location-delta logic from `FlightBossBarHandler` (small, self-contained)
- No reflection needed: `Player.sendActionBar(String)` is available on Spigot API 1.9+; plugin compiles against 1.13.2

## FlyTimeManager Wiring
Add `private final FlightActionBarHandler actionBarHandler;` field and instantiate in constructor.

Mirror all 19 `bossBarHandler` call sites with `actionBarHandler` calls:

| Location | bossBarHandler call | actionBarHandler call |
|----------|-------------------|----------------------|
| constructor | `new FlightBossBarHandler(plugin)` | `new FlightActionBarHandler(plugin)` |
| `addTime` | `setInitialFlight(uuid, newTime)` | `setInitialFlight(uuid, newTime)` |
| `setTime` | `setInitialFlight(uuid, seconds)` | `setInitialFlight(uuid, seconds)` |
| `restoreState` | `setInitialFlight`, `markActive`, `markInactive`, `reset` | same calls |
| private `markActive` | `setInitialFlight`, `markActive` | same calls |
| private `markInactive` | `markInactive` | `markInactive` |
| countdownTask offline | `reset(uuid)` | `reset(uuid)` |
| countdownTask exhausted | `reset(uuid)` | `reset(uuid)` |
| countdownTask inactive | `update(...)` | `update(...)` |
| countdownTask unlimited | `update(...)` | `update(...)` |
| countdownTask decrement | `update(...)` | `update(...)` |
| `dispose` | `clearAll()` | `clearAll()` |

Note: action bar `update()` checks `activePlayers.contains(uuid)` before sending, so it naturally no-ops when the player was never marked active (e.g., bypass/unlimited flight), matching boss bar behavior.

## Startup Logging
Add to `StartupBootstrap`:
```java
boolean actionBarEnabled = cm != null ? cm.isActionBarEnabled() : false;
String actionBarStatus = actionBarEnabled ? "enabled" : "disabled";
plugin.getLogger().info("[EzFlyTime] ActionBar display: " + actionBarStatus);
```

## ConfigManager
Add:
```java
public boolean isActionBarEnabled() {
    org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("actionbar");
    return section != null && section.getBoolean("enabled", false);
}
```

## Edge Cases
- **Unlimited flight**: `markActive` with `bypass || unlimitedFlight` returns early, so player is not added to `activePlayers`. `update()` no-ops. Same behavior as boss bar.
- **Player goes offline**: `reset()` clears tracking; `update()` no-ops because player object is null in countdownTask.
- **Reload**: `dispose()` clears both handlers; FlyTimeManager re-creates them on next enable.
- **Spigot < 1.9**: `sendActionBar` doesn't exist, but plugin compiles against 1.13.2. No runtime fallback needed for action bar since minimum supported version is already past 1.9. (Boss bar retains its own reflection-based fallback for older servers.)

## Validation
1. Build with `mvn compile` — no compilation errors
2. Verify `config.yml` contains both `bossbar:` and new `actionbar:` sections
3. Verify `ConfigManager.isActionBarEnabled()` reads the new section
4. Verify startup log includes "ActionBar display: enabled/disabled"
5. Verify `FlyTimeManager` calls `actionBarHandler` at all 19 mirrored sites
6. Verify `dispose()` clears both handlers
