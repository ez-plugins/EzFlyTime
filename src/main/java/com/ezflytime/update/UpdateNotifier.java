package com.ezflytime.update;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotifier implements Listener {

    private final EzFlyTimePlugin plugin;

    public UpdateNotifier(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UpdateCheckResult result = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getUpdateCheckResult() : null;
        if (result == null || result.getStatus() != UpdateCheckResult.Status.UPDATE_AVAILABLE) {
            return;
        }
        if (!event.getPlayer().hasPermission("ezflytime.update")) {
            return;
        }
        String message = plugin.getMessage("messages.update-available")
                .replace("{current}", result.getCurrentVersion())
                .replace("{latest}", result.getLatestVersion())
                .replace("{url}", result.getDownloadUrl());
        plugin.sendMessage(event.getPlayer(), message);
    }
}
