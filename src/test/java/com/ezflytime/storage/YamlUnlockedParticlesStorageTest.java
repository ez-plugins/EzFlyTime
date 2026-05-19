package com.ezflytime.storage;

import com.ezflytime.EzFlyTimePlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class YamlUnlockedParticlesStorageTest {

    private File tempDir;
    private EzFlyTimePlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        tempDir = Files.createTempDirectory("ezflytime-test").toFile();
        plugin = Mockito.mock(EzFlyTimePlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(tempDir);
        Mockito.when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
    }

    @AfterEach
    public void tearDown() {
        for (File f : tempDir.listFiles()) f.delete();
        tempDir.delete();
    }

    @Test
    public void saveAndLoadUnlockedAndAutoEquip() {
        YamlUnlockedParticlesStorage storage = new YamlUnlockedParticlesStorage(plugin);

        Map<String, Set<String>> unlocked = new HashMap<>();
        unlocked.put("uuid-1", new HashSet<>(Arrays.asList("primary", "extra")));
        storage.saveUnlockedParticles(unlocked);

        Map<String, Boolean> autoEquip = new HashMap<>();
        autoEquip.put("uuid-1", true);
        storage.saveAutoEquipSettings(autoEquip);

        Map<String, Set<String>> loaded = storage.loadUnlockedParticles();
        assertTrue(loaded.containsKey("uuid-1"));
        assertEquals(2, loaded.get("uuid-1").size());

        Map<String, Boolean> loadedAuto = storage.loadAutoEquipSettings();
        assertTrue(loadedAuto.getOrDefault("uuid-1", false));
    }
}
