package com.ezflytime.bootstrap.bstats;

import com.ezflytime.EzFlyTimePlugin;
import org.bstats.bukkit.Metrics;

public class BStatsManager {
    private final EzFlyTimePlugin plugin;
    private Metrics metrics;

    public BStatsManager(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            this.metrics = new Metrics(plugin, 27727);
            // register metric providers
            new StorageBackendMetric().register(metrics, plugin);
            new AutoFlightRewardsMetric().register(metrics, plugin);
            new FlightActivationModeMetric().register(metrics, plugin);
            plugin.setMetrics(metrics);
        } catch (Throwable t) {
            plugin.getLogger().warning("Unable to initialise bStats metrics: " + t.getMessage());
        }
    }

    public Metrics getMetrics() {
        return metrics;
    }
}
