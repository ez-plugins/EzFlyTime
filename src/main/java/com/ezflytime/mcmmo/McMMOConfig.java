package com.ezflytime.mcmmo;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class McMMOConfig {
    private final EzFlyTimePlugin plugin;
    private FileConfiguration config;

    public McMMOConfig(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "mcmmo.yml");
        if (!file.exists()) {
            plugin.saveResource("mcmmo.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        try (InputStream defaultStream = plugin.getResource("mcmmo.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defaultStream));
                this.config.setDefaults(defaults);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load default mcmmo configuration: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isEnabled() {
        if (config == null) {
            return true;
        }
        return config.getBoolean("enabled", true);
    }

    public void reload() {
        load();
    }
}
