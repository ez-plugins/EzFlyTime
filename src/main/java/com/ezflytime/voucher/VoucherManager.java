package com.ezflytime.voucher;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.VoucherStorage;
import com.ezflytime.util.Hand;
import com.ezflytime.util.InteractionHandResolver;
import com.ezflytime.util.MaterialResolver;
import com.ezflytime.util.PlayerInventoryAccessor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import com.ezflytime.util.OnlinePlayers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Queue;
import com.ezflytime.util.MoneyFormatter;

public class VoucherManager implements Listener {

    private final EzFlyTimePlugin plugin;
    private final Map<String, FlyVoucher> vouchers = new HashMap<>();
    private boolean dupeDetectionEnabled;
    private static final long RECENT_USE_WINDOW_MS = 150L;

    private final Set<String> consumedVoucherIds = new HashSet<>();
    private final VoucherStorage storage;
    private final Map<UUID, LastVoucherUse> recentMainHandUses = new HashMap<>();
    private static final String DUPLICATE_ALERT_PERMISSION = "ezflytime.notify";
    private final Queue<String> pendingDuplicateAlerts = new ArrayDeque<>();
    private final Map<UUID, Long> playerRedemptionCooldowns = new HashMap<>();
    private static final long REDEMPTION_COOLDOWN_MS = 1000L; // 1 second cooldown between redemptions

    public VoucherManager(EzFlyTimePlugin plugin, VoucherStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        consumedVoucherIds.addAll(storage.loadConsumedVoucherIds());
        reload();
    }

