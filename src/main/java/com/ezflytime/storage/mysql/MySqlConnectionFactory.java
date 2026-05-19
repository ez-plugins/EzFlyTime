package com.ezflytime.storage.mysql;

import com.ezflytime.EzFlyTimePlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySqlConnectionFactory {

    private final EzFlyTimePlugin plugin;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean useSsl;

    public MySqlConnectionFactory(EzFlyTimePlugin plugin, String host, int port, String database,
                                  String username, String password, boolean useSsl) {
        this.plugin = plugin;
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
        this.username = username;
        this.password = password;
        this.useSsl = useSsl;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("MySQL driver not found: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("useSSL", Boolean.toString(useSsl));
        properties.setProperty("autoReconnect", "true");
        return DriverManager.getConnection(jdbcUrl, properties);
    }
}
