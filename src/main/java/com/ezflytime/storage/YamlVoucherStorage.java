package com.ezflytime.storage;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class YamlVoucherStorage implements VoucherStorage {

    private final EzFlyTimePlugin plugin;
    private final File consumedVoucherFile;

    public YamlVoucherStorage(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        this.consumedVoucherFile = new File(plugin.getDataFolder(), "consumed-vouchers.yml");
        if (!consumedVoucherFile.exists()) {
            try {
                consumedVoucherFile.getParentFile().mkdirs();
                consumedVoucherFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create consumed-vouchers.yml", e);
            }
        }
    }

    @Override
    public Set<String> loadConsumedVoucherIds() {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(consumedVoucherFile);
        return new HashSet<>(configuration.getStringList("consumed"));
    }

    @Override
    public void saveConsumedVoucherIds(Set<String> consumedVoucherIds) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("consumed", new java.util.ArrayList<>(consumedVoucherIds));
        try {
            configuration.save(consumedVoucherFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save consumed-vouchers.yml", e);
        }
    }
}