    public void reload() {
        vouchers.clear();
        dupeDetectionEnabled = plugin.getConfig().getBoolean("detect-voucher-dupes", true);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("vouchers");
        if (section == null) {
            plugin.getLogger().warning("No vouchers defined in config.yml");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection voucherSection = section.getConfigurationSection(id);
            if (voucherSection == null) {
                continue;
            }

            Material material = MaterialResolver.resolve(voucherSection.getString("material", "PAPER"), Material.PAPER);
            if (material == null) {
                plugin.getLogger().warning("Invalid material for voucher " + id + ". Defaulting to PAPER.");
                material = Material.PAPER;
            }
            if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
                ((EzFlyTimePlugin) plugin).debug("Loaded voucher " + id + " with material: " + material.name());
            }

            String name = voucherSection.getString("name", "Fly Voucher");
            int duration = voucherSection.getInt("duration-seconds", 300);
            java.util.List<String> lore = voucherSection.getStringList("lore");

            double price = -1;
            if (voucherSection.contains("price")) {
                Object priceObj = voucherSection.get("price");
                if (priceObj instanceof Number) {
                    price = ((Number) priceObj).doubleValue();
                } else if (priceObj instanceof String) {
                    try {
                        price = MoneyFormatter.parse((String) priceObj);
                    } catch (NumberFormatException ex) {
                        // Fallback: try to read as double (old behavior) or mark as unavailable
                        try {
                            price = voucherSection.getDouble("price");
                        } catch (Exception ignored) {
                            price = -1;
                        }
                    }
                } else {
                    try { price = voucherSection.getDouble("price"); } catch (Exception ignored) { price = -1; }
                }
            }
            FlyVoucher voucher = new FlyVoucher(plugin, id, material, name, lore, duration, price);
            vouchers.put(id.toLowerCase(), voucher);
        }
    }

    public FlyVoucher getVoucher(String id) {
        if (id == null) {
            return null;
        }
        return vouchers.get(id.toLowerCase());
    }

    public Set<String> getVoucherIds() {
        return Collections.unmodifiableSet(vouchers.keySet());
    }

    @EventHandler
    public void onVoucherUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
            ((EzFlyTimePlugin) plugin).debug("PlayerInteractEvent: " + action + " by " + event.getPlayer().getName());
        }
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
            ((EzFlyTimePlugin) plugin).debug("Right-click item: " + (item != null ? item.getType() + " x" + item.getAmount() : "null"));
        }
        if (item == null) {
            return;
        }

        FlyVoucher voucher = getVoucherFromItem(item);
        if (voucher == null) {
            if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
                ((EzFlyTimePlugin) plugin).debug("No voucher found for item");
            }
            return;
        }

        if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
            ((EzFlyTimePlugin) plugin).debug("Found voucher: " + voucher.getId());
        }

        Player player = event.getPlayer();

        // Check redemption cooldown to prevent rapid successive redemptions
        UUID playerUUID = player.getUniqueId();
        Long lastRedemption = playerRedemptionCooldowns.get(playerUUID);
        long now = System.currentTimeMillis();
        if (lastRedemption != null && (now - lastRedemption) < REDEMPTION_COOLDOWN_MS) {
            if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
                ((EzFlyTimePlugin) plugin).debug("Redemption cooldown active for player " + player.getName());
            }
            event.setCancelled(true);
            return;
        }

        String uniqueId = voucher.extractUniqueId(item);
        Hand hand = InteractionHandResolver.resolve(event);
        if (shouldIgnoreOffHand(player, hand)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (dupeDetectionEnabled && uniqueId != null && consumedVoucherIds.contains(uniqueId)) {
            player.sendMessage(plugin.getMessage("messages.voucher-duplicate")
                    .replace("{voucher}", voucher.getDisplayName()));
            notifyAdminsOfDuplicate(player, voucher);
            return;
        }
        plugin.getServiceRegistry().getFlyTimeManager().addTime(player, voucher.getDurationSeconds(), false);
        consumeItem(player, hand);
        if (uniqueId != null) {
            consumedVoucherIds.add(uniqueId);
            storage.saveConsumedVoucherIds(consumedVoucherIds);
        }
        recordRecentUse(player, hand);
        playerRedemptionCooldowns.put(playerUUID, now);
        player.sendMessage(plugin.getMessage("messages.voucher-redeemed")
                .replace("{voucher}", voucher.getDisplayName())
                .replace("{minutes}", String.valueOf(voucher.getDurationSeconds() / 60))
                .replace("{seconds}", String.valueOf(voucher.getDurationSeconds())));
    }

    private void consumeItem(Player player, Hand hand) {
        ItemStack stack = PlayerInventoryAccessor.getItem(player, hand);
        decrementItem(stack, () -> PlayerInventoryAccessor.clearItem(player, hand));
    }

    private void decrementItem(ItemStack stack, Runnable emptyAction) {
        if (stack == null) {
            return;
        }
        int amount = stack.getAmount();
        if (amount <= 1) {
            emptyAction.run();
        } else {
            stack.setAmount(amount - 1);
        }
    }

    private FlyVoucher getVoucherFromItem(ItemStack item) {
        if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
            ((EzFlyTimePlugin) plugin).debug("Checking item for voucher: " + item.getType());
        }
        if (item == null) {
            return null;
        }
        ItemStack localItem = item;
        org.bukkit.inventory.meta.ItemMeta meta = null;
        try { meta = localItem.getItemMeta(); } catch (Throwable ignored) {}

        if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
            if (meta != null) {
                String name = meta.hasDisplayName() ? meta.getDisplayName() : "<no-name>";
                String lore = meta.hasLore() ? meta.getLore().toString() : "<no-lore>";
                ((EzFlyTimePlugin) plugin).debug("Item meta: name='" + name + "', lore=" + lore);
            } else {
                ((EzFlyTimePlugin) plugin).debug("Item has no ItemMeta");
            }
        }
        if (meta == null) {
            return null;
        }
        for (FlyVoucher voucher : vouchers.values()) {
            if (voucher.matches(item, dupeDetectionEnabled)) {
                if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
                    ((EzFlyTimePlugin) plugin).debug("Item matches voucher: " + voucher.getId());
                }
                return voucher;
            }
        }
        if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
            ((EzFlyTimePlugin) plugin).debug("Item does not match any voucher");
        }
        return null;
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(DUPLICATE_ALERT_PERMISSION)) {
            return;
        }
        if (pendingDuplicateAlerts.isEmpty()) {
            return;
        }
        while (!pendingDuplicateAlerts.isEmpty()) {
            player.sendMessage(pendingDuplicateAlerts.poll());
        }
    }

    public void saveData() {
        storage.saveConsumedVoucherIds(consumedVoucherIds);
    }

    public boolean isVoucherIdConsumed(String uniqueId) {
        return uniqueId != null && consumedVoucherIds.contains(uniqueId);
    }

    private void recordRecentUse(Player player, Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            recentMainHandUses.put(player.getUniqueId(), new LastVoucherUse(System.currentTimeMillis()));
        } else {
            recentMainHandUses.remove(player.getUniqueId());
        }
    }

    private boolean shouldIgnoreOffHand(Player player, Hand hand) {
        if (hand != Hand.OFF_HAND) {
            return false;
        }
        LastVoucherUse lastUse = recentMainHandUses.get(player.getUniqueId());
        if (lastUse == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastUse.getTimestamp() > RECENT_USE_WINDOW_MS) {
            recentMainHandUses.remove(player.getUniqueId());
            return false;
        }

        recentMainHandUses.remove(player.getUniqueId());
        return true;
    }

    private void notifyAdminsOfDuplicate(Player player, FlyVoucher voucher) {
        String alertMessage = plugin.getMessage("messages.voucher-duplicate-alert")
                .replace("{player}", player.getName())
                .replace("{voucher}", voucher.getDisplayName());

        boolean delivered = false;
        for (Player onlinePlayer : OnlinePlayers.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(DUPLICATE_ALERT_PERMISSION)) {
                onlinePlayer.sendMessage(alertMessage);
                delivered = true;
            }
        }

        if (!delivered) {
            pendingDuplicateAlerts.add(alertMessage);
        }
    }

    private static class LastVoucherUse {
        private final long timestamp;

        private LastVoucherUse(long timestamp) {
            this.timestamp = timestamp;
        }

        private long getTimestamp() {
            return timestamp;
        }
    }
}
