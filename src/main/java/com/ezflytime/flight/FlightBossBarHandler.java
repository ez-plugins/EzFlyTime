package com.ezflytime.flight;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.config.ConfigManager;
import com.ezflytime.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

class FlightBossBarHandler {

    private final EzFlyTimePlugin plugin;
    private final Map<UUID, Object> bossBars = new HashMap<>();
    private final Map<UUID, Integer> initialFlightSeconds = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastLocationTimes = new HashMap<>();
    private final boolean enabled;
    private final String title;
    private final boolean showSpeed;
    private final String speedFormat;
    private final String timeFormat;
    private final BossBarAdapter bossBarAdapter;

    FlightBossBarHandler(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        org.bukkit.configuration.ConfigurationSection bossBarSection = plugin.getConfig().getConfigurationSection("bossbar");
        this.enabled = bossBarSection == null || bossBarSection.getBoolean("enabled", true);
        com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        boolean fuelMode = cm != null ? cm.isFuelModeEnabled() : "fuel".equalsIgnoreCase(plugin.getConfig().getString("display.flytime-mode", "time"));
        String defaultTitle = fuelMode
            ? "&aFlight fuel: {fuel}%"
            : "&aFlight time remaining: {time}";
        this.title = formatColors(bossBarSection != null ? bossBarSection.getString("title", defaultTitle) : defaultTitle);
        String color = bossBarSection != null ? bossBarSection.getString("color", "GREEN") : "GREEN";
        String style = bossBarSection != null ? bossBarSection.getString("style", "SOLID") : "SOLID";
        this.showSpeed = bossBarSection != null && bossBarSection.getConfigurationSection("speed-counter") != null
            && bossBarSection.getConfigurationSection("speed-counter").getBoolean("enabled", false);
        this.speedFormat = bossBarSection != null ? bossBarSection.getString("speed-counter.speed-format", "&bSpeed: {speed} b/s") : "&bSpeed: {speed} b/s";
        this.timeFormat = cm != null ? cm.getTimeFormat() : "compact";
        this.bossBarAdapter = BossBarAdapter.create(plugin, color, style);
        if (enabled && bossBarAdapter == null) {
            plugin.getLogger().warning("BossBar is enabled in config, but this server does not support it.");
        }
    }

