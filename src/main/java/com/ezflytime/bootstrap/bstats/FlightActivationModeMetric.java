package com.ezflytime.bootstrap.bstats;

import com.ezflytime.EzFlyTimePlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class FlightActivationModeMetric {
    public void register(Metrics metrics, EzFlyTimePlugin plugin) {
        try {
            metrics.addCustomChart(new SimplePie("flight_activation_mode", () -> plugin.getServiceRegistry() != null && plugin.getServiceRegistry().getConfigManager() != null ? plugin.getServiceRegistry().getConfigManager().resolveActivationMode() : plugin.getConfig().getString("flight.activation-mode", "manual")));
        } catch (Throwable ignored) {
        }
    }
}
