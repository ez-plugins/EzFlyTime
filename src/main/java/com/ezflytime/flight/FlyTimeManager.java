package com.ezflytime.flight;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.FlyTimeStorage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.ezflytime.util.FlightSupport;
import com.ezflytime.particles.ParticlesManager;

public class FlyTimeManager implements Listener {

    private final EzFlyTimePlugin plugin;
    private final Map<UUID, Integer> remainingSeconds = new HashMap<>();
    private final Map<UUID, Boolean> activelyFlying = new HashMap<>();
    // Tracks the remaining seconds at the start of the active flight session
    // Used to enforce per-session maximum continuous flight.
    private final Map<UUID, Integer> sessionStartRemaining = new HashMap<>();
    private final FlightBossBarHandler bossBarHandler;
    private final ParticlesManager particlesManager;
    private BukkitRunnable countdownTask;
    private BukkitRunnable autoSaveTask;
    private org.bukkit.scheduler.BukkitTask autoSaveBukkitTask;
    private final FlyTimeStorage storage;
    private final long autoSaveIntervalTicks;
    private final ActivationMode activationMode;
    private final boolean preserveFlightOnDeath;

    public FlyTimeManager(EzFlyTimePlugin plugin, FlyTimeStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        int autoSaveIntervalSeconds = Math.max(0, plugin.getConfig().getInt("auto-save.interval-seconds", 300));
        this.autoSaveIntervalTicks = autoSaveIntervalSeconds * 20L;
        this.bossBarHandler = new FlightBossBarHandler(plugin);
        this.particlesManager = plugin.getServiceRegistry().getParticlesManager();
        this.activationMode = parseActivationMode(plugin.getConfig().getString("flight.activation-mode", "NORMAL"));
        this.preserveFlightOnDeath = plugin.getConfig().getBoolean("flight.preserve-on-death", true);
        loadData();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void addTime(Player player, int seconds) {
        addTime(player, seconds, true);
    }

    public void addTime(Player player, int seconds, boolean notify) {
        UUID uuid = player.getUniqueId();
        int currentTime = remainingSeconds.getOrDefault(uuid, 0);
        debug("Granting %d seconds of flight time to %s (%s). Previous total: %d seconds.",
                seconds, player.getName(), uuid, currentTime);
        int newTime = currentTime + seconds;
        remainingSeconds.put(uuid, newTime);
        debug("Updated flight time for %s (%s): %d seconds total.",
                player.getName(), uuid, newTime);
        bossBarHandler.setInitialFlight(uuid, newTime);
        enableFlight(player);
        int minutes = seconds / 60;
        if (notify) {
            player.sendMessage(plugin.getMessage("messages.flight-added")
                    .replace("{minutes}", String.valueOf(minutes))
                    .replace("{seconds}", String.valueOf(seconds))
                    .replace("{total}", String.valueOf(newTime)));
        }
    }

    public void setTime(Player player, int seconds) {
        setTime(player, seconds, true);
    }

    public void setTime(Player player, int seconds, boolean notify) {
        UUID uuid = player.getUniqueId();
        int currentTime = remainingSeconds.getOrDefault(uuid, 0);
        debug("Setting flight time for %s (%s) to %d seconds. Previous total: %d seconds.",
                player.getName(), uuid, seconds, currentTime);
        remainingSeconds.put(uuid, Math.max(0, seconds));
        debug("Updated flight time for %s (%s): %d seconds total.",
                player.getName(), uuid, seconds);
        bossBarHandler.setInitialFlight(uuid, seconds);

        // If new time is 0, revoke flight regardless of whether player is actively flying
        if (seconds <= 0) {
            player.setFlying(false);
            player.setAllowFlight(false);
            if (activelyFlying.getOrDefault(uuid, false)) {
                activelyFlying.put(uuid, false);
                if (notify) {
                    player.sendMessage(plugin.getMessage("messages.flight-time-expired"));
                }
            }
        } else {
            enableFlight(player);
        }

        if (notify && seconds > 0) {
            player.sendMessage(plugin.getMessage("messages.flight-time-set")
                    .replace("{time}", String.valueOf(seconds)));
        }
    }

    public int getRemainingSeconds(Player player) {
        return getRemainingSeconds(player.getUniqueId());
    }

    public int getRemainingSeconds(UUID uuid) {
        return remainingSeconds.getOrDefault(uuid, 0);
    }

    public Map<UUID, Integer> snapshotRemainingSeconds() {
        return new HashMap<>(remainingSeconds);
    }

    public Map<UUID, Boolean> snapshotActiveStates() {
        return new HashMap<>(activelyFlying);
    }

    public void restoreState(Map<UUID, Integer> remaining, Map<UUID, Boolean> activeStates) {
        remainingSeconds.clear();
        remainingSeconds.putAll(remaining);
        activelyFlying.clear();
        activelyFlying.putAll(activeStates);

        for (Map.Entry<UUID, Integer> entry : remaining.entrySet()) {
            UUID uuid = entry.getKey();
            int seconds = entry.getValue();
            bossBarHandler.setInitialFlight(uuid, seconds);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (activelyFlying.getOrDefault(uuid, false) && isFlightEngaged(player)) {
                    com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
                    boolean bypass = cm != null && cm.hasBypassUnlimitedFlight(player);
                    bossBarHandler.markActive(player, seconds, bypass, hasUnlimitedFlight(player));
                } else {
                    bossBarHandler.markInactive(player);
                }
            } else {
                bossBarHandler.reset(uuid);
            }
        }
    }

