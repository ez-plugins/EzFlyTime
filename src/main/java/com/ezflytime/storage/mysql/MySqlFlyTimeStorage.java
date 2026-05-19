package com.ezflytime.storage.mysql;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.FlyTimeStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySqlFlyTimeStorage implements FlyTimeStorage {

    private final EzFlyTimePlugin plugin;
    private final MySqlConnectionFactory connectionFactory;
    private final String tableName;

    public MySqlFlyTimeStorage(EzFlyTimePlugin plugin, MySqlConnectionFactory connectionFactory, String tablePrefix) {
        this.plugin = plugin;
        this.connectionFactory = connectionFactory;
        this.tableName = tablePrefix + "fly_time";
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "uuid CHAR(36) PRIMARY KEY," +
                "seconds INT NOT NULL" +
                ")";
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create fly time table: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Integer> loadFlyTimes() {
        Map<UUID, Integer> remainingSeconds = new HashMap<>();
        String sql = "SELECT uuid, seconds FROM " + tableName;
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String uuidString = resultSet.getString("uuid");
                int seconds = resultSet.getInt("seconds");
                if (seconds > 0) {
                    remainingSeconds.put(UUID.fromString(uuidString), seconds);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load fly times from MySQL: " + e.getMessage());
        }
        return remainingSeconds;
    }

    @Override
    public void saveFlyTimes(Map<UUID, Integer> remainingSeconds) {
        String deleteSql = "DELETE FROM " + tableName;
        String insertSql = "INSERT INTO " + tableName + " (uuid, seconds) VALUES (?, ?)";
        try (Connection connection = connectionFactory.getConnection();
             Statement deleteStatement = connection.createStatement()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                deleteStatement.executeUpdate(deleteSql);
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    for (Map.Entry<UUID, Integer> entry : remainingSeconds.entrySet()) {
                        if (entry.getValue() <= 0) {
                            continue;
                        }
                        insertStatement.setString(1, entry.getKey().toString());
                        insertStatement.setInt(2, entry.getValue());
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    plugin.getLogger().severe("Could not rollback fly times transaction: " + rollbackException.getMessage());
                }
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save fly times to MySQL: " + e.getMessage());
        }
    }
}
