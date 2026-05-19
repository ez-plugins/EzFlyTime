package com.ezflytime.particles;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class ParticleSpawner {
    private final EzFlyTimePlugin plugin;
    private final ParticleConfig cfg;
    private final ParticleTrailManager trailManager;
    private final Random random = new Random();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastLocationTimes = new HashMap<>();

    public ParticleSpawner(EzFlyTimePlugin plugin, ParticleConfig cfg, ParticleTrailManager trailManager) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.trailManager = trailManager;
    }

    public void spawnParticles(Player player) {
        if (!isEnabled()) return;

        String requiredPermission = cfg.getConfig().getString("permissions.required-permission", "");
        if (!requiredPermission.isEmpty() && !player.hasPermission(requiredPermission)) return;

        Location playerLocation = player.getLocation();
        Vector velocity = player.getVelocity();
        double speed = velocity.length();
        // Compute horizontal (XZ) speed from location deltas (more reliable while flying)
        double horizontalSpeed = 0.0;
        UUID playerId = player.getUniqueId();
        try {
            long now = System.currentTimeMillis();
            Location nowLoc = playerLocation;
            Location lastLoc = lastLocations.get(playerId);
            Long lastTime = lastLocationTimes.get(playerId);
            if (lastLoc != null && lastTime != null && now > lastTime) {
                double dx = nowLoc.getX() - lastLoc.getX();
                double dz = nowLoc.getZ() - lastLoc.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                long dt = now - lastTime; // ms
                if (dt > 0) {
                    horizontalSpeed = dist / (dt / 1000.0); // blocks per second
                }
            }
            lastLocations.put(playerId, nowLoc.clone());
            lastLocationTimes.put(playerId, now);
        } catch (Throwable t) {
            horizontalSpeed = 0.0;
        }

        if (cfg.getConfig().getBoolean("advanced.only-when-moving", false)) {
            double minSpeed = cfg.getConfig().getDouble("advanced.min-speed", 0.05);
            if (horizontalSpeed < minSpeed) return;
        }

        addEnvironmentalEffects(player, playerLocation);
        playSounds(player, speed);

        double spawnDistance = cfg.getConfig().getDouble("advanced.spawn-distance", 0.0);
        double spread = cfg.getConfig().getDouble("advanced.spread", 0.7);
        int maxParticlesPerTick = cfg.getConfig().getInt("advanced.performance.max-particles-per-tick", 50);

        // fetch unlocked particles for this player (if manager available)
        com.ezflytime.particles.UnlockedParticlesManager unlockedManager = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getUnlockedParticlesManager() : null;
        java.util.Set<String> unlockedSet = null;
        if (unlockedManager != null) {
            try {
                unlockedSet = unlockedManager.getUnlocked(player.getUniqueId());
            } catch (Throwable t) {
                unlockedSet = null;
            }
        }

        int totalParticlesSpawned = 0;
        trailManager.updatePlayerTrails(player, playerLocation, cfg);

        String[] effectKeys = {"primary", "secondary", "tertiary", "wing-trail", "speed-boost", "ground-effect", "sonic-boom", "magic-aura"};
        for (String effectKey : effectKeys) {
            if (totalParticlesSpawned >= maxParticlesPerTick) break;
            boolean shouldSpawn = shouldSpawnEffect(player, effectKey, horizontalSpeed, playerLocation.getY(), unlockedSet);
            if (shouldSpawn) {
                int spawned = spawnParticleEffect(player, effectKey, playerLocation, velocity, spawnDistance, spread, horizontalSpeed);
                totalParticlesSpawned += spawned;
                if (cfg.getConfig().getBoolean("particles." + effectKey + ".trail.enabled", false)) {
                    trailManager.addToTrail(playerId, effectKey, playerLocation);
                }
            }
        }
    }

    public boolean isEnabled() { return cfg.getConfig().getBoolean("enabled", true); }

    private void addEnvironmentalEffects(Player player, Location location) {
        World world = location.getWorld(); if (world == null) return;
        if (cfg.getConfig().getBoolean("advanced.environment.water-effect", true)) {
            Block block = location.getBlock();
            if (block.getType() == Material.WATER) world.spawnParticle(Particle.WATER_BUBBLE, location, 3, 0.5, 0.5, 0.5, 0.1);
        }
        if (cfg.getConfig().getBoolean("advanced.environment.lava-effect", true)) {
            Block block = location.getBlock();
            if (block.getType() == Material.LAVA) world.spawnParticle(Particle.FLAME, location, 5, 0.3, 0.3, 0.3, 0.05);
        }
        double height = location.getY();
        if (height > 150 && cfg.getConfig().getBoolean("advanced.environment.high-altitude-effect", true)) {
            world.spawnParticle(Particle.CLOUD, location, 2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private void playSounds(Player player, double speed) {
        if (!cfg.getConfig().getBoolean("sounds.enabled", true)) return;
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (speed >= 1.0) {
            Long lastBoom = (Long) null; // simplified: use plugin-level maps if needed
            int cooldown = cfg.getConfig().getInt("sounds.sonic-boom.cooldown", 5) * 1000;
            // simplified: always play for now
            playSound(player, "sonic-boom");
        }
        if (cfg.getConfig().getBoolean("sounds.ambient.enabled", false)) {
            playSound(player, "ambient");
        }
    }

    private void playSound(Player player, String soundKey) {
        try {
            String soundName = cfg.getConfig().getString("sounds." + soundKey + ".sound");
            if (soundName == null) return;
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) cfg.getConfig().getDouble("sounds." + soundKey + ".volume", 1.0);
            float pitch = (float) cfg.getConfig().getDouble("sounds." + soundKey + ".pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + cfg.getConfig().getString("sounds." + soundKey + ".sound"));
        }
    }

    private boolean shouldSpawnEffect(Player player, String effectKey, double speed, double height, java.util.Set<String> unlockedSet) {
        if (!cfg.getConfig().getBoolean("particles." + effectKey + ".enabled", effectKey.equals("primary"))) return false;

        // If an unlocked-particles manager is present and the player has any unlocked entries,
        // require the effectKey to be unlocked or the player to have the specific permission.
        if (unlockedSet != null && !unlockedSet.isEmpty()) {
            String particlePermission = "ezflytime.particles." + effectKey;
            if (!unlockedSet.contains(effectKey) && !player.hasPermission(particlePermission)) {
                return false;
            }
        }

        // Optional: require the player to be sprinting for this effect
        if (cfg.getConfig().getBoolean("particles." + effectKey + ".require-sprinting", false)) {
            // Some servers/flight modes do not set the player's sprint flag while flying.
            // Consider the player sprinting when either the sprint flag is set or their
            // horizontal velocity exceeds a configurable sprint detection threshold.
            double sprintDetect = cfg.getConfig().getDouble("advanced.sprint-threshold", 0.18);
            boolean movingFast = speed >= sprintDetect;
            if (!player.isSprinting() && !movingFast) {
                return false;
            }
        }
        if (effectKey.equals("speed-boost")) {
            String speedBoostPerm = cfg.getConfig().getString("permissions.speed-boost", "");
            if (!speedBoostPerm.isEmpty() && !player.hasPermission(speedBoostPerm)) return false;
        }
        double minSpeed = cfg.getConfig().getDouble("particles." + effectKey + ".min-speed", 0.0);
        if (speed < minSpeed) return false;
        if (effectKey.equals("ground-effect")) {
            double maxHeight = cfg.getConfig().getDouble("particles." + effectKey + ".max-height", 10.0);
            if (height > maxHeight) return false;
        }
        return true;
    }

    private int spawnParticleEffect(Player player, String effectKey, Location location, Vector velocity, double spawnDistance, double spread, double speed) {
        String particleTypeStr = cfg.getConfig().getString("particles." + effectKey + ".type", "FLAME");
        Particle particleType;
        try { particleType = Particle.valueOf(particleTypeStr.toUpperCase()); } catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid particle type: " + particleTypeStr + ". Using FLAME."); particleType = Particle.FLAME; }

        int baseCount = cfg.getConfig().getInt("particles." + effectKey + ".count", 5);
        double baseSpeed = cfg.getConfig().getDouble("particles." + effectKey + ".speed", 0.1);
        boolean speedResponsive = cfg.getConfig().getBoolean("particles." + effectKey + ".speed-responsive", false);
        int count = baseCount;
        if (speedResponsive && speed > 0.1) count = (int) Math.min(baseCount * 2, baseCount * (1 + speed * 5));

        // If player is sprinting (by speed) and a sprint-specific particle type is configured, use it
        double sprintDetect = cfg.getConfig().getDouble("advanced.sprint-threshold", 0.18);
        if (speed >= sprintDetect) {
            String sprintType = cfg.getConfig().getString("particles." + effectKey + ".sprint-type", null);
            if (sprintType != null && !sprintType.isEmpty()) {
                try {
                    Particle sprintParticle = Particle.valueOf(sprintType.toUpperCase());
                    particleType = sprintParticle;
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid sprint-type for particles." + effectKey + ": " + sprintType + ". Using default type.");
                }
            }
        }

        // Debug information for spawn (helps identify unexpected white smoke fallbacks)
        double effectSpread = cfg.getConfig().getDouble("particles." + effectKey + ".spread", spread);
        plugin.debug(String.format("Spawning particle effect '%s' type=%s count=%d speed=%s sprintDetect=%.2f spread=%s", effectKey, particleType.name(), count, baseSpeed, sprintDetect, effectSpread));

        double offsetX = cfg.getConfig().getDouble("particles." + effectKey + ".offset.x", 0.5);
        double offsetY = cfg.getConfig().getDouble("particles." + effectKey + ".offset.y", 0.0);
        double offsetZ = cfg.getConfig().getDouble("particles." + effectKey + ".offset.z", 0.5);

        boolean directional = cfg.getConfig().getBoolean("particles." + effectKey + ".directional", false);
        Location spawnLocation = location.clone();
        if (directional && speed > 0.1) {
            // prefer movement direction from velocity when available, otherwise use player facing
            Vector direction;
            if (velocity.length() > 0.1) {
                direction = velocity.clone().normalize();
            } else {
                direction = player.getLocation().getDirection().clone().normalize();
            }
            if (effectKey.equals("wing-trail")) direction.multiply(-1);
            spawnLocation.add(direction.multiply(spawnDistance > 0 ? spawnDistance : 0.5));
        } else if (spawnDistance > 0) {
            Vector direction = velocity.clone().normalize().multiply(-spawnDistance);
            spawnLocation.add(direction);
        }

        if (effectSpread > 0) spawnLocation.add((random.nextDouble() - 0.5) * effectSpread, (random.nextDouble() - 0.5) * effectSpread, (random.nextDouble() - 0.5) * effectSpread);

        if (particleType == Particle.REDSTONE) {
            int red = cfg.getConfig().getInt("particles." + effectKey + ".color.red", 255);
            int green = cfg.getConfig().getInt("particles." + effectKey + ".color.green", 100);
            int blue = cfg.getConfig().getInt("particles." + effectKey + ".color.blue", 0);
            float size = (float) cfg.getConfig().getDouble("particles." + effectKey + ".size", 1.0);
            Color color = Color.fromRGB(red, green, blue);
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, size);
            player.getWorld().spawnParticle(particleType, spawnLocation, count, offsetX, offsetY, offsetZ, baseSpeed, dustOptions);
        } else {
            player.getWorld().spawnParticle(particleType, spawnLocation, count, offsetX, offsetY, offsetZ, baseSpeed);
        }

        return count;
    }

    public void playStartFlyingSound(Player player) {
        if (cfg.getConfig().getBoolean("sounds.enabled", true)) playSound(player, "start-flying");
    }

    public void clearPlayerTrails(UUID playerId) { trailManager.clearPlayerTrails(playerId); }
}
