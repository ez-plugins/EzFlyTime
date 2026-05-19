package com.ezflytime.particles;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.UnlockedParticlesStorage;

import java.util.*;

public class UnlockedParticlesManager {

    private final EzFlyTimePlugin plugin;
    private final UnlockedParticlesStorage storage;

    // key: player UUID string -> set of particle IDs
    private final Map<String, Set<String>> unlocked = new HashMap<>();
    private final Map<String, Boolean> autoEquip = new HashMap<>();
    private final Map<String, Set<String>> equipped = new HashMap<>();

    public UnlockedParticlesManager(EzFlyTimePlugin plugin, UnlockedParticlesStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        loadData();
    }

    private void loadData() {
        Map<String, Set<String>> loaded = storage.loadUnlockedParticles();
        if (loaded != null) unlocked.putAll(loaded);
        Map<String, Boolean> settings = storage.loadAutoEquipSettings();
        if (settings != null) autoEquip.putAll(settings);
        Map<String, Set<String>> equippedLoaded = storage.loadEquippedParticles();
        if (equippedLoaded != null) equipped.putAll(equippedLoaded);
    }

    public synchronized boolean isUnlocked(UUID player, String particleId) {
        return unlocked.getOrDefault(player.toString(), Collections.emptySet()).contains(particleId);
    }

    public synchronized Set<String> getUnlocked(UUID player) {
        return Collections.unmodifiableSet(unlocked.getOrDefault(player.toString(), Collections.emptySet()));
    }

    public synchronized boolean unlock(UUID player, String particleId) {
        String key = player.toString();
        Set<String> set = unlocked.computeIfAbsent(key, k -> new HashSet<>());
        boolean added = set.add(particleId);
        if (added) saveUnlocked();
        return added;
    }

    public synchronized boolean setAutoEquip(UUID player, boolean value) {
        String key = player.toString();
        Boolean prev = autoEquip.put(key, value);
        saveAutoEquip();
        return prev == null || !prev.equals(value);
    }

    public synchronized boolean isAutoEquip(UUID player) {
        return autoEquip.getOrDefault(player.toString(), false);
    }

    public synchronized Set<String> getEquipped(UUID player) {
        return Collections.unmodifiableSet(equipped.getOrDefault(player.toString(), Collections.emptySet()));
    }

    public synchronized boolean toggleEquipped(UUID player, String particleId) {
        String key = player.toString();
        Set<String> set = equipped.computeIfAbsent(key, k -> new HashSet<>());
        boolean changed;
        if (set.contains(particleId)) {
            changed = set.remove(particleId);
        } else {
            changed = set.add(particleId);
        }
        if (changed) saveEquipped();
        return changed;
    }

    public synchronized void saveData() {
        saveUnlocked();
        saveAutoEquip();
        saveEquipped();
    }

    private void saveUnlocked() {
        storage.saveUnlockedParticles(unlocked);
    }

    private void saveAutoEquip() {
        storage.saveAutoEquipSettings(autoEquip);
    }

    private void saveEquipped() {
        storage.saveEquippedParticles(equipped);
    }
}
