package com.ezflytime.flight;

import com.ezflytime.EzFlyTimePlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.ezflytime.util.TimeFormatter.formatCompact;

class FlightActionBarHandler {

    private final EzFlyTimePlugin plugin;
    private final Map<UUID, Integer> initialFlightSeconds = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastLocationTimes = new HashMap<>();
    private final Set<UUID> activePlayers = new java.util.HashSet<>();
    private final boolean enabled;
    private final String title;
    private final boolean showSpeed;
    private final String speedFormat;

    FlightActionBarHandler(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        org.bukkit.configuration.ConfigurationSection actionBarSection = plugin.getConfig().getConfigurationSection("actionbar");
        this.enabled = actionBarSection == null || actionBarSection.getBoolean("enabled", false);
        com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        boolean fuelMode = cm != null ? cm.isFuelModeEnabled() : "fuel".equalsIgnoreCase(plugin.getConfig().getString("display.flytime-mode", "time"));
        String defaultTitle = fuelMode
            ? "&aFlight fuel: {fuel}%"
            : "&aFlight time remaining: {time}";
        this.title = formatColors(actionBarSection != null ? actionBarSection.getString("title", defaultTitle) : defaultTitle);
        this.showSpeed = actionBarSection != null && actionBarSection.getConfigurationSection("speed-counter") != null
            && actionBarSection.getConfigurationSection("speed-counter").getBoolean("enabled", false);
        this.speedFormat = formatColors(actionBarSection != null ? actionBarSection.getString("speed-counter.speed-format", "&bSpeed: {speed} b/s") : "&bSpeed: {speed} b/s");
    }

    void setInitialFlight(UUID uuid, int seconds) {
        if (!isAvailable()) {
            return;
        }
        initialFlightSeconds.put(uuid, seconds);
    }

    void markActive(Player player, int remainingSeconds, boolean bypass, boolean unlimitedFlight) {
        if (!isAvailable()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (bypass || unlimitedFlight) {
            return;
        }
        initialFlightSeconds.put(uuid, Math.max(remainingSeconds, initialFlightSeconds.getOrDefault(uuid, 0)));
        activePlayers.add(uuid);
        plugin.debug(String.format("ActionBar: marking active for %s (%s) with %d seconds", player.getName(), uuid, remainingSeconds));
        update(player, remainingSeconds);
    }

    void markInactive(Player player) {
        if (!isAvailable()) {
            return;
        }
        activePlayers.remove(player.getUniqueId());
    }

    void reset(UUID uuid) {
        if (!isAvailable()) {
            return;
        }
        activePlayers.remove(uuid);
        initialFlightSeconds.remove(uuid);
        lastLocations.remove(uuid);
        lastLocationTimes.remove(uuid);
    }

    void update(Player player, int remainingSeconds) {
        if (!isAvailable()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!activePlayers.contains(uuid)) {
            return;
        }

        int initial = initialFlightSeconds.getOrDefault(uuid, remainingSeconds);
        if (initial <= 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formatTitle(player, remainingSeconds, 100)));
            return;
        }
        double progress = Math.max(0.0, Math.min(1.0, remainingSeconds / (double) initial));
        int percent = (int) Math.round(progress * 100);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formatTitle(player, remainingSeconds, percent)));
    }

    void clearAll() {
        if (!isAvailable()) {
            return;
        }
        activePlayers.clear();
        initialFlightSeconds.clear();
        lastLocations.clear();
        lastLocationTimes.clear();
        plugin.debug("ActionBar: cleared all action bars");
    }

    private boolean isAvailable() {
        return enabled;
    }

    private String formatTitle(Player player, int remainingSeconds, int percent) {
        String out = title
                .replace("{time}", formatCompact(remainingSeconds))
                .replace("{seconds}", String.valueOf(remainingSeconds))
                .replace("{fuel}", String.valueOf(percent));
        boolean needsSpeed = showSpeed || out.contains("{speed}");
        if (!needsSpeed) {
            return out;
        }

        double horizontalSpeed = 0.0;
        UUID uuid = player.getUniqueId();
        try {
            Location nowLoc = player.getLocation();
            long now = System.currentTimeMillis();
            Location lastLoc = lastLocations.get(uuid);
            Long lastTime = lastLocationTimes.get(uuid);
            if (lastLoc != null && lastTime != null && now > lastTime) {
                double dx = nowLoc.getX() - lastLoc.getX();
                double dz = nowLoc.getZ() - lastLoc.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                long dt = now - lastTime;
                if (dt > 0) {
                    horizontalSpeed = dist / (dt / 1000.0);
                }
            }
            lastLocations.put(uuid, nowLoc.clone());
            lastLocationTimes.put(uuid, now);
        } catch (Throwable t) {
            horizontalSpeed = 0.0;
        }

        String speedStr = String.format(Locale.ROOT, "%.1f", horizontalSpeed);
        String speedText = speedFormat.replace("{speed}", speedStr).replace("{time}", formatCompact(remainingSeconds));

        if (out.contains("{speed}")) {
            return out.replace("{speed}", speedStr);
        }
        return out + " " + speedText;
    }

    private String formatColors(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "§");
    }
}
