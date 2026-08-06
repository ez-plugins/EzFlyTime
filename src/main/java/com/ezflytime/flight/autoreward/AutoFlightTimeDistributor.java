package com.ezflytime.flight.autoreward;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.config.ConfigManager;
import com.ezflytime.flight.FlyTimeManager;
import com.ezflytime.util.TimeFormatter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import com.ezflytime.util.OnlinePlayers;

public class AutoFlightTimeDistributor {

    private final EzFlyTimePlugin plugin;
    private final FlyTimeManager flyTimeManager;
    private McMMOIntegration mcMMOIntegration;

    private AutoRewardSettings settings;
    private BukkitTask task;
    private boolean loggedMissingMcMMO;

    public AutoFlightTimeDistributor(EzFlyTimePlugin plugin, FlyTimeManager flyTimeManager) {
        this.plugin = plugin;
        this.flyTimeManager = flyTimeManager;
        // Only construct mcMMO integration when enabled in mcmmo.yml (or when no dedicated config exists).
        com.ezflytime.mcmmo.McMMOConfig mmc = null;
        try {
            com.ezflytime.bootstrap.ServiceRegistry sr = plugin.getServiceRegistry();
            com.ezflytime.config.ConfigManager cm = sr != null ? sr.getConfigManager() : null;
            mmc = cm != null ? cm.getMcMMOConfig() : null;
        } catch (Throwable ignored) {
        }

        if (mmc == null || mmc.isEnabled()) {
            this.mcMMOIntegration = new McMMOIntegration(plugin);
        } else {
            this.mcMMOIntegration = null;
        }

        this.settings = AutoRewardSettings.fromConfiguration(plugin);
    }

    public void start() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (!settings.isEnabled()) {
            return;
        }

        long intervalTicks = settings.getIntervalSeconds() * 20L;
        if (intervalTicks <= 0) {
            plugin.getLogger().warning("Auto flight rewards interval must be greater than zero.");
            return;
        }

        if (mcMMOIntegration != null) {
            mcMMOIntegration.refresh();
        }
        loggedMissingMcMMO = false;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runDistribution, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reload() {
        stop();
        // Re-evaluate mcMMO config on reload: recreate or dispose integration depending on setting
        com.ezflytime.mcmmo.McMMOConfig mmc = null;
        try {
            com.ezflytime.bootstrap.ServiceRegistry sr = plugin.getServiceRegistry();
            com.ezflytime.config.ConfigManager cm = sr != null ? sr.getConfigManager() : null;
            mmc = cm != null ? cm.getMcMMOConfig() : null;
        } catch (Throwable ignored) {
        }

        if (mmc == null || mmc.isEnabled()) {
            if (this.mcMMOIntegration == null) {
                this.mcMMOIntegration = new McMMOIntegration(plugin);
            } else {
                this.mcMMOIntegration.refresh();
            }
        } else {
            this.mcMMOIntegration = null;
        }

        this.settings = AutoRewardSettings.fromConfiguration(plugin);
        start();
    }

    private void runDistribution() {
        if (settings.hasSkillRewards() && (mcMMOIntegration == null || !mcMMOIntegration.isAvailable()) && !loggedMissingMcMMO) {
            plugin.getLogger().warning("mcMMO skill rewards are configured but mcMMO is not available.");
            loggedMissingMcMMO = true;
        }

        for (Player player : OnlinePlayers.getOnlinePlayers()) {
            int totalSeconds = settings.getBaseSeconds();
            totalSeconds += settings.rankReward(player);
            totalSeconds += settings.skillReward(player, mcMMOIntegration);

            if (totalSeconds <= 0) {
                continue;
            }

            flyTimeManager.addTime(player, totalSeconds, false);
            if (settings.shouldNotifyPlayers()) {
                ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
                String fmt = cm != null ? cm.getTimeFormat() : "compact";
                player.sendMessage(plugin.getMessage("messages.auto-flight-reward")
                        .replace("{seconds}", String.valueOf(totalSeconds))
                        .replace("{minutes}", String.valueOf(totalSeconds / 60))
                        .replace("{time}", TimeFormatter.format(totalSeconds, fmt)));
            }
        }
    }
}
