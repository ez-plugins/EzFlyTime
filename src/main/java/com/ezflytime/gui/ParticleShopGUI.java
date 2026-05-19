package com.ezflytime.gui;

import com.ezflytime.EzFlyTimePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import com.ezflytime.util.MaterialResolver;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ParticleShopGUI implements Listener {

    private final EzFlyTimePlugin plugin;
    private final Map<Integer, String> slotToParticle = new HashMap<>();
    private String guiTitle = ChatColor.translateAlternateColorCodes('&', "&6&lParticle Shop");
    private Sound openSound, purchaseSuccessSound, purchaseFailSound, closeSound;
    private float openVolume = 1.0f, openPitch = 1.0f, purchaseSuccessVolume = 1.0f, purchaseSuccessPitch = 1.0f, purchaseFailVolume = 1.0f, purchaseFailPitch = 1.0f, closeVolume = 1.0f, closePitch = 1.0f;

    public ParticleShopGUI(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadConfiguration();
    }

    private void loadConfiguration() {
        File guiConfigFile = new File(plugin.getDataFolder(), "particle-shop-gui.yml");
        if (!guiConfigFile.exists()) {
            plugin.saveResource("particle-shop-gui.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(guiConfigFile);
        try (InputStream resource = plugin.getResource("particle-shop-gui.yml")) {
            if (resource != null && cfg.getKeys(false).isEmpty()) {
                cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {}

        guiTitle = ChatColor.translateAlternateColorCodes('&', cfg.getString("title", guiTitle));
    }

    public void open(Player player) {
        if (!player.hasPermission(plugin.getConfig().getString("permissions.particles-shop", "ezflytime.buy"))) {
            player.sendMessage(plugin.getMessage("messages.buy-no-permission"));
            return;
        }

        FileConfiguration particlesCfg = plugin.getServiceRegistry().getParticlesManager().getConfig();
        ConfigurationSection particlesSection = particlesCfg.getConfigurationSection("particles");
        if (particlesSection == null) {
            player.sendMessage(plugin.getMessage("messages.particles-none"));
            return;
        }

        List<String> keys = new ArrayList<>(particlesSection.getKeys(false));
        int count = Math.max(1, keys.size());
        int rows = (int) Math.ceil(count / 9.0);
        int size = Math.max(9, rows * 9);

        Inventory inv = Bukkit.createInventory(null, size, guiTitle);
        slotToParticle.clear();

        // Fill sequentially
        int slot = 0;
        for (String particleId : keys) {
            ConfigurationSection psec = particlesSection.getConfigurationSection(particleId);
            if (psec == null) continue;

            double price = psec.getDouble("shop.price", -1.0);
            String matName = psec.getString("shop.material", "PAPER");
            String displayName = psec.getString("shop.name", capitalize(particleId));

            Material mat = MaterialResolver.resolve(matName, Material.PAPER);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "ID: " + ChatColor.WHITE + particleId);
                if (price >= 0) {
                    Economy econ = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getEconomy() : null;
                    String priceText = econ != null ? String.valueOf(price) : String.valueOf(price);
                    lore.add(ChatColor.YELLOW + "Price: " + ChatColor.WHITE + priceText);
                } else {
                    lore.add(ChatColor.YELLOW + "Not for sale");
                }

                // unlocked status
                com.ezflytime.particles.UnlockedParticlesManager um = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getUnlockedParticlesManager() : null;
                boolean unlocked = um != null && um.isUnlocked(player.getUniqueId(), particleId);
                lore.add(unlocked ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
            slotToParticle.put(slot, particleId);
            slot++;
        }

        player.openInventory(inv);
        if (openSound != null) player.playSound(player.getLocation(), openSound, openVolume, openPitch);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() == 1) return s.toUpperCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(guiTitle)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (!slotToParticle.containsKey(slot)) return;

        String particleId = slotToParticle.get(slot);
        FileConfiguration particlesCfg = plugin.getServiceRegistry().getParticlesManager().getConfig();
        ConfigurationSection psec = particlesCfg.getConfigurationSection("particles." + particleId);
        if (psec == null) return;

        double price = psec.getDouble("shop.price", -1.0);
        com.ezflytime.particles.UnlockedParticlesManager um = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getUnlockedParticlesManager() : null;

        if (price < 0) {
            player.sendMessage(plugin.getMessage("messages.particle-not-for-sale").replace("{particle}", particleId));
            if (purchaseFailSound != null) player.playSound(player.getLocation(), purchaseFailSound, purchaseFailVolume, purchaseFailPitch);
            return;
        }

        Economy econ = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getEconomy() : null;
        if (econ == null) {
            player.sendMessage(plugin.getMessage("messages.buy-economy-unavailable"));
            if (purchaseFailSound != null) player.playSound(player.getLocation(), purchaseFailSound, purchaseFailVolume, purchaseFailPitch);
            return;
        }

        double bal = econ.getBalance(player);
        if (bal < price) {
            player.sendMessage(plugin.getMessage("messages.buy-insufficient-funds").replace("{price}", String.valueOf(price)).replace("{particle}", particleId));
            if (purchaseFailSound != null) player.playSound(player.getLocation(), purchaseFailSound, purchaseFailVolume, purchaseFailPitch);
            return;
        }

        // Withdraw and unlock
        econ.withdrawPlayer(player, price);
        boolean added = false;
        if (um != null) {
            added = um.unlock(player.getUniqueId(), particleId);
            um.saveData();
        }

        if (added) {
            player.sendMessage(plugin.getMessage("messages.particle-unlocked").replace("{particle}", particleId));
            if (purchaseSuccessSound != null) player.playSound(player.getLocation(), purchaseSuccessSound, purchaseSuccessVolume, purchaseSuccessPitch);
            // refresh GUI for updated unlocked state
            Bukkit.getScheduler().runTask(plugin, () -> open(player));
        } else {
            player.sendMessage(plugin.getMessage("messages.particle-already-unlocked").replace("{particle}", particleId));
            if (purchaseFailSound != null) player.playSound(player.getLocation(), purchaseFailSound, purchaseFailVolume, purchaseFailPitch);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(guiTitle) && closeSound != null) {
            Player player = (Player) event.getPlayer();
            player.playSound(player.getLocation(), closeSound, closeVolume, closePitch);
        }
    }

    public void reload() {
        loadConfiguration();
    }
}
