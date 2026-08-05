package com.ezflytime.bootstrap;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.ezflytime.bootstrap.ServiceRegistry;
import com.ezflytime.bootstrap.StorageInitializer;
import com.ezflytime.bootstrap.CommandRegistrar;
import com.ezflytime.flight.FlyTimeManager;
import com.ezflytime.flight.autoreward.AutoFlightTimeDistributor;
import com.ezflytime.particles.ParticlesManager;
import com.ezflytime.placeholder.PlaceholderIntegration;
import com.ezflytime.voucher.VoucherManager;
import com.ezflytime.gui.VoucherGUI;
import com.ezflytime.util.ServerUUID;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import com.ezflytime.update.SpigotUpdateChecker;
import com.ezflytime.update.UpdateCheckResult;
import com.ezflytime.update.UpdateNotifier;
import java.util.Locale;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import com.ezflytime.storage.FlyTimeStorage;

public class StartupBootstrap {
    private final EzFlyTimePlugin plugin;
    private final ServiceRegistry registry;

    public StartupBootstrap(EzFlyTimePlugin plugin, ServiceRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start() {
        // Initialize configuration manager and register it
        ConfigManager configManager = new ConfigManager(plugin);
        configManager.init();
        registry.setConfigManager(configManager);

        // Setup economy via Vault and register provider in registry
        try {
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
                plugin.getLogger().warning("Vault not found. Voucher buying will be disabled.");
            } else {
                RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
                if (rsp == null) {
                    plugin.getLogger().warning("No Vault economy provider found. Voucher buying will be disabled.");
                } else {
                    registry.setEconomy(rsp.getProvider());
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Error initialising economy provider: " + t.getMessage());
        }
        // Initialize storage backends
        StorageInitializer.StoragePair storagePair = new StorageInitializer(plugin).initialize();
        registry.setFlyTimeStorage(storagePair.getFlyTimeStorage());
        registry.setVoucherStorage(storagePair.getVoucherStorage());
        registry.setUnlockedParticlesStorage(storagePair.getUnlockedParticlesStorage());

        // Server UUID
        ServerUUID serverUUID = new ServerUUID(plugin);
        registry.setServerUUID(serverUUID);

        // Managers
        VoucherManager voucherManager = new VoucherManager(plugin, registry.getVoucherStorage());
        registry.setVoucherManager(voucherManager);

        ParticlesManager particlesManager = new ParticlesManager(plugin);
        registry.setParticlesManager(particlesManager);

        com.ezflytime.particles.UnlockedParticlesManager unlockedManager = new com.ezflytime.particles.UnlockedParticlesManager(plugin, registry.getUnlockedParticlesStorage());
        registry.setUnlockedParticlesManager(unlockedManager);

        FlyTimeManager flyTimeManager = new FlyTimeManager(plugin, registry.getFlyTimeStorage());
        registry.setFlyTimeManager(flyTimeManager);

        if (registry.getConfigManager() != null && registry.getConfigManager().isVoucherShopEnabled()) {
            registry.getConfigManager().saveDefaultVoucherGUI();
            VoucherGUI voucherGUI = new VoucherGUI(plugin);
            registry.setVoucherGUI(voucherGUI);
        }

        // Particle shop GUI (optional)
        if (plugin.getConfig().getBoolean("particle-shop.enabled", true)) {
            com.ezflytime.gui.ParticleShopGUI particleShopGUI = new com.ezflytime.gui.ParticleShopGUI(plugin);
            com.ezflytime.gui.ParticleSelectGUI particleSelectGUI = new com.ezflytime.gui.ParticleSelectGUI(plugin);
            registry.setParticleShopGUI(particleShopGUI);
            registry.setParticleSelectGUI(particleSelectGUI);
        }

        AutoFlightTimeDistributor autoFlightTimeDistributor = new AutoFlightTimeDistributor(plugin, flyTimeManager);
        registry.setAutoFlightTimeDistributor(autoFlightTimeDistributor);

        // Commands and placeholders
        new CommandRegistrar(plugin).register(voucherManager);

        flyTimeManager.start();
        autoFlightTimeDistributor.start();

        PlaceholderIntegration placeholderIntegration = new PlaceholderIntegration(plugin);
        placeholderIntegration.registerIfAvailable();
        registry.setPlaceholderIntegration(placeholderIntegration);

        // TeamsIntegration (and transitively FlySubcommand / TeamsSubcommand) must
        // never be class-loaded when TeamsAPI is absent: bytecode verification of
        // TeamsIntegration's methods triggers FlySubcommand loading, which requires
        // TeamsSubcommand, before any runtime guard inside the class can fire.
        if (plugin.getServer().getPluginManager().getPlugin("TeamsAPI") != null) {
            try {
                com.ezflytime.teams.TeamsIntegration teamsIntegration = new com.ezflytime.teams.TeamsIntegration(plugin);
                teamsIntegration.registerIfAvailable();
                registry.setTeamsIntegration(teamsIntegration);
            } catch (Throwable t) {
                plugin.getLogger().warning("TeamsAPI integration failed to initialise (incompatible API?): " + t.getMessage());
            }
        } else {
            plugin.getLogger().info("TeamsAPI not found. Team subcommand and claimed-chunks restriction disabled.");
        }

        // Initialize bStats metrics via dedicated manager
        try {
            com.ezflytime.bootstrap.bstats.BStatsManager bStatsManager = new com.ezflytime.bootstrap.bstats.BStatsManager(plugin);
            bStatsManager.start();
        } catch (Throwable t) {
            plugin.getLogger().warning("Unable to initialise bStats metrics: " + t.getMessage());
        }

        // Initialize update checker
        if (plugin.getConfig().getBoolean("updates.enabled", true)) {
            int resourceId = plugin.getConfig().getInt("updates.resource-id", 129745);
            SpigotUpdateChecker updateChecker = new SpigotUpdateChecker(plugin, resourceId);
            registry.setUpdateChecker(updateChecker);
            if (plugin.getConfig().getBoolean("updates.notify-on-join", true)) {
                plugin.getServer().getPluginManager().registerEvents(new UpdateNotifier(plugin), plugin);
            }
            updateChecker.checkAsync(result -> {
                registry.setUpdateCheckResult(result);
                plugin.logUpdateResult(result);
            });
        } else {
            plugin.getLogger().info("Update checks are disabled.");
        }

        // Log startup summary
        logStartupSummary();
    }

    private void logStartupSummary() {
        String version = plugin.getDescription().getVersion();
        plugin.getLogger().info("[EzFlyTime] Starting EzFlyTime v" + version + ".");
        plugin.getLogger().info("[EzFlyTime] Server: " + plugin.getServer().getName() + " " + plugin.getServer().getVersion());
        com.ezflytime.config.ConfigManager cm = registry.getConfigManager();
        String storage = cm != null ? cm.resolveStorageBackend() : "yaml";
        plugin.getLogger().info("[EzFlyTime] Storage backend: " + storage);
        String autoFlight = cm != null ? cm.resolveAutoFlightRewardsStatus() : "disabled";
        plugin.getLogger().info("[EzFlyTime] Auto-flight rewards: " + autoFlight);
        boolean bossBarEnabled = cm != null ? cm.isBossBarEnabled() : true;
        String bossBarStatus = bossBarEnabled ? "enabled" : "disabled";
        if (bossBarEnabled && !com.ezflytime.util.BossBarSupport.isSupported()) {
            bossBarStatus += " (unsupported on this server)";
        }
        plugin.getLogger().info("[EzFlyTime] BossBar display: " + bossBarStatus);
        boolean actionBarEnabled = cm != null ? cm.isActionBarEnabled() : false;
        String actionBarStatus = actionBarEnabled ? "enabled" : "disabled";
        plugin.getLogger().info("[EzFlyTime] ActionBar display: " + actionBarStatus);
        plugin.getLogger().info("[EzFlyTime] Update checks: " + (plugin.getConfig().getBoolean("updates.enabled", true) ? "enabled" : "disabled"));
    }

    public void stop() {
        // Stop managers and close storages
        if (registry.getFlyTimeManager() != null) {
            registry.getFlyTimeManager().stop();
            registry.getFlyTimeManager().saveData();
        }
        if (registry.getParticlesManager() != null) {
            registry.getParticlesManager().stop();
        }
        if (registry.getAutoFlightTimeDistributor() != null) {
            registry.getAutoFlightTimeDistributor().stop();
        }
        if (registry.getVoucherManager() != null) {
            registry.getVoucherManager().saveData();
        }
        if (registry.getFlyTimeStorage() != null) {
            registry.getFlyTimeStorage().close();
        }
        if (registry.getVoucherStorage() != null) {
            registry.getVoucherStorage().close();
        }
        if (registry.getPlaceholderIntegration() != null) {
            registry.getPlaceholderIntegration().unregister();
        }
        if (registry.getTeamsIntegration() != null) {
            registry.getTeamsIntegration().unregister();
        }
    }

    /**
     * Reload runtime configuration and recreate managers that depend on it.
     */
    public void reload() {
        com.ezflytime.config.ConfigManager cm = registry.getConfigManager();
        if (cm != null) {
            cm.reload();
        } else {
            plugin.reloadConfig();
        }

        ParticlesManager pm = registry.getParticlesManager();
        if (pm != null) {
            pm.reloadConfig();
        }

        VoucherGUI gui = registry.getVoucherGUI();
        if (gui != null) {
            gui.reload();
        } else if (cm != null && cm.isVoucherShopEnabled()) {
            cm.saveDefaultVoucherGUI();
            VoucherGUI newGui = new VoucherGUI(plugin);
            registry.setVoucherGUI(newGui);
        }

        FlyTimeManager ftm = registry.getFlyTimeManager();
        if (ftm != null && registry != null) {
            Map<UUID, Integer> remaining = ftm.snapshotRemainingSeconds();
            Map<UUID, Boolean> activeStates = ftm.snapshotActiveStates();
            ftm.saveData();
            ftm.dispose();
            FlyTimeStorage fts = registry.getFlyTimeStorage();
            FlyTimeManager newFtm = new FlyTimeManager(plugin, fts);
            newFtm.restoreState(remaining, activeStates);
            newFtm.start();
            registry.setFlyTimeManager(newFtm);

            AutoFlightTimeDistributor afd = registry.getAutoFlightTimeDistributor();
            if (afd != null) {
                afd.stop();
            }
            AutoFlightTimeDistributor newAfd = new AutoFlightTimeDistributor(plugin, newFtm);
            newAfd.start();
            registry.setAutoFlightTimeDistributor(newAfd);

            for (UUID uuid : remaining.keySet()) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player == null) continue;
                if (activeStates.getOrDefault(uuid, false)) {
                    newFtm.resumeCountdown(player);
                } else {
                    newFtm.pauseCountdown(player);
                }
            }
        }

        VoucherManager vm = registry.getVoucherManager();
        if (vm != null) vm.reload();

        PlaceholderIntegration pi = registry.getPlaceholderIntegration();
        if (pi != null) {
            pi.unregister();
            pi.registerIfAvailable();
        }

        if (registry == null || registry.getFlyTimeManager() == null) {
            AutoFlightTimeDistributor afd = registry != null ? registry.getAutoFlightTimeDistributor() : null;
            if (afd != null) afd.reload();
        }
    }
}
