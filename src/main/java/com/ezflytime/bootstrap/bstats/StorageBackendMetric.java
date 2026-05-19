package com.ezflytime.bootstrap.bstats;

import com.ezflytime.EzFlyTimePlugin;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class StorageBackendMetric {
    public void register(Metrics metrics, EzFlyTimePlugin plugin) {
        try {
            metrics.addCustomChart(new SimplePie("storage_backend", () -> plugin.getServiceRegistry() != null && plugin.getServiceRegistry().getConfigManager() != null ? plugin.getServiceRegistry().getConfigManager().resolveStorageBackend() : plugin.getConfig().getString("storage.type", "yaml")));
        } catch (Throwable ignored) {
        }
    }
}