    static boolean isBossBarSupported() {
        return BossBarAdapter.isSupported();
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
            hide(uuid);
            return;
        }
        initialFlightSeconds.put(uuid, Math.max(remainingSeconds, initialFlightSeconds.getOrDefault(uuid, 0)));
        plugin.debug(String.format("BossBar: marking active for %s (%s) with %d seconds", player.getName(), uuid, remainingSeconds));
        show(player);
        update(player, remainingSeconds);
    }

    void markInactive(Player player) {
        if (!isAvailable()) {
            return;
        }
        hide(player.getUniqueId());
    }

    void reset(UUID uuid) {
        if (!isAvailable()) {
            return;
        }
        hide(uuid);
        initialFlightSeconds.remove(uuid);
    }

    void update(Player player, int remainingSeconds) {
        if (!isAvailable()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Object bossBar = bossBars.get(uuid);
        if (bossBar == null) {
            return;
        }

        int initial = initialFlightSeconds.getOrDefault(uuid, remainingSeconds);
        if (initial <= 0) {
            bossBarAdapter.setProgress(bossBar, 1.0);
            bossBarAdapter.setTitle(bossBar, formatTitle(player, remainingSeconds, 100));
            return;
        }
        double progress = Math.max(0.0, Math.min(1.0, remainingSeconds / (double) initial));
        int percent = (int) Math.round(progress * 100);
        bossBarAdapter.setProgress(bossBar, progress);
        bossBarAdapter.setTitle(bossBar, formatTitle(player, remainingSeconds, percent));
    }

    private void show(Player player) {
        if (bossBarAdapter == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Object bossBar = bossBars.get(uuid);
        if (bossBar == null) {
            bossBar = bossBarAdapter.createBossBar("");
            if (bossBar == null) {
                return;
            }
            bossBars.put(uuid, bossBar);
            plugin.debug(String.format("BossBar: created bossbar for %s (%s)", player.getName(), uuid));
        }
        List<?> players = bossBarAdapter.getPlayers(bossBar);
        if (players != null && players.contains(player)) {
            return;
        }
        plugin.debug(String.format("BossBar: adding player %s to bossbar (%s)", player.getName(), uuid));
        bossBarAdapter.addPlayer(bossBar, player);
    }

    private void hide(UUID uuid) {
        Object bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            bossBarAdapter.removeAll(bossBar);
        }
    }

    /**
     * Remove all boss bars managed by this handler and clear internal state.
     */
    void clearAll() {
        if (bossBarAdapter == null) return;
        for (Object bossBar : bossBars.values()) {
            try {
                bossBarAdapter.removeAll(bossBar);
            } catch (Exception ignored) {
            }
        }
        bossBars.clear();
        initialFlightSeconds.clear();
        plugin.debug("BossBar: cleared all bossbars");
    }

    private boolean isAvailable() {
        return enabled && bossBarAdapter != null;
    }

    private String formatTitle(Player player, int remainingSeconds, int percent) {
        String out = title
                .replace("{time}", TimeFormatter.format(remainingSeconds, timeFormat))
                .replace("{seconds}", String.valueOf(remainingSeconds))
                .replace("{fuel}", String.valueOf(percent));
        boolean needsSpeed = showSpeed || out.contains("{speed}");
        if (!needsSpeed) return out;

        // compute horizontal speed in blocks/second using location delta (more reliable while flying)
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
                long dt = now - lastTime; // ms
                if (dt > 0) {
                    horizontalSpeed = dist / (dt / 1000.0); // blocks per second
                }
            }
            lastLocations.put(uuid, nowLoc.clone());
            lastLocationTimes.put(uuid, now);
        } catch (Throwable t) {
            horizontalSpeed = 0.0;
        }

        String speedStr = String.format(Locale.ROOT, "%.1f", horizontalSpeed);
        String speedText = speedFormat.replace("{speed}", speedStr).replace("{time}", TimeFormatter.format(remainingSeconds, timeFormat));

        // If the title already contains a {speed} placeholder, replace it; otherwise append the speed text.
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

    private static final class BossBarAdapter {
        private final Method createBossBar;
        private final Method addPlayer;
        private final Method removeAll;
        private final Method setProgress;
        private final Method setTitle;
        private final Method getPlayers;
        private final Object barColor;
        private final Object barStyle;
        private final Object barFlagArray;

        private BossBarAdapter(Method createBossBar, Method addPlayer, Method removeAll,
                               Method setProgress, Method setTitle, Method getPlayers,
                               Object barColor, Object barStyle, Object barFlagArray) {
            this.createBossBar = createBossBar;
            this.addPlayer = addPlayer;
            this.removeAll = removeAll;
            this.setProgress = setProgress;
            this.setTitle = setTitle;
            this.getPlayers = getPlayers;
            this.barColor = barColor;
            this.barStyle = barStyle;
            this.barFlagArray = barFlagArray;
        }

        static boolean isSupported() {
            try {
                Class.forName("org.bukkit.boss.BossBar");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }

        static BossBarAdapter create(EzFlyTimePlugin plugin, String colorName, String styleName) {
            try {
                Class<?> bossBarClass = Class.forName("org.bukkit.boss.BossBar");
                Class<?> barColorClass = Class.forName("org.bukkit.boss.BarColor");
                Class<?> barStyleClass = Class.forName("org.bukkit.boss.BarStyle");

                Object color = parseEnum(barColorClass, colorName, "GREEN");
                Object style = parseEnum(barStyleClass, styleName, "SOLID");

                Method createBossBar = resolveCreateBossBarMethod(barColorClass, barStyleClass);
                Method addPlayer = bossBarClass.getMethod("addPlayer", Player.class);
                Method removeAll = bossBarClass.getMethod("removeAll");
                Method setProgress = bossBarClass.getMethod("setProgress", double.class);
                Method setTitle = bossBarClass.getMethod("setTitle", String.class);
                Method getPlayers = bossBarClass.getMethod("getPlayers");

                Object barFlagArray = createBossBar.getParameterCount() == 4
                        ? java.lang.reflect.Array.newInstance(Class.forName("org.bukkit.boss.BarFlag"), 0)
                        : null;
                return new BossBarAdapter(createBossBar, addPlayer, removeAll, setProgress, setTitle, getPlayers, color, style, barFlagArray);
            } catch (Exception ex) {
                if (plugin != null) {
                    plugin.getLogger().warning("Unable to initialize BossBar support: " + ex.getMessage());
                }
                return null;
            }
        }

        private static Method resolveCreateBossBarMethod(Class<?> barColorClass, Class<?> barStyleClass)
                throws ClassNotFoundException, NoSuchMethodException {
            try {
                Class<?> barFlagClass = Class.forName("org.bukkit.boss.BarFlag");
                Class<?> barFlagArrayClass = java.lang.reflect.Array.newInstance(barFlagClass, 0).getClass();
                return Bukkit.class.getMethod("createBossBar", String.class, barColorClass, barStyleClass, barFlagArrayClass);
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                return Bukkit.class.getMethod("createBossBar", String.class, barColorClass, barStyleClass);
            }
        }

        Object createBossBar(String title) {
            try {
                if (createBossBar.getParameterCount() == 4) {
                    return createBossBar.invoke(null, title, barColor, barStyle, barFlagArray);
                }
                return createBossBar.invoke(null, title, barColor, barStyle);
            } catch (Exception ignored) {
                return null;
            }
        }

        void addPlayer(Object bossBar, Player player) {
            try {
                addPlayer.invoke(bossBar, player);
            } catch (Exception ignored) {
            }
        }

        void removeAll(Object bossBar) {
            try {
                removeAll.invoke(bossBar);
            } catch (Exception ignored) {
            }
        }

        void setProgress(Object bossBar, double progress) {
            try {
                setProgress.invoke(bossBar, progress);
            } catch (Exception ignored) {
            }
        }

        void setTitle(Object bossBar, String title) {
            try {
                setTitle.invoke(bossBar, title);
            } catch (Exception ignored) {
            }
        }

        List<?> getPlayers(Object bossBar) {
            try {
                return (List<?>) getPlayers.invoke(bossBar);
            } catch (Exception ignored) {
                return null;
            }
        }

        private static Object parseEnum(Class<?> enumClass, String value, String fallback) {
            String normalized = value == null ? fallback : value.trim().toUpperCase(Locale.ROOT);
            try {
                return Enum.valueOf((Class<Enum>) enumClass, normalized);
            } catch (IllegalArgumentException ex) {
                return Enum.valueOf((Class<Enum>) enumClass, fallback);
            }
        }
    }
}
