package com.ezflytime.particles;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ParticlesManager {

    private final EzFlyTimePlugin plugin;
    private final ParticleConfig config;
    private final ParticleTrailManager trailManager;
    private final ParticleSpawner spawner;

    public ParticlesManager(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        this.config = new ParticleConfig(plugin);
        this.trailManager = new ParticleTrailManager(plugin);
        this.spawner = new ParticleSpawner(plugin, config, trailManager);
    }

    // ParticleTrail is a top-level class in the same package; ParticlesManager delegates trail handling

    public void reloadConfig() { config.reload(); }

    public boolean isEnabled() { return spawner.isEnabled(); }

    public void spawnParticles(Player player) { spawner.spawnParticles(player); }

    public void playStartFlyingSound(Player player) { spawner.playStartFlyingSound(player); }

    public void updatePlayerTrails(Player player, org.bukkit.Location location) { trailManager.updatePlayerTrails(player, location, config); }

    public void clearPlayerTrails(UUID playerId) { trailManager.clearPlayerTrails(playerId); }

    public org.bukkit.configuration.file.FileConfiguration getConfig() { return config.getConfig(); }

    public void stop() {
        trailManager.stop();
    }
}