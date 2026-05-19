package com.ezflytime.storage;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class YamlUnlockedParticlesStorage implements UnlockedParticlesStorage {

    private final EzFlyTimePlugin plugin;
    private final File dataFile;

    public YamlUnlockedParticlesStorage(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "unlocked-particles.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create unlocked-particles.yml", e);
            }
        }
    }

    @Override
    public Map<String, Set<String>> loadUnlockedParticles() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        Map<String, Set<String>> result = new HashMap<>();
        if (cfg.isConfigurationSection("players")) {
            for (String key : cfg.getConfigurationSection("players").getKeys(false)) {
                List<String> list = cfg.getStringList("players." + key + ".unlocked");
                result.put(key, new HashSet<>(list));
            }
        }
        return result;
    }

    @Override
    public Map<String, Set<String>> loadEquippedParticles() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        Map<String, Set<String>> result = new HashMap<>();
        if (cfg.isConfigurationSection("players")) {
            for (String key : cfg.getConfigurationSection("players").getKeys(false)) {
                List<String> list = cfg.getStringList("players." + key + ".equipped");
                result.put(key, new HashSet<>(list));
            }
        }
        return result;
    }

    @Override
    public void saveUnlockedParticles(Map<String, Set<String>> unlockedParticles) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (Map.Entry<String, Set<String>> e : unlockedParticles.entrySet()) {
            cfg.set("players." + e.getKey() + ".unlocked", new ArrayList<>(e.getValue()));
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save unlocked-particles.yml", ex);
        }
    }

    @Override
    public void saveEquippedParticles(Map<String, Set<String>> equippedParticles) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (Map.Entry<String, Set<String>> e : equippedParticles.entrySet()) {
            cfg.set("players." + e.getKey() + ".equipped", new ArrayList<>(e.getValue()));
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save unlocked-particles.yml", ex);
        }
    }

    @Override
    public Map<String, Boolean> loadAutoEquipSettings() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        Map<String, Boolean> result = new HashMap<>();
        if (cfg.isConfigurationSection("players")) {
            for (String key : cfg.getConfigurationSection("players").getKeys(false)) {
                boolean b = cfg.getBoolean("players." + key + ".auto-equip", false);
                result.put(key, b);
            }
        }
        return result;
    }

    @Override
    public void saveAutoEquipSettings(Map<String, Boolean> settings) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (Map.Entry<String, Boolean> e : settings.entrySet()) {
            cfg.set("players." + e.getKey() + ".auto-equip", e.getValue());
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save unlocked-particles.yml", ex);
        }
    }
}
