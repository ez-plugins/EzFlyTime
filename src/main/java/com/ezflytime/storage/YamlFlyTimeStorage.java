package com.ezflytime.storage;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class YamlFlyTimeStorage implements FlyTimeStorage {

    private final EzFlyTimePlugin plugin;
    private final File dataFolder;
    private final File legacyDataFile;

    public YamlFlyTimeStorage(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "flytime-data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("Could not create flytime-data directory at " + dataFolder.getAbsolutePath());
        }
        this.legacyDataFile = new File(plugin.getDataFolder(), "flytime-data.yml");
        if (legacyDataFile.exists() && legacyDataFile.isFile()) {
            migrateLegacyDataFile();
        }
    }

    @Override
    public Map<UUID, Integer> loadFlyTimes() {
        Map<UUID, Integer> remainingSeconds = new HashMap<>();
        if (!dataFolder.exists()) {
            return remainingSeconds;
        }

        File[] playerFiles = dataFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (playerFiles == null) {
            return remainingSeconds;
        }

        for (File playerFile : playerFiles) {
            String fileName = playerFile.getName();
            if (fileName.length() <= 4) {
                plugin.getLogger().severe("Ignoring invalid flytime data file name: " + fileName);
                continue;
            }

            String uuidPart = fileName.substring(0, fileName.length() - 4);
            try {
                UUID playerId = UUID.fromString(uuidPart);
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(playerFile);
                int seconds = configuration.getInt("seconds", 0);
                if (seconds > 0) {
                    remainingSeconds.put(playerId, seconds);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("Ignoring invalid flytime data file name: " + fileName);
            }
        }

        return remainingSeconds;
    }

    @Override
    public void saveFlyTimes(Map<UUID, Integer> remainingSeconds) {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("Could not create flytime-data directory at " + dataFolder.getAbsolutePath());
            return;
        }

        File[] existingFilesArray = dataFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        Set<String> remainingFiles = new HashSet<>();
        if (existingFilesArray != null) {
            for (File file : existingFilesArray) {
                remainingFiles.add(file.getName());
            }
        }

        for (Map.Entry<UUID, Integer> entry : remainingSeconds.entrySet()) {
            File playerFile = new File(dataFolder, entry.getKey() + ".yml");
            remainingFiles.remove(playerFile.getName());

            int seconds = entry.getValue();
            if (seconds > 0) {
                YamlConfiguration configuration = new YamlConfiguration();
                configuration.set("seconds", seconds);
                try {
                    configuration.save(playerFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save flytime data for " + entry.getKey() + ": " + e.getMessage());
                }
            } else if (playerFile.exists() && !playerFile.delete()) {
                plugin.getLogger().severe("Could not delete flytime data file for " + entry.getKey());
            }
        }

        for (String leftover : remainingFiles) {
            File file = new File(dataFolder, leftover);
            if (!file.delete()) {
                plugin.getLogger().severe("Could not delete obsolete flytime data file " + file.getName());
            }
        }
    }

    private void migrateLegacyDataFile() {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(legacyDataFile);
        if (!configuration.contains("players")) {
            if (!legacyDataFile.delete()) {
                plugin.getLogger().severe("Could not delete legacy flytime-data.yml file at " + legacyDataFile.getAbsolutePath());
            }
            return;
        }

        Map<UUID, Integer> legacyData = new HashMap<>();
        ConfigurationSection playersSection = configuration.getConfigurationSection("players");
        if (playersSection == null) {
            if (!legacyDataFile.delete()) {
                plugin.getLogger().severe("Could not delete legacy flytime-data.yml file at " + legacyDataFile.getAbsolutePath());
            }
            return;
        }

        for (String key : playersSection.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                int seconds = configuration.getInt("players." + key + ".seconds", 0);
                if (seconds > 0) {
                    legacyData.put(playerId, seconds);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("Skipping invalid UUID in legacy flytime-data.yml: " + key);
            }
        }

        saveFlyTimes(legacyData);

        File backupFile = new File(plugin.getDataFolder(), "flytime-data.yml.legacy");
        if (!legacyDataFile.renameTo(backupFile)) {
            if (!legacyDataFile.delete()) {
                plugin.getLogger().severe("Could not remove legacy flytime-data.yml after migration");
            }
        }
    }
}