    private void enableFlight(Player player) {
        UUID uuid = player.getUniqueId();
        debug("Enabling flight for %s (%s). Remaining: %d seconds.",
                player.getName(), uuid, remainingSeconds.getOrDefault(uuid, 0));
        player.setAllowFlight(true);
        if (isFlightEngaged(player)) {
            markActive(player);
        } else {
            markInactive(player);
        }
    }

    private void disableFlight(Player player) {
        if (!player.isOnline()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        debug("Disabling flight for %s (%s). Remaining: %d seconds.",
                player.getName(), uuid, remainingSeconds.getOrDefault(uuid, 0));
        com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        boolean bypass = cm != null && cm.hasBypassUnlimitedFlight(player);
        if (!bypass && !hasUnlimitedFlight(player)) {
            player.setFlying(false);
            player.setAllowFlight(false);
            if (activationMode == ActivationMode.DOUBLE_JUMP_ELYTRA && FlightSupport.supportsGliding()) {
                FlightSupport.setGliding(player, false);
            }
        }
        markInactive(player);
    }

    private boolean hasUnlimitedFlight(Player player) {
        com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        // Global bypass (config + permission) overrides mode checks
        if (cm != null && cm.hasBypassUnlimitedFlight(player)) {
            return true;
        }

        GameMode mode = player.getGameMode();
        // Creative
        if (mode == GameMode.CREATIVE) {
            // If config says creative grants unlimited, or player has granular permission, treat as unlimited
            if (cm == null) return true; // preserve default behavior if config manager unavailable
            if (cm.isCreativeModeTreatedAsUnlimited() || player.hasPermission("ezflytime.bypass.creative")) {
                return true;
            }
            return false;
        }

        // Spectator
        if (mode == GameMode.SPECTATOR) {
            if (cm == null) return true;
            if (cm.isSpectatorModeTreatedAsUnlimited() || player.hasPermission("ezflytime.bypass.spectator")) {
                return true;
            }
            return false;
        }

        return false;
    }

    public void pauseCountdown(Player player) {
        markInactive(player);
    }

    public void resumeCountdown(Player player) {
        if (isFlightEngaged(player)) {
            markActive(player);
        } else {
            markInactive(player);
        }
    }

    private void markActive(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasInactive = !activelyFlying.getOrDefault(uuid, false);
        activelyFlying.put(uuid, true);
        if (!remainingSeconds.containsKey(uuid)) {
            return;
        }
        com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        boolean bypassUnlimited = cm != null && cm.hasBypassUnlimitedFlight(player);
        boolean bypassMaxSingle = bypassUnlimited || hasMaxSingleFlightBypass(player);
        int currentRemaining = remainingSeconds.getOrDefault(uuid, 0);
        int maxSingle = cm != null ? cm.getMaxSingleFlightSeconds() : 0;

        // If a per-session max is configured and player doesn't bypass, record session start and
        // set the bossbar initial value to the session cap (min of currentRemaining and maxSingle).
        if (maxSingle > 0 && !bypassMaxSingle && !hasUnlimitedFlight(player)) {
            sessionStartRemaining.put(uuid, currentRemaining);
            int sessionCap = Math.min(currentRemaining, maxSingle);
            bossBarHandler.setInitialFlight(uuid, sessionCap);
            bossBarHandler.markActive(player, sessionCap, bypassUnlimited, hasUnlimitedFlight(player));
        } else {
            // No per-session limit or player bypasses it
            sessionStartRemaining.remove(uuid);
            bossBarHandler.setInitialFlight(uuid, currentRemaining);
            bossBarHandler.markActive(player, currentRemaining, bypassUnlimited, hasUnlimitedFlight(player));
        }

        // Play start flying sound if just started flying
        if (wasInactive && plugin.getServiceRegistry().getParticlesManager() != null) {
            plugin.getServiceRegistry().getParticlesManager().playStartFlyingSound(player);
        }
    }

    private void markInactive(Player player) {
        activelyFlying.put(player.getUniqueId(), false);
        bossBarHandler.markInactive(player);
        // Clear particle trails when stopping flight
        if (plugin.getServiceRegistry().getParticlesManager() != null) {
            plugin.getServiceRegistry().getParticlesManager().clearPlayerTrails(player.getUniqueId());
        }
    }

    public void start() {
        if (countdownTask != null) {
            return;
        }

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Integer> entry : new HashMap<>(remainingSeconds).entrySet()) {
                    UUID uuid = entry.getKey();
                    int seconds = entry.getValue();
                    Player player = Bukkit.getPlayer(uuid);

                    if (player == null) {
                        // Player is offline - clean up their state but don't log spam
                        activelyFlying.remove(uuid);
                        bossBarHandler.reset(uuid);
                        if (seconds <= 0) {
                            remainingSeconds.remove(uuid);
                        }
                        continue;
                    }

                    if (seconds <= 0) {
                        debug("Flight time exhausted for %s (%s). Removing countdown state.",
                                player.getName(), uuid);
                        remainingSeconds.remove(uuid);
                        activelyFlying.remove(uuid);
                        bossBarHandler.reset(uuid);
                        disableFlight(player);
                        player.sendMessage(plugin.getMessage("messages.flight-ended"));
                        continue;
                    }

                    boolean isActive = activelyFlying.getOrDefault(uuid, isFlightEngaged(player));
                    if (!isActive || !isFlightEngaged(player)) {
                        bossBarHandler.update(player, remainingSeconds.getOrDefault(uuid, seconds));
                        continue;
                    }

                    // If the player currently has unlimited flight (via config or granular permission),
                    // do not decrement their stored remaining seconds — keep the timer paused.
                    if (hasUnlimitedFlight(player)) {
                        bossBarHandler.update(player, remainingSeconds.getOrDefault(uuid, seconds));
                        continue;
                    }

                    int updatedSeconds = seconds - 1;
                    remainingSeconds.put(uuid, updatedSeconds);
                    // Update bossbar first
                    bossBarHandler.update(player, remainingSeconds.getOrDefault(uuid, 0));

                    // Enforce per-session maximum continuous flight if configured.
                    int sessionStart = sessionStartRemaining.getOrDefault(uuid, -1);
                    com.ezflytime.config.ConfigManager cm2 = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
                    int maxSingle2 = cm2 != null ? cm2.getMaxSingleFlightSeconds() : 0;
                    boolean bypassUnlimited = cm2 != null && cm2.hasBypassUnlimitedFlight(player);
                    boolean bypassMaxSingle = bypassUnlimited || hasMaxSingleFlightBypass(player);
                    if (sessionStart >= 0 && maxSingle2 > 0 && !bypassMaxSingle && !hasUnlimitedFlight(player)) {
                        int consumed = sessionStart - updatedSeconds;
                        if (consumed >= maxSingle2) {
                            // Reached the per-session cap: end flight but keep remaining seconds stored
                            sessionStartRemaining.remove(uuid);
                            disableFlight(player);
                            player.sendMessage(plugin.getMessage("messages.flight-session-max-reached"));
                            continue;
                        }
                    }
                }
            }
        };

        // Schedule the countdown via the BukkitRunnable API to avoid UnsupportedOperationException
        countdownTask.runTaskTimer(plugin, 20L, 20L);

        startAutoSaveTask();
    }

    public void stop() {
        if (countdownTask != null) {
            try {
                countdownTask.cancel();
            } catch (IllegalStateException ignored) {
            }
            countdownTask = null;
        }
        if (autoSaveTask != null) {
            try {
                if (autoSaveBukkitTask != null) {
                    autoSaveBukkitTask.cancel();
                } else {
                    autoSaveTask.cancel();
                }
            } catch (IllegalStateException ignored) {
            }
            autoSaveTask = null;
        }
    }

    /**
     * Dispose of this manager: stop tasks, clear boss bars and unregister events.
     * This is intended to be called during plugin reload to ensure no duplicate
     * boss bars or lingering tasks remain.
     */
    public void dispose() {
        stop();
        try {
            bossBarHandler.clearAll();
        } catch (Exception ignored) {
        }
        HandlerList.unregisterAll(this);
    }

    private void startAutoSaveTask() {
        if (autoSaveIntervalTicks <= 0 || autoSaveTask != null) {
            return;
        }

        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveData();
            }
        };
        // Use the BukkitRunnable scheduling helper to obtain a BukkitTask and schedule safely
        autoSaveBukkitTask = autoSaveTask.runTaskTimer(plugin, autoSaveIntervalTicks, autoSaveIntervalTicks);
    }

    private void loadData() {
        debug("Loading stored flight time data from storage.");
        Map<UUID, Integer> loaded = storage.loadFlyTimes();
        debug("Loaded %d flight time entries from storage.", loaded.size());
        loaded.forEach((uuid, seconds) -> debug(" - %s has %d seconds stored.", uuid, seconds));
        remainingSeconds.putAll(loaded);
    }

    public void saveData() {
        debug("Saving %d flight time entries to storage.", remainingSeconds.size());
        remainingSeconds.forEach((uuid, seconds) -> debug(" - %s retaining %d seconds.", uuid, seconds));
        storage.saveFlyTimes(remainingSeconds);
        debug("Flight time data save complete.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (getRemainingSeconds(player) > 0) {
            enableFlight(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Optionally preserve flight across death: mirror join behavior when enabled
        if (preserveFlightOnDeath && getRemainingSeconds(player) > 0) {
            // Use scheduler to ensure respawn state is fully applied before enabling flight
            Bukkit.getScheduler().runTask(plugin, () -> enableFlight(player));
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!remainingSeconds.containsKey(uuid)) {
            return;
        }

        if (activationMode == ActivationMode.DOUBLE_JUMP_ELYTRA && event.isFlying()) {
            if (shouldTriggerElytra(player)) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!FlightSupport.isGliding(player)) {
                        FlightSupport.setGliding(player, true);
                    }
                    markActive(player);
                });
                return;
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (isFlightEngaged(player)) {
                markActive(player);
            } else {
                markInactive(player);
            }
        });
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!remainingSeconds.containsKey(uuid)) {
            return;
        }

        boolean currentlyActive = activelyFlying.getOrDefault(uuid, false);
        boolean isFlying = isFlightEngaged(player);

        if (isFlying && !currentlyActive) {
            markActive(player);
        } else if (!isFlying && currentlyActive && player.isOnGround()) {
            markInactive(player);
        }

        // Spawn particles when actively flying
        if (currentlyActive && isFlying) {
            particlesManager.spawnParticles(player);
        }
    }

    private ActivationMode parseActivationMode(String value) {
        if (value == null) {
            return ActivationMode.NORMAL;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            ActivationMode mode = ActivationMode.valueOf(normalized);
            if (mode == ActivationMode.DOUBLE_JUMP_ELYTRA && !FlightSupport.supportsElytra()) {
                plugin.getLogger().warning("Elytra activation mode is not supported on this server. Using NORMAL.");
                return ActivationMode.NORMAL;
            }
            return mode;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown flight activation mode '" + value + "'. Using NORMAL.");
            return ActivationMode.NORMAL;
        }
    }

    private boolean isFlightEngaged(Player player) {
        if (activationMode == ActivationMode.DOUBLE_JUMP_ELYTRA) {
            return player.isFlying() || FlightSupport.isGliding(player);
        }
        return player.isFlying();
    }

    private boolean shouldTriggerElytra(Player player) {
        if (!FlightSupport.supportsGliding()) {
            return false;
        }
        if (!player.isOnGround() && FlightSupport.isGliding(player)) {
            return false;
        }
        if (!isElytraEquipped(player)) {
            return false;
        }
        return !player.isFlying();
    }

    private boolean isElytraEquipped(Player player) {
        if (player.getInventory().getChestplate() == null) {
            return false;
        }
        Material elytra = Material.getMaterial("ELYTRA");
        return elytra != null && player.getInventory().getChestplate().getType() == elytra;
    }

    private void debug(String pattern, Object... args) {
        com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        if (cm == null || !cm.isDebugEnabled()) return;
        if (args.length == 0) {
            cm.debug(pattern);
        } else {
            cm.debug(String.format(pattern, args));
        }
    }

    public boolean hasMaxSingleFlightBypass(Player player) {
        if (player.hasPermission("ezflytime.maxsingle.bypass")) {
            return true;
        }
        Set<UUID> bypassStore = getMaxSingleBypassStore();
        return bypassStore != null && bypassStore.contains(player.getUniqueId());
    }

    public boolean setMaxSingleFlightBypass(UUID uuid, boolean enabled) {
        Set<UUID> bypassStore = getMaxSingleBypassStore();
        if (bypassStore == null) {
            return false;
        }
        if (enabled) {
            bypassStore.add(uuid);
        } else {
            bypassStore.remove(uuid);
        }
        return enabled;
    }

    public boolean toggleMaxSingleFlightBypass(UUID uuid) {
        Set<UUID> bypassStore = getMaxSingleBypassStore();
        if (bypassStore == null) {
            return false;
        }
        if (bypassStore.contains(uuid)) {
            bypassStore.remove(uuid);
            return false;
        }
        bypassStore.add(uuid);
        return true;
    }

    private Set<UUID> getMaxSingleBypassStore() {
        com.ezflytime.bootstrap.ServiceRegistry registry = plugin.getServiceRegistry();
        return registry != null ? registry.getMaxSingleBypassPlayers() : null;
    }

    private enum ActivationMode {
        NORMAL,
        DOUBLE_JUMP_ELYTRA
    }
}
