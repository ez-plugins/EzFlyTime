package com.ezflytime.storage.mysql;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.VoucherStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class MySqlVoucherStorage implements VoucherStorage {

    private final EzFlyTimePlugin plugin;
    private final MySqlConnectionFactory connectionFactory;
    private final String tableName;

    public MySqlVoucherStorage(EzFlyTimePlugin plugin, MySqlConnectionFactory connectionFactory, String tablePrefix) {
        this.plugin = plugin;
        this.connectionFactory = connectionFactory;
        this.tableName = tablePrefix + "consumed_vouchers";
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "voucher_id VARCHAR(255) PRIMARY KEY" +
                ")";
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create consumed vouchers table: " + e.getMessage());
        }
    }

    @Override
    public Set<String> loadConsumedVoucherIds() {
        Set<String> ids = new HashSet<>();
        String sql = "SELECT voucher_id FROM " + tableName;
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getString("voucher_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load consumed vouchers from MySQL: " + e.getMessage());
        }
        return ids;
    }

    @Override
    public void saveConsumedVoucherIds(Set<String> consumedVoucherIds) {
        String deleteSql = "DELETE FROM " + tableName;
        String insertSql = "INSERT INTO " + tableName + " (voucher_id) VALUES (?)";
        try (Connection connection = connectionFactory.getConnection();
             Statement deleteStatement = connection.createStatement()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                deleteStatement.executeUpdate(deleteSql);
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    for (String id : consumedVoucherIds) {
                        insertStatement.setString(1, id);
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    plugin.getLogger().severe("Could not rollback consumed vouchers transaction: " + rollbackException.getMessage());
                }
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save consumed vouchers to MySQL: " + e.getMessage());
        }
    }
}
