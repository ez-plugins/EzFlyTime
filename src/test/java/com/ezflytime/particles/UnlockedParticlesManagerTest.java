package com.ezflytime.particles;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.YamlUnlockedParticlesStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UnlockedParticlesManagerTest {

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
    public void testUnlockAndEquipPersistence() {
        YamlUnlockedParticlesStorage storage = new YamlUnlockedParticlesStorage(plugin);
        UnlockedParticlesManager manager = new UnlockedParticlesManager(plugin, storage);

        UUID player = UUID.randomUUID();
        assertFalse(manager.isUnlocked(player, "primary"));

        boolean added = manager.unlock(player, "primary");
        assertTrue(added);
        assertTrue(manager.isUnlocked(player, "primary"));

        boolean toggled = manager.toggleEquipped(player, "primary");
        assertTrue(toggled);
        assertTrue(manager.getEquipped(player).contains("primary"));

        manager.saveData();

        // Create new manager backed by same storage to verify persistence
        UnlockedParticlesManager manager2 = new UnlockedParticlesManager(plugin, storage);
        assertTrue(manager2.isUnlocked(player, "primary"));
        assertTrue(manager2.getEquipped(player).contains("primary"));
    }
}
