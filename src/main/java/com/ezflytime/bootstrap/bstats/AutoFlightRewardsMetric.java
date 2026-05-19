package com.ezflytime.bootstrap.bstats;

import com.ezflytime.EzFlyTimePlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class AutoFlightRewardsMetric {
    public void register(Metrics metrics, EzFlyTimePlugin plugin) {
        try {
            metrics.addCustomChart(new SimplePie("auto_flight_rewards", () -> plugin.getServiceRegistry() != null && plugin.getServiceRegistry().getConfigManager() != null ? plugin.getServiceRegistry().getConfigManager().resolveAutoFlightRewardsStatus() : (plugin.getConfig().getBoolean("auto-flight-rewards.enabled", false) ? "enabled" : "disabled")));
        } catch (Throwable ignored) {
        }
    }
}
