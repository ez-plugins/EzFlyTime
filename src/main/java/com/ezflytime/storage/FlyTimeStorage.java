package com.ezflytime.storage;

import java.util.Map;
import java.util.UUID;

public interface FlyTimeStorage {

    Map<UUID, Integer> loadFlyTimes();

    void saveFlyTimes(Map<UUID, Integer> remainingSeconds);

    default void close() {
        // Default no-op.
    }
}
