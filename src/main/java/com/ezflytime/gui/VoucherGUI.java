package com.ezflytime.gui;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.voucher.FlyVoucher;
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

public class VoucherGUI implements Listener {

    private final EzFlyTimePlugin plugin;
    private final Map<String, VoucherDisplay> voucherDisplays = new HashMap<>();
    private String guiTitle;
    private int guiSize;
    private ItemStack backgroundItem;
    private ItemStack closeButton;
    private ItemStack infoButton;
    private int closeSlot = 26;
    private int infoSlot = 18;
    private Sound openSound, purchaseSuccessSound, purchaseFailSound, closeSound;
    private float openVolume, openPitch, purchaseSuccessVolume, purchaseSuccessPitch,
                  purchaseFailVolume, purchaseFailPitch, closeVolume, closePitch;
    private int defaultAmount, shiftAmount, maxAmount;
    private boolean confirmExpensive;
    private double confirmThreshold;
    private String currencySingular, currencyPlural;
    private boolean showCurrencySymbol;
    private String currencySymbol;

    public VoucherGUI(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        loadConfiguration();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfiguration() {
        // Save default voucher-gui.yml if it doesn't exist
        File guiConfigFile = new File(plugin.getDataFolder(), "voucher-gui.yml");
        if (!guiConfigFile.exists()) {
            plugin.saveResource("voucher-gui.yml", false);
        }

        // Load the voucher GUI configuration
        FileConfiguration config = YamlConfiguration.loadConfiguration(guiConfigFile);

        // Try to load defaults from resource if file is empty
        try (InputStream resource = plugin.getResource("voucher-gui.yml")) {
            if (resource != null && config.getKeys(false).isEmpty()) {
                config = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load voucher GUI defaults: " + e.getMessage());
        }

        // GUI Settings
        guiTitle = ChatColor.translateAlternateColorCodes('&',
            config.getString("gui.title", "&6&lFlight Shop &7- &eBuy Vouchers"));
        guiSize = config.getInt("gui.size", 27);

        // Background item
        backgroundItem = createItemStack(
            config.getString("gui.background.material", "GRAY_STAINED_GLASS_PANE"),
            config.getString("gui.background.name", "&7"),
            config.getStringList("gui.background.lore")
        );

        // Close button
        ConfigurationSection closeConfig = config.getConfigurationSection("gui.close-button");
        closeButton = createItemStack(
            closeConfig.getString("material", "BARRIER"),
            closeConfig.getString("name", "&cClose Shop"),
            closeConfig.getStringList("lore")
        );
        // Load close slot from GUI config
        closeSlot = closeConfig.getInt("slot", 26);

        // Info button
        ConfigurationSection infoConfig = config.getConfigurationSection("gui.info-button");
        infoButton = createItemStack(
            infoConfig.getString("material", "BOOK"),
            infoConfig.getString("name", "&eFlight Information"),
            infoConfig.getStringList("lore")
        );
        // Load info slot from GUI config
        infoSlot = infoConfig.getInt("slot", 18);

        // Voucher displays
        ConfigurationSection vouchersConfig = config.getConfigurationSection("vouchers");
        if (vouchersConfig != null) {
            for (String voucherId : vouchersConfig.getKeys(false)) {
                ConfigurationSection voucherConfig = vouchersConfig.getConfigurationSection(voucherId);
                if (voucherConfig != null) {
                    VoucherDisplay display = new VoucherDisplay(
                        voucherConfig.getInt("slot", 0),
                        voucherConfig.getString("material", "PAPER"),
                        voucherConfig.getInt("custom-model-data", 0),
                        voucherConfig.getString("name", "&a" + voucherId + " Voucher"),
                        voucherConfig.getStringList("lore"),
                        voucherConfig.getBoolean("glow", false)
                    );
                    voucherDisplays.put(voucherId.toLowerCase(), display);
                }
            }
        }

        // Purchase settings
        ConfigurationSection purchaseConfig = config.getConfigurationSection("purchase");
        defaultAmount = purchaseConfig.getInt("default-amount", 1);
        shiftAmount = purchaseConfig.getInt("shift-amount", 5);
        maxAmount = purchaseConfig.getInt("max-amount", 64);
        confirmExpensive = purchaseConfig.getBoolean("confirm-expensive", true);
        confirmThreshold = purchaseConfig.getDouble("confirm-threshold", 1000);

        // Sound settings
        ConfigurationSection soundsConfig = config.getConfigurationSection("sounds");
        loadSoundSetting(soundsConfig.getConfigurationSection("open"), "ui.toast.in");
        loadSoundSetting(soundsConfig.getConfigurationSection("purchase-success"), "entity.experience_orb.pickup");
        loadSoundSetting(soundsConfig.getConfigurationSection("purchase-fail"), "entity.villager.no");
        loadSoundSetting(soundsConfig.getConfigurationSection("close"), "ui.toast.out");

        // Economy settings
        ConfigurationSection economyConfig = config.getConfigurationSection("economy");
        currencySingular = economyConfig.getString("currency-singular", "coin");
        currencyPlural = economyConfig.getString("currency-plural", "coins");
        showCurrencySymbol = economyConfig.getBoolean("show-currency-symbol", false);
        currencySymbol = economyConfig.getString("currency-symbol", "$");
    }

    private void loadSoundSetting(ConfigurationSection soundConfig, String defaultSound) {
        if (soundConfig == null) return;

        String soundName = soundConfig.getString("sound", defaultSound);
        float volume = (float) soundConfig.getDouble("volume", 1.0);
        float pitch = (float) soundConfig.getDouble("pitch", 1.0);

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            if (defaultSound.equals("ui.toast.in")) {
                openSound = sound;
                openVolume = volume;
                openPitch = pitch;
            } else if (defaultSound.equals("entity.experience_orb.pickup")) {
                purchaseSuccessSound = sound;
                purchaseSuccessVolume = volume;
                purchaseSuccessPitch = pitch;
            } else if (defaultSound.equals("entity.villager.no")) {
                purchaseFailSound = sound;
                purchaseFailVolume = volume;
                purchaseFailPitch = pitch;
            } else if (defaultSound.equals("ui.toast.out")) {
                closeSound = sound;
                closeVolume = volume;
                closePitch = pitch;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }

    private ItemStack createItemStack(String materialName, String displayName, List<String> lore) {
        Material material = MaterialResolver.resolve(materialName, Material.STONE);
        if (material == Material.STONE && (materialName == null || !"STONE".equalsIgnoreCase(materialName.trim()))) {
            plugin.getLogger().warning("Invalid material: " + materialName + ", using STONE");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openGUI(Player player) {
        if (!player.hasPermission(plugin.getConfig().getString("permissions.open", "ezflytime.buy"))) {
            player.sendMessage(plugin.getMessage("messages.buy-no-permission"));
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, guiSize, guiTitle);

        // Fill background
        for (int i = 0; i < guiSize; i++) {
            inventory.setItem(i, backgroundItem.clone());
        }

        // Add close button
        inventory.setItem(closeSlot, closeButton.clone());

        // Add info button
        inventory.setItem(infoSlot, infoButton.clone());

        // Add voucher items
        for (Map.Entry<String, VoucherDisplay> entry : voucherDisplays.entrySet()) {
            String voucherId = entry.getKey();
            VoucherDisplay display = entry.getValue();
            FlyVoucher voucher = plugin.getServiceRegistry().getVoucherManager().getVoucher(voucherId);

            if (voucher != null && voucher.getPrice() >= 0) {
                ItemStack voucherItem = createVoucherItem(voucher, display);
                inventory.setItem(display.slot, voucherItem);
            }
        }

        player.openInventory(inventory);

        // Play open sound
        if (openSound != null) {
            player.playSound(player.getLocation(), openSound, openVolume, openPitch);
        }
    }

    private ItemStack createVoucherItem(FlyVoucher voucher, VoucherDisplay display) {
        ItemStack item = new ItemStack(display.material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set custom model data if specified
            if (display.customModelData > 0) {
                try {
                    // Use reflection to call setCustomModelData if available
                    java.lang.reflect.Method method = meta.getClass().getMethod("setCustomModelData", int.class);
                    method.invoke(meta, display.customModelData);
                } catch (Exception ignored) {
                    // Custom model data not supported in this version
                }
            }

            // Set display name
            String displayName = display.name.replace("{voucher}", voucher.getDisplayName());
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

            // Set lore with price information
            List<String> lore = new ArrayList<>();
            Economy econ = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getEconomy() : null;
            String currencyName = currencyPlural;

            if (econ != null) {
                currencyName = econ.currencyNamePlural();
            }

            for (String line : display.lore) {
                String processedLine = line
                    .replace("{price}", String.valueOf(voucher.getPrice()))
                    .replace("{currency}", showCurrencySymbol ? currencySymbol : currencyName);
                lore.add(ChatColor.translateAlternateColorCodes('&', processedLine));
            }

            meta.setLore(lore);

            // Add glow effect if enabled
            if (display.glow) {
                try {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } catch (Exception ignored) {
                    // Enchantments not available
                }
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(guiTitle)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Handle close button
        if (slot == closeSlot) {
            player.closeInventory();
            return;
        }

        // Handle info button (just cancel the event, no action needed)
        if (slot == infoSlot) {
            return;
        }

        // Handle voucher purchases
        for (Map.Entry<String, VoucherDisplay> entry : voucherDisplays.entrySet()) {
            VoucherDisplay display = entry.getValue();
            if (slot == display.slot) {
                String voucherId = entry.getKey();
                FlyVoucher voucher = plugin.getServiceRegistry().getVoucherManager().getVoucher(voucherId);

                if (voucher != null && voucher.getPrice() >= 0) {
                    int amount = event.isShiftClick() ? shiftAmount : defaultAmount;
                    amount = Math.min(amount, maxAmount);
                    purchaseVoucher(player, voucher, amount);
                }
                break;
            }
        }
    }

    private void purchaseVoucher(Player player, FlyVoucher voucher, int amount) {
        Economy econ = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getEconomy() : null;
        if (econ == null) {
            player.sendMessage(plugin.getMessage("messages.buy-economy-unavailable"));
            if (purchaseFailSound != null) {
                player.playSound(player.getLocation(), purchaseFailSound, purchaseFailVolume, purchaseFailPitch);
            }
            return;
        }

        double totalPrice = voucher.getPrice() * amount;

        // Check balance
        if (econ.getBalance(player) < totalPrice) {
            player.sendMessage(plugin.getMessage("messages.buy-insufficient-funds")
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{voucher}", voucher.getDisplayName())
                    .replace("{price}", String.valueOf(totalPrice))
                    .replace("{currency}", econ.currencyNamePlural()));
            if (purchaseFailSound != null) {
                player.playSound(player.getLocation(), purchaseFailSound, purchaseFailVolume, purchaseFailPitch);
            }
            return;
        }

        // Check for confirmation on expensive purchases
        if (confirmExpensive && totalPrice >= confirmThreshold &&
            !player.hasPermission(plugin.getConfig().getString("permissions.bypass-confirm", "ezflytime.buy.bypass"))) {
            // TODO: Implement confirmation dialog
            // For now, just proceed with purchase
        }

        // Process purchase
        econ.withdrawPlayer(player, totalPrice);

        // Give vouchers
        List<ItemStack> leftovers = new ArrayList<>();
        ItemStack[] items = voucher.createItems(amount);
        for (ItemStack voucherItem : items) {
            Map<Integer, ItemStack> result = player.getInventory().addItem(voucherItem);
            leftovers.addAll(result.values());
        }

        // Drop leftovers
        if (!leftovers.isEmpty()) {
            leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            int leftoverAmount = leftovers.stream().mapToInt(ItemStack::getAmount).sum();
            player.sendMessage(plugin.getMessage("messages.buy-inventory-full")
                    .replace("{amount}", String.valueOf(leftoverAmount))
                    .replace("{voucher}", voucher.getDisplayName()));
        }

        // Success message
        player.sendMessage(plugin.getMessage("messages.buy-success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{voucher}", voucher.getDisplayName())
                .replace("{price}", String.valueOf(totalPrice))
                .replace("{currency}", econ.currencyNamePlural()));

        // Play success sound
        if (purchaseSuccessSound != null) {
            player.playSound(player.getLocation(), purchaseSuccessSound, purchaseSuccessVolume, purchaseSuccessPitch);
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

    private static class VoucherDisplay {
        final int slot;
        final Material material;
        final int customModelData;
        final String name;
        final List<String> lore;
        final boolean glow;

        VoucherDisplay(int slot, String materialName, int customModelData, String name, List<String> lore, boolean glow) {
            this.slot = slot;
            this.material = MaterialResolver.resolve(materialName, Material.STONE);
            this.customModelData = customModelData;
            this.name = name;
            this.lore = lore;
            this.glow = glow;
        }
    }
}