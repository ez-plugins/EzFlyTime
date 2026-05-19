
package com.ezflytime;

import com.ezflytime.flight.FlyTimeManager;
import com.ezflytime.particles.ParticlesManager;
import com.ezflytime.voucher.VoucherManager;
import com.ezflytime.flight.autoreward.AutoFlightTimeDistributor;
import com.ezflytime.storage.FlyTimeStorage;
import com.ezflytime.storage.VoucherStorage;
import com.ezflytime.placeholder.PlaceholderIntegration;
import com.ezflytime.bootstrap.ServiceRegistry;
import com.ezflytime.bootstrap.StartupBootstrap;
import com.ezflytime.update.UpdateCheckResult;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


public class EzFlyTimePlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 27727;
    
    private ServiceRegistry serviceRegistry;
    private StartupBootstrap startupBootstrap;
    private Metrics metrics;
    
    /**
     * Returns true if fly time should be displayed as fuel (percentage), false for time.
     */
    public boolean isFuelModeEnabled() {
        String mode = getConfig().getString("display.flytime-mode", "time");
        return "fuel".equalsIgnoreCase(mode);
    }


    @Override
    public void onEnable() {
        // Delegate startup to bootstrap (config + economy initialisation moved there)
        this.serviceRegistry = new ServiceRegistry();
        this.startupBootstrap = new StartupBootstrap(this, serviceRegistry);
        this.startupBootstrap.start();
    }

    @Override
    public void onDisable() {
        if (startupBootstrap != null) {
            startupBootstrap.stop();
        } else if (serviceRegistry != null) {
            FlyTimeManager ftm = serviceRegistry.getFlyTimeManager();
            if (ftm != null) {
                ftm.stop();
                ftm.saveData();
            }
            ParticlesManager pm = serviceRegistry.getParticlesManager();
            if (pm != null) {
                pm.stop();
            }
            AutoFlightTimeDistributor afd = serviceRegistry.getAutoFlightTimeDistributor();
            if (afd != null) {
                afd.stop();
            }
            VoucherManager vm = serviceRegistry.getVoucherManager();
            if (vm != null) {
                vm.saveData();
            }
            FlyTimeStorage fts = serviceRegistry.getFlyTimeStorage();
            if (fts != null) {
                fts.close();
            }
            VoucherStorage vs = serviceRegistry.getVoucherStorage();
            if (vs != null) {
                vs.close();
            }
            PlaceholderIntegration pi = serviceRegistry.getPlaceholderIntegration();
            if (pi != null) {
                pi.unregister();
            }
        }
        metrics = null;
        if (serviceRegistry != null) {
            serviceRegistry.setUpdateChecker(null);
            serviceRegistry.setUpdateCheckResult(null);
        }
    }

    // Tiny getters removed: prefer direct access via ServiceRegistry inside the plugin

    // Server UUID available via getServiceRegistry().getServerUUID()

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public String getMessage(String path) {
        com.ezflytime.config.ConfigManager cm = serviceRegistry != null ? serviceRegistry.getConfigManager() : null;
        return cm != null ? cm.getMessage(path) : path;
    }

    public void reloadPluginConfiguration() {
        if (startupBootstrap != null) {
            startupBootstrap.reload();
            return;
        }
        // fallback: if bootstrap absent, nothing to do
    }
    public void logUpdateResult(UpdateCheckResult result) {
        if (result == null) {
            return;
        }
        switch (result.getStatus()) {
            case UPDATE_AVAILABLE:
                getLogger().warning("[EzFlyTime] " + stripForConsole(getMessage("messages.update-available")
                        .replace("{current}", result.getCurrentVersion())
                        .replace("{latest}", result.getLatestVersion())
                        .replace("{url}", result.getDownloadUrl())));
                break;
            case FAILED:
                getLogger().warning("[EzFlyTime] " + stripForConsole(getMessage("messages.update-check-failed")
                        .replace("{reason}", result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error")));
                break;
            case UP_TO_DATE:
            default:
                getLogger().info("[EzFlyTime] " + stripForConsole(getMessage("messages.update-up-to-date").replace("{version}", result.getCurrentVersion())));
                break;
        }
    }

    private String stripForConsole(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public boolean isBypassUnlimitedFlightEnabled() {
        com.ezflytime.config.ConfigManager cm = serviceRegistry != null ? serviceRegistry.getConfigManager() : null;
        return cm != null ? cm.isBypassUnlimitedFlightEnabled() : getConfig().getBoolean("flight.bypass-grants-unlimited", true);
    }

    public boolean hasBypassUnlimitedFlight(Player player) {
        com.ezflytime.config.ConfigManager cm = serviceRegistry != null ? serviceRegistry.getConfigManager() : null;
        return (cm != null ? cm.isBypassUnlimitedFlightEnabled() : getConfig().getBoolean("flight.bypass-grants-unlimited", true))
                && player.hasPermission("ezflytime.bypass");
    }

    public boolean isDebugEnabled() {
        com.ezflytime.config.ConfigManager cm = serviceRegistry != null ? serviceRegistry.getConfigManager() : null;
        return cm != null ? cm.isDebugEnabled() : false;
    }

    public void debug(String message) {
        com.ezflytime.config.ConfigManager cm = serviceRegistry != null ? serviceRegistry.getConfigManager() : null;
        if (cm != null) {
            cm.debug(message);
        }
    }

    /**
     * Returns true when the voucher/shop GUI should be available.
     */
    public boolean isVoucherShopEnabled() {
        com.ezflytime.config.ConfigManager cm = serviceRegistry != null ? serviceRegistry.getConfigManager() : null;
        return cm != null ? cm.isVoucherShopEnabled() : getConfig().getBoolean("voucher-shop.enabled", true);
    }
}
