package com.ezflytime.util;

import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerUUIDTest {

    @Test
    void createsFileWhenMissing() throws Exception {
        File tmp = Files.createTempDirectory("ezflytest").toFile();
        tmp.deleteOnExit();

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tmp);

        ServerUUID su = new ServerUUID(plugin);
        assertNotNull(su.getUuid());

        File uuidFile = new File(tmp, "server.uuid");
        assertTrue(uuidFile.exists());
        String content = Files.readString(uuidFile.toPath()).trim();
        assertEquals(su.getUuid().toString(), content);
    }

    @Test
    void handlesExistingFile() throws Exception {
        File tmp = Files.createTempDirectory("ezflytest2").toFile();
        tmp.deleteOnExit();
        File uuidFile = new File(tmp, "server.uuid");
        String id = java.util.UUID.randomUUID().toString();
        Files.writeString(uuidFile.toPath(), id);

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tmp);

        ServerUUID su = new ServerUUID(plugin);
        assertEquals(id, su.getUuid().toString());
    }
}
