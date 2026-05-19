package com.ezflytime.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class ServerUUID {

    private final UUID uuid;

    public ServerUUID(JavaPlugin plugin) {
        File uuidFile = new File(plugin.getDataFolder(), "server.uuid");
        UUID found = null;
        if (uuidFile.exists()) {
            try {
                String uuidString = java.nio.file.Files.readString(uuidFile.toPath()).trim();
                found = UUID.fromString(uuidString);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read server UUID file, generating new one: " + e.getMessage());
            }
        }

        if (found == null) {
            found = UUID.randomUUID();
            try {
                if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                    plugin.getLogger().warning("Unable to create plugin data folder for server UUID.");
                }
                java.nio.file.Files.writeString(uuidFile.toPath(), found.toString());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save server UUID: " + e.getMessage());
            }
        }

        this.uuid = found;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return uuid == null ? "" : uuid.toString();
    }
}
