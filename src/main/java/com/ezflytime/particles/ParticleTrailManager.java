package com.ezflytime.particles;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleTrailManager {
    private final EzFlyTimePlugin plugin;
    private final Map<UUID, List<ParticleTrail>> activeTrails = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTrailUpdate = new ConcurrentHashMap<>();
    private BukkitRunnable trailUpdater;

    public ParticleTrailManager(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        startTrailUpdater();
    }

    // ParticleTrail is now a top-level class in the same package: ParticleTrail

    private void startTrailUpdater() {
        trailUpdater = new BukkitRunnable() {
            @Override
            public void run() {
                updateTrails();
                cleanupExpiredTrails();
            }
        };
        trailUpdater.runTaskTimer(plugin, 1L, 1L);
    }

    private void updateTrails() {
        for (Map.Entry<UUID, List<ParticleTrail>> entry : activeTrails.entrySet()) {
            UUID playerId = entry.getKey();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;
            List<ParticleTrail> trails = entry.getValue();
            for (ParticleTrail trail : trails) {
                // Consumers of trails will pull points as needed
            }
        }
    }

    private void cleanupExpiredTrails() {
        activeTrails.entrySet().removeIf(entry -> {
            List<ParticleTrail> trails = entry.getValue();
            trails.removeIf(ParticleTrail::isExpired);
            return trails.isEmpty();
        });
    }

    public void updatePlayerTrails(Player player, Location location, ParticleConfig cfg) {
        UUID playerId = player.getUniqueId();
        List<ParticleTrail> playerTrails = activeTrails.computeIfAbsent(playerId, k -> new ArrayList<>());

        for (ParticleTrail trail : playerTrails) {
            trail.addPoint(location);
        }

        String[] trailEffects = {"primary", "secondary", "wing-trail"};
        for (String effectKey : trailEffects) {
            if (cfg.getConfig().getBoolean("particles." + effectKey + ".trail.enabled", false)) {
                boolean hasTrail = playerTrails.stream().anyMatch(t -> t.getEffectKey().equals(effectKey));
                if (!hasTrail) {
                    int trailLength = cfg.getConfig().getInt("particles." + effectKey + ".trail.length", 10);
                    playerTrails.add(new ParticleTrail(effectKey, trailLength));
                }
            }
        }
    }

    public void addToTrail(UUID playerId, String effectKey, Location location) {
        List<ParticleTrail> playerTrails = activeTrails.get(playerId);
        if (playerTrails != null) {
            for (ParticleTrail trail : playerTrails) {
                if (trail.getEffectKey().equals(effectKey)) {
                    trail.addPoint(location);
                    break;
                }
            }
        }
    }

    public List<ParticleTrail> getTrails(UUID playerId) {
        return activeTrails.getOrDefault(playerId, Collections.emptyList());
    }

    public void clearPlayerTrails(UUID playerId) {
        activeTrails.remove(playerId);
    }

    public void stop() {
        if (trailUpdater != null) {
            trailUpdater.cancel();
            trailUpdater = null;
        }
    }
}
