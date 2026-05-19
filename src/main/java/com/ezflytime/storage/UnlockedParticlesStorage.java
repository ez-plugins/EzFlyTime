package com.ezflytime.storage;

import java.util.Map;
import java.util.Set;

public interface UnlockedParticlesStorage {

    Map<String, Set<String>> loadUnlockedParticles();

    void saveUnlockedParticles(Map<String, Set<String>> unlockedParticles);

    Map<String, Boolean> loadAutoEquipSettings();

    void saveAutoEquipSettings(Map<String, Boolean> settings);

    Map<String, Set<String>> loadEquippedParticles();

    void saveEquippedParticles(Map<String, Set<String>> equippedParticles);

    default void close() {
        // Default no-op
    }
}
