package com.ezflytime.bootstrap;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.FlyTimeStorage;
import com.ezflytime.storage.VoucherStorage;
import com.ezflytime.storage.YamlFlyTimeStorage;
import com.ezflytime.storage.YamlVoucherStorage;
import com.ezflytime.storage.UnlockedParticlesStorage;
import com.ezflytime.storage.YamlUnlockedParticlesStorage;
import com.ezflytime.storage.mysql.MySqlConnectionFactory;
import com.ezflytime.storage.mysql.MySqlFlyTimeStorage;
import com.ezflytime.storage.mysql.MySqlVoucherStorage;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Logger;

public class StorageInitializer {

    private final EzFlyTimePlugin plugin;

    public StorageInitializer(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    public StoragePair initialize() {
        String storageType = plugin.getConfig().getString("storage.type", "yaml").toLowerCase(Locale.ROOT);
        if (storageType.equals("mysql")) {
            StoragePair mysqlStorage = setupMySqlStorage();
            if (mysqlStorage != null) {
                return mysqlStorage;
            }
            plugin.getLogger().warning("Falling back to YAML storage due to MySQL configuration issues.");
        } else if (!storageType.equals("yaml")) {
            plugin.getLogger().warning("Unknown storage type '" + storageType + "'. Falling back to YAML storage.");
        }
        return useYamlStorage();
    }

    private StoragePair setupMySqlStorage() {
        ConfigurationSection mysqlSection = plugin.getConfig().getConfigurationSection("storage.mysql");
        if (mysqlSection == null) {
            plugin.getLogger().severe("MySQL storage selected but storage.mysql section is missing.");
            return null;
        }

        String host = mysqlSection.getString("host", "localhost");
        int port = mysqlSection.getInt("port", 3306);
        String database = mysqlSection.getString("database", "ezflytime");
        String username = mysqlSection.getString("username", "root");
        String password = mysqlSection.getString("password", "");
        boolean useSsl = mysqlSection.getBoolean("use-ssl", false);
        String tablePrefix = sanitizeTablePrefix(mysqlSection.getString("table-prefix", "ezflytime_"));

        MySqlConnectionFactory connectionFactory =
                new MySqlConnectionFactory(plugin, host, port, database, username, password, useSsl);
        try (Connection ignored = connectionFactory.getConnection()) {
            // Connection successful
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to MySQL: " + e.getMessage());
            return null;
        }

        return new StoragePair(
            new MySqlFlyTimeStorage(plugin, connectionFactory, tablePrefix),
            new MySqlVoucherStorage(plugin, connectionFactory, tablePrefix),
            new com.ezflytime.storage.mysql.MySqlUnlockedParticlesStorage(plugin, connectionFactory, tablePrefix)
        );
    }

    private String sanitizeTablePrefix(String rawPrefix) {
        Logger logger = plugin.getLogger();
        if (rawPrefix == null || rawPrefix.isEmpty()) {
            logger.warning("Table prefix was empty. Using default 'ezflytime_'.");
            return "ezflytime_";
        }
        if (!rawPrefix.matches("[a-zA-Z0-9_]*")) {
            logger.warning("Invalid characters in table prefix '" + rawPrefix + "'. Using default 'ezflytime_'.");
            return "ezflytime_";
        }
        return rawPrefix;
    }

    private StoragePair useYamlStorage() {
        return new StoragePair(new YamlFlyTimeStorage(plugin), new YamlVoucherStorage(plugin), new YamlUnlockedParticlesStorage(plugin));
    }

    public static final class StoragePair {
        private final FlyTimeStorage flyTimeStorage;
        private final VoucherStorage voucherStorage;
        private final UnlockedParticlesStorage unlockedParticlesStorage;

        public StoragePair(FlyTimeStorage flyTimeStorage, VoucherStorage voucherStorage, UnlockedParticlesStorage unlockedParticlesStorage) {
            this.flyTimeStorage = flyTimeStorage;
            this.voucherStorage = voucherStorage;
            this.unlockedParticlesStorage = unlockedParticlesStorage;
        }

        public FlyTimeStorage getFlyTimeStorage() {
            return flyTimeStorage;
        }

        public VoucherStorage getVoucherStorage() {
            return voucherStorage;
        }

        public UnlockedParticlesStorage getUnlockedParticlesStorage() {
            return unlockedParticlesStorage;
        }
    }
}
