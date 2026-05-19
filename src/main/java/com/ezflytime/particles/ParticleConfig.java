package com.ezflytime.particles;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ParticleConfig {
    private final EzFlyTimePlugin plugin;
    private FileConfiguration config;

    public ParticleConfig(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File particlesFile = new File(plugin.getDataFolder(), "particles.yml");
        if (!particlesFile.exists()) {
            plugin.saveResource("particles.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(particlesFile);

        try (InputStream defaultStream = plugin.getResource("particles.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defaultStream));
                this.config.setDefaults(defaultConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load default particles configuration: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void reload() {
        load();
    }
}
