package com.ezflytime.placeholder;

import com.ezflytime.EzFlyTimePlugin;

public class PlaceholderIntegration {

    private final EzFlyTimePlugin plugin;
    private FlyTimePlaceholderExpansion expansion;

    public PlaceholderIntegration(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerIfAvailable() {
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            expansion = new FlyTimePlaceholderExpansion(plugin);
            expansion.register();
            plugin.getLogger().info("Registered PlaceholderAPI expansion.");
        } else {
            plugin.getLogger().info("PlaceholderAPI not found. Skipping placeholder expansion registration.");
        }
    }

    public void unregister() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }
}
