package com.ezflytime.gui;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import com.ezflytime.util.MaterialResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ParticleSelectGUI implements Listener {

    private final EzFlyTimePlugin plugin;
    private String title = ChatColor.translateAlternateColorCodes('&', "&6&lSelect Particles");
    private final Map<Integer, String> slotToParticle = new HashMap<>();
    private Sound closeSound;

    public ParticleSelectGUI(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        com.ezflytime.particles.UnlockedParticlesManager um = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getUnlockedParticlesManager() : null;
        if (um == null) {
            player.sendMessage(plugin.getMessage("messages.particles-error"));
            return;
        }

        Set<String> unlocked = um.getUnlocked(player.getUniqueId());
        List<String> list = new ArrayList<>(unlocked);
        int count = Math.max(1, list.size());
        int rows = (int) Math.ceil(count / 9.0);
        int size = Math.max(9, rows * 9);

        Inventory inv = Bukkit.createInventory(null, size, title);
        slotToParticle.clear();
        int slot = 0;
        Set<String> equipped = um.getEquipped(player.getUniqueId());
        for (String pid : list) {
            Material mat = MaterialResolver.resolve("PAPER", Material.PAPER);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.WHITE + pid);
                List<String> lore = new ArrayList<>();
                lore.add(equipped.contains(pid) ? ChatColor.GREEN + "Equipped" : ChatColor.GRAY + "Click to equip/unequip");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slotToParticle.put(slot, pid);
            slot++;
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(title)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (!slotToParticle.containsKey(slot)) return;
        String pid = slotToParticle.get(slot);
        com.ezflytime.particles.UnlockedParticlesManager um = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getUnlockedParticlesManager() : null;
        if (um == null) return;
        boolean changed = um.toggleEquipped(player.getUniqueId(), pid);
        um.saveData();
        if (changed) {
            player.sendMessage(plugin.getMessage("messages.particle-equipped").replace("{particle}", pid));
        } else {
            player.sendMessage(plugin.getMessage("messages.particle-unequipped").replace("{particle}", pid));
        }
        // refresh GUI
        Bukkit.getScheduler().runTask(plugin, () -> open(player));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(title) && closeSound != null) {
            Player player = (Player) event.getPlayer();
            player.playSound(player.getLocation(), closeSound, 1.0f, 1.0f);
        }
    }

    public void reload() {
        // noop for now
    }
}

