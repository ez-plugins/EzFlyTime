package com.ezflytime.storage.mysql;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.UnlockedParticlesStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class MySqlUnlockedParticlesStorage implements UnlockedParticlesStorage {

    private final EzFlyTimePlugin plugin;
    private final MySqlConnectionFactory connectionFactory;
    private final String tableName;
    private final String autoEquipTable;
    private final String equippedTable;

    public MySqlUnlockedParticlesStorage(EzFlyTimePlugin plugin, MySqlConnectionFactory connectionFactory, String tablePrefix) {
        this.plugin = plugin;
        this.connectionFactory = connectionFactory;
        this.tableName = tablePrefix + "unlocked_particles";
        this.autoEquipTable = tablePrefix + "unlocked_particles_auto_equip";
        this.equippedTable = tablePrefix + "unlocked_particles_equipped";
        createTables();
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (player_uuid VARCHAR(36), particle_id VARCHAR(255), PRIMARY KEY (player_uuid, particle_id))";
        String sql2 = "CREATE TABLE IF NOT EXISTS " + autoEquipTable + " (player_uuid VARCHAR(36) PRIMARY KEY, auto_equip BOOLEAN)";
        String sql3 = "CREATE TABLE IF NOT EXISTS " + equippedTable + " (player_uuid VARCHAR(36), particle_id VARCHAR(255), PRIMARY KEY (player_uuid, particle_id))";
        try (Connection connection = connectionFactory.getConnection(); Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(sql2);
            stmt.execute(sql3);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create unlocked particles tables: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Set<String>> loadUnlockedParticles() {
        Map<String, Set<String>> result = new HashMap<>();
        String sql = "SELECT player_uuid, particle_id FROM " + tableName;
        try (Connection connection = connectionFactory.getConnection(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String uuid = rs.getString("player_uuid");
                String pid = rs.getString("particle_id");
                result.computeIfAbsent(uuid, k -> new HashSet<>()).add(pid);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load unlocked particles from MySQL: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void saveUnlockedParticles(Map<String, Set<String>> unlockedParticles) {
        String deleteSql = "DELETE FROM " + tableName;
        String insertSql = "INSERT INTO " + tableName + " (player_uuid, particle_id) VALUES (?, ?)";
        try (Connection connection = connectionFactory.getConnection(); Statement del = connection.createStatement()) {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                del.executeUpdate(deleteSql);
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    for (Map.Entry<String, Set<String>> e : unlockedParticles.entrySet()) {
                        String uuid = e.getKey();
                        for (String pid : e.getValue()) {
                            ps.setString(1, uuid);
                            ps.setString(2, pid);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException ex) {
                try { connection.rollback(); } catch (SQLException r) { plugin.getLogger().severe("Rollback failed: " + r.getMessage()); }
                throw ex;
            } finally {
                connection.setAutoCommit(previous);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save unlocked particles to MySQL: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Boolean> loadAutoEquipSettings() {
        Map<String, Boolean> result = new HashMap<>();
        String sql = "SELECT player_uuid, auto_equip FROM " + autoEquipTable;
        try (Connection connection = connectionFactory.getConnection(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("player_uuid"), rs.getBoolean("auto_equip"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load auto-equip settings from MySQL: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void saveAutoEquipSettings(Map<String, Boolean> settings) {
        String deleteSql = "DELETE FROM " + autoEquipTable;
        String insertSql = "INSERT INTO " + autoEquipTable + " (player_uuid, auto_equip) VALUES (?, ?)";
        try (Connection connection = connectionFactory.getConnection(); Statement del = connection.createStatement()) {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                del.executeUpdate(deleteSql);
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    for (Map.Entry<String, Boolean> e : settings.entrySet()) {
                        ps.setString(1, e.getKey());
                        ps.setBoolean(2, e.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException ex) {
                try { connection.rollback(); } catch (SQLException r) { plugin.getLogger().severe("Rollback failed: " + r.getMessage()); }
                throw ex;
            } finally {
                connection.setAutoCommit(previous);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save auto-equip settings to MySQL: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Set<String>> loadEquippedParticles() {
        Map<String, Set<String>> result = new HashMap<>();
        String sql = "SELECT player_uuid, particle_id FROM " + equippedTable;
        try (Connection connection = connectionFactory.getConnection(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String uuid = rs.getString("player_uuid");
                String pid = rs.getString("particle_id");
                result.computeIfAbsent(uuid, k -> new HashSet<>()).add(pid);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load equipped particles from MySQL: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void saveEquippedParticles(Map<String, Set<String>> equippedParticles) {
        String deleteSql = "DELETE FROM " + equippedTable;
        String insertSql = "INSERT INTO " + equippedTable + " (player_uuid, particle_id) VALUES (?, ?)";
        try (Connection connection = connectionFactory.getConnection(); Statement del = connection.createStatement()) {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                del.executeUpdate(deleteSql);
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    for (Map.Entry<String, Set<String>> e : equippedParticles.entrySet()) {
                        String uuid = e.getKey();
                        for (String pid : e.getValue()) {
                            ps.setString(1, uuid);
                            ps.setString(2, pid);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException ex) {
                try { connection.rollback(); } catch (SQLException r) { plugin.getLogger().severe("Rollback failed: " + r.getMessage()); }
                throw ex;
            } finally {
                connection.setAutoCommit(previous);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save equipped particles to MySQL: " + e.getMessage());
        }
    }
}
