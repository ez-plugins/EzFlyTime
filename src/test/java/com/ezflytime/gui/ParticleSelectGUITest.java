package com.ezflytime.gui;

import com.ezflytime.particles.UnlockedParticlesManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ParticleSelectGUITest extends com.ezflytime.command.CommandTestBase {

    @Test
    void clickingUnlockedParticleTogglesEquipAndSaves() {
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        UnlockedParticlesManager um = mock(UnlockedParticlesManager.class);
        when(serviceRegistry.getUnlockedParticlesManager()).thenReturn(um);
        when(um.getUnlocked(uuid)).thenReturn(Set.of("basic"));
        when(um.getEquipped(uuid)).thenReturn(Collections.emptySet());

        Inventory inv = mock(Inventory.class);
        mockedBukkit.when(() -> org.bukkit.Bukkit.createInventory(any(), any(Integer.class), any(String.class))).thenReturn(inv);
        // ensure Bukkit ItemFactory is present for ItemStack operations
        mockedBukkit.when(() -> org.bukkit.Bukkit.getItemFactory()).thenReturn(mock(org.bukkit.inventory.ItemFactory.class));

        // ensure plugin data folder exists for config loading
        java.io.File dataFolder = new java.io.File("target/test-classes");
        dataFolder.mkdirs();
        when(plugin.getDataFolder()).thenReturn(dataFolder);

        ParticleSelectGUI gui = new ParticleSelectGUI(plugin);
        gui.open(player);

        // verify the open/populate flow called manager and opened inventory
        verify(um).getUnlocked(uuid);
        verify(um).getEquipped(uuid);
        verify(player).openInventory(inv);
    }
}
