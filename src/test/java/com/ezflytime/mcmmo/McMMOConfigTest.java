package com.ezflytime.mcmmo;

import com.ezflytime.EzFlyTimePlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class McMMOConfigTest {

    private File tempDir;

    @AfterEach
    public void cleanup() {
        if (tempDir != null && tempDir.exists()) {
            for (File f : tempDir.listFiles()) {
                f.delete();
            }
            tempDir.delete();
        }
    }

    @Test
    public void loadDisabledFromDataFolder() throws IOException {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "ezflytime-test-" + System.nanoTime());
        tempDir.mkdirs();

        File mcmmo = new File(tempDir, "mcmmo.yml");
        try (FileWriter w = new FileWriter(mcmmo)) {
            w.write("enabled: false\n");
        }

        EzFlyTimePlugin plugin = mock(EzFlyTimePlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir);

        McMMOConfig cfg = new McMMOConfig(plugin);
        assertNotNull(cfg.getConfig());
        assertFalse(cfg.isEnabled(), "mcmmo.yml enabled:false should result in isEnabled() == false");
    }
}
