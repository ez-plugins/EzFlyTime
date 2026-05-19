package com.ezflytime.gui;

import com.ezflytime.bootstrap.ServiceRegistry;
import com.ezflytime.particles.UnlockedParticlesManager;
import com.ezflytime.util.MaterialResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ParticleShopGUITest extends com.ezflytime.command.CommandTestBase {

    @Test
    void purchaseInsufficientFundsShowsMessageAndDoesNotUnlock() {
        // Arrange
        Player player = mock(Player.class);
        when(player.hasPermission("ezflytime.buy")).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        // Particles config
        com.ezflytime.particles.ParticlesManager pm = mock(com.ezflytime.particles.ParticlesManager.class);
        when(serviceRegistry.getParticlesManager()).thenReturn(pm);
        // load sample particles.yml from resources
        InputStream in = getClass().getClassLoader().getResourceAsStream("particles.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in));
        when(pm.getConfig()).thenReturn(cfg);

        // Unlocked manager: locked initially
        UnlockedParticlesManager um = mock(UnlockedParticlesManager.class);
        when(serviceRegistry.getUnlockedParticlesManager()).thenReturn(um);
        when(um.isUnlocked(any(UUID.class), eq("basic"))).thenReturn(false);

        // Economy with insufficient funds
        net.milkbowl.vault.economy.Economy econ = mock(net.milkbowl.vault.economy.Economy.class);
        when(serviceRegistry.getEconomy()).thenReturn(econ);
        when(econ.getBalance(player)).thenReturn(0.0);

        // Mock inventory and scheduler via Bukkit static (available from CommandTestBase)
        Inventory inv = mock(Inventory.class);
        mockedBukkit.when(() -> org.bukkit.Bukkit.createInventory(any(), any(Integer.class), any(String.class))).thenReturn(inv);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(invoc -> {
            Runnable r = invoc.getArgument(1);
            r.run();
            return mock(BukkitTask.class);
        });
        mockedBukkit.when(() -> org.bukkit.Bukkit.getScheduler()).thenReturn(scheduler);

        // ensure plugin data folder exists for config loading
        java.io.File dataFolder = new java.io.File("target/test-classes");
        dataFolder.mkdirs();
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getConfig().getString("permissions.particles-shop", "ezflytime.buy")).thenReturn("ezflytime.buy");
        // mock item factory to avoid NPE when creating ItemStacks
        mockedBukkit.when(() -> org.bukkit.Bukkit.getItemFactory()).thenReturn(mock(org.bukkit.inventory.ItemFactory.class));
        mockedBukkit.when(() -> org.bukkit.Bukkit.getLogger()).thenReturn(java.util.logging.Logger.getLogger("TestLogger"));

        ParticleShopGUI gui = new ParticleShopGUI(plugin);

        // Act: open GUI (populates internal slot mapping)
        gui.open(player);

        // Simulate click on the slot that contains 'basic'
        int basicSlot = new java.util.ArrayList<>(cfg.getConfigurationSection("particles").getKeys(false)).indexOf("basic");
        InventoryClickEvent evt = mock(InventoryClickEvent.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getTitle()).thenReturn("Particle Shop");
        when(evt.getView()).thenReturn(view);
        when(evt.getWhoClicked()).thenReturn(player);
        when(evt.getRawSlot()).thenReturn(basicSlot);

        gui.onInventoryClick(evt);

        // Assert
        verify(econ).getBalance(player);
        verify(um, never()).unlock(any(UUID.class), eq("basic"));
        verify(player).sendMessage(contains("You need"));
    }

    @Test
    void purchaseSuccessWithdrawsAndUnlocks() {
        // Arrange
        Player player = mock(Player.class);
        when(player.hasPermission("ezflytime.buy")).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        com.ezflytime.particles.ParticlesManager pm = mock(com.ezflytime.particles.ParticlesManager.class);
        when(serviceRegistry.getParticlesManager()).thenReturn(pm);
        InputStream in = getClass().getClassLoader().getResourceAsStream("particles.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in));
        when(pm.getConfig()).thenReturn(cfg);

        UnlockedParticlesManager um = mock(UnlockedParticlesManager.class);
        when(serviceRegistry.getUnlockedParticlesManager()).thenReturn(um);
        when(um.isUnlocked(uuid, "basic")).thenReturn(false);
        when(um.unlock(uuid, "basic")).thenReturn(true);

        net.milkbowl.vault.economy.Economy econ = mock(net.milkbowl.vault.economy.Economy.class);
        when(serviceRegistry.getEconomy()).thenReturn(econ);
        when(econ.getBalance(player)).thenReturn(1000.0);
        when(econ.currencyNamePlural()).thenReturn("coins");

        Inventory inv = mock(Inventory.class);
        mockedBukkit.when(() -> org.bukkit.Bukkit.createInventory(any(), any(Integer.class), any(String.class))).thenReturn(inv);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(invoc -> {
            Runnable r = invoc.getArgument(1);
            r.run();
            return mock(BukkitTask.class);
        });
        mockedBukkit.when(() -> org.bukkit.Bukkit.getScheduler()).thenReturn(scheduler);

        // ensure plugin data folder and bukkit helpers for config loading in constructor
        java.io.File dataFolder2 = new java.io.File("target/test-classes");
        dataFolder2.mkdirs();
        when(plugin.getDataFolder()).thenReturn(dataFolder2);
        when(plugin.getConfig().getString("permissions.particles-shop", "ezflytime.buy")).thenReturn("ezflytime.buy");
        mockedBukkit.when(() -> org.bukkit.Bukkit.getItemFactory()).thenReturn(mock(org.bukkit.inventory.ItemFactory.class));
        mockedBukkit.when(() -> org.bukkit.Bukkit.getLogger()).thenReturn(java.util.logging.Logger.getLogger("TestLogger"));

        ParticleShopGUI gui = new ParticleShopGUI(plugin);
        gui.open(player);

        int basicSlot2 = new java.util.ArrayList<>(cfg.getConfigurationSection("particles").getKeys(false)).indexOf("basic");
        InventoryClickEvent evt = mock(InventoryClickEvent.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getTitle()).thenReturn("Particle Shop");
        when(evt.getView()).thenReturn(view);
        when(evt.getWhoClicked()).thenReturn(player);
        when(evt.getRawSlot()).thenReturn(basicSlot2);

        gui.onInventoryClick(evt);

        // Assert
        verify(econ).withdrawPlayer(player, 100.0);
        verify(um).unlock(uuid, "basic");
        verify(player).sendMessage(contains("Unlocked"));
    }
}
