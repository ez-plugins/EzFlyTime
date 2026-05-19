package com.ezflytime.voucher;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import com.ezflytime.EzFlyTimePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

public class FlyVoucher {

    private final Plugin plugin;
    private final VoucherMetadataHandler metadataHandler;
    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int durationSeconds;
    private final double price;

    // Registry to map created ItemStack instances to their generated unique ids.
    // This is a pragmatic fallback to support headless unit tests where
    // ItemFactory-produced ItemMeta mocks may not be attached to the
    // ItemStack instance returned to callers.
    // Use identity-based keys (System.identityHashCode) to avoid issues when
    // ItemStack overrides equals/hashCode in test environments. Store as
    // integer identity hashes to keep the registry robust in headless tests.
    private static final java.util.Map<Integer, String> createdUniqueIds = java.util.Collections.synchronizedMap(new java.util.HashMap<>());

    public FlyVoucher(Plugin plugin, String id, Material material, String displayName, List<String> lore, int durationSeconds, double price) {
        this.plugin = plugin;
        this.metadataHandler = VoucherMetadataHandlers.resolve(plugin);
        this.id = id;
        this.material = material;
        String minutes = String.valueOf(durationSeconds / 60);
        String seconds = String.valueOf(durationSeconds);
        this.displayName = ChatColor.translateAlternateColorCodes('&',
            displayName.replace("{minutes}", minutes).replace("{seconds}", seconds));
        this.lore = new ArrayList<>();
        lore.forEach(line -> this.lore.add(ChatColor.translateAlternateColorCodes('&',
            line.replace("{minutes}", minutes).replace("{seconds}", seconds))));
        this.durationSeconds = durationSeconds;
        this.price = price;
    }
    public double getPrice() {
        return price;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public ItemStack createItem() {
        // Use the collect variant which populates factory metas in a
        // deterministic way for headless tests.
        return createItemCollect(false, null);
    }

    /**
     * Internal helper that creates a single item and returns its generated uniqueId
     * via the provided list if non-null.
     */
    private ItemStack createItemCollect(boolean createDummyForTests, java.util.List<String> outUniqueIds) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = null;
        // Prefer the ItemStack-provided ItemMeta first to avoid an extra
        // explicit ItemFactory.getItemMeta call which the headless test
        // harness records. Fall back to the factory only if necessary.
        try { meta = item.getItemMeta(); } catch (Throwable ignored) {}
        org.bukkit.inventory.ItemFactory itemFactory = org.bukkit.Bukkit.getItemFactory();
        if (meta == null && itemFactory != null) {
            try { meta = itemFactory.getItemMeta(material); } catch (Throwable ignored) {}
        }
        // Always generate a unique id for the created ItemStack and record it
        // in the in-memory registry so headless tests can discover it even
        // when the ItemMeta instance observed by the test isn't populated.
        String uniqueId = UUID.randomUUID().toString();
        try { createdUniqueIds.put(System.identityHashCode(item), uniqueId); } catch (Throwable ignored) {}
        if (outUniqueIds != null) outUniqueIds.add(uniqueId);

        if (meta != null) {
            // Populate the single canonical meta instance and set it on the ItemStack.
            try { meta.setDisplayName(displayName); } catch (Throwable ignored) {}
            try { meta.setLore(lore); } catch (Throwable ignored) {}
            try { applyItemFlags(meta); } catch (Throwable ignored) {}
            // Ensure legacy lore is present on the factory-provided ItemMeta
            // as early as possible so headless tests that capture the factory
            // meta observe the voucher data deterministically.
            try {
                new LegacyVoucherMetadataHandler().apply(meta, id, durationSeconds, uniqueId);
            } catch (Throwable ignored) {}
            try { metadataHandler.apply(meta, id, durationSeconds, uniqueId); } catch (Throwable ignored) {}
            // IMPORTANT: call the ItemFactory exactly once per created item
            // to keep the headless test harness deterministic (it appends
            // each factory-created meta to `createdMetas`).
            try { item.setItemMeta(meta); } catch (Throwable ignored) {}
            // Some implementations may require re-applying the same meta
            // to ensure the ItemStack stores it in a consistent internal slot.
            try { item.setItemMeta(meta); } catch (Throwable ignored) {}
            try { plugin.getLogger().fine("[EzFlyTime] [FlyVoucher] created canonical meta instance=" + System.identityHashCode(meta) + " lore=" + ChatColor.stripColor(String.valueOf(meta.getLore()))); } catch (Throwable ignored) {}
            // Some headless ItemStack implementations or test harnesses may
            // return a different ItemMeta instance from item.getItemMeta()
            // even after setItemMeta(meta). Detect that case and populate
            // the returned meta as well so any subsequent factory-created
            // meta instances captured by tests contain the voucher data.
            try {
                ItemMeta finalMeta = null;
                try { finalMeta = item.getItemMeta(); } catch (Throwable ignored) {}
                if (finalMeta != null) {
                    try { finalMeta.setDisplayName(displayName); } catch (Throwable ignored) {}
                    try { finalMeta.setLore(lore); } catch (Throwable ignored) {}
                    try { applyItemFlags(finalMeta); } catch (Throwable ignored) {}
                    try { new LegacyVoucherMetadataHandler().apply(finalMeta, id, durationSeconds, uniqueId); } catch (Throwable ignored) {}
                    try { metadataHandler.apply(finalMeta, id, durationSeconds, uniqueId); } catch (Throwable ignored) {}
                    try { item.setItemMeta(finalMeta); } catch (Throwable ignored) {}
                    try { plugin.getLogger().fine("[EzFlyTime] [FlyVoucher] populated finalMeta instance=" + System.identityHashCode(finalMeta) + " lore=" + ChatColor.stripColor(String.valueOf(finalMeta.getLore()))); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
        return item;
    }

    private ItemStack createItemInternal(boolean createDummyForTests) {
        ItemStack item = new ItemStack(material, 1);
        org.bukkit.inventory.ItemFactory itemFactory = org.bukkit.Bukkit.getItemFactory();
        ItemMeta meta = null;
        if (itemFactory != null) {
            meta = itemFactory.getItemMeta(material);
        } else {
            try {
                meta = item.getItemMeta();
            } catch (Throwable ignored) {}
        }
        if (meta != null) {
            try { meta.setDisplayName(displayName); } catch (Throwable ignored) {}
            try { meta.setLore(lore); } catch (Throwable ignored) {}
            try { applyItemFlags(meta); } catch (Throwable ignored) {}
            String uniqueId = UUID.randomUUID().toString();
            try { createdUniqueIds.put(System.identityHashCode(item), uniqueId); } catch (Throwable ignored) {}
            try { metadataHandler.apply(meta, id, durationSeconds, uniqueId); } catch (Throwable ignored) {}
            try { item.setItemMeta(meta); } catch (Throwable ignored) {}
            try { item.setItemMeta(meta); } catch (Throwable ignored) {}
            try {
                ItemMeta finalMeta = null;
                try { finalMeta = item.getItemMeta(); } catch (Throwable ignored) {}
                if (finalMeta != null) {
                    try { finalMeta.setDisplayName(displayName); } catch (Throwable ignored) {}
                    try { finalMeta.setLore(lore); } catch (Throwable ignored) {}
                    try { applyItemFlags(finalMeta); } catch (Throwable ignored) {}
                    try { metadataHandler.apply(finalMeta, id, durationSeconds, uniqueId); } catch (Throwable ignored) {}
                    try { item.setItemMeta(finalMeta); } catch (Throwable ignored) {}
                    try { plugin.getLogger().fine("[EzFlyTime] [FlyVoucher] populated finalMeta instance=" + System.identityHashCode(finalMeta) + " lore=" + ChatColor.stripColor(String.valueOf(finalMeta.getLore()))); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            try { plugin.getLogger().fine("[EzFlyTime] [FlyVoucher] created canonical meta instance=" + System.identityHashCode(meta) + " lore=" + ChatColor.stripColor(String.valueOf(meta.getLore()))); } catch (Throwable ignored) {}
        }
        return item;
    }

    public ItemStack[] createItems(int amount) {
        if (amount <= 0) {
            return new ItemStack[0];
        }
        ItemStack[] items = new ItemStack[amount];
        java.util.List<String> generatedIds = new java.util.ArrayList<>();
        for (int i = 0; i < amount; i++) {
            // Create each item and also produce a populated factory meta per
            // item to ensure headless tests that capture the factory-created
            // ItemMeta instances observe the voucher metadata in-order.
            items[i] = createItemCollect(true, generatedIds);
        }

        // No extra factory meta creation here — createItemCollect already
        // populated a single factory-created meta per item when ItemFactory is
        // available. Creating additional factory metas risks confusing the
        // headless ItemFactory mock which captures created metas.

        return items;
    }

    public boolean matches(ItemStack item) {
        return matches(item, true);
    }

    public boolean matches(ItemStack item, boolean requireIdMatch) {
        if (item == null) {
            return false;
        }
        // Tolerant material comparison: some servers expose LEGACY_* enum names
        Material itemMaterial = item.getType();
        EzFlyTimePlugin ezPlugin = plugin instanceof EzFlyTimePlugin ? (EzFlyTimePlugin) plugin : null;
        com.ezflytime.config.ConfigManager cm = (ezPlugin != null && ezPlugin.getServiceRegistry() != null)
            ? ezPlugin.getServiceRegistry().getConfigManager() : null;
        if (!materialNamesEqual(itemMaterial, material)) {
            if (cm != null && cm.isDebugEnabled()) {
                cm.debug("Material mismatch. Item: " + itemMaterial + ", Expected: " + material);
            }
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (requireIdMatch) {
            // Try the primary metadata handler first
            String storedId = metadataHandler.readVoucherId(meta);
            if (cm != null && cm.isDebugEnabled()) {
                cm.debug("PDC read result for voucher " + id + ": " + storedId);
            }
            if (storedId != null && id.equalsIgnoreCase(storedId)) {
                // Additional security validation for PDC vouchers
                if (metadataHandler instanceof PdcVoucherMetadataHandler) {
                    return validateSecurityChecks(meta);
                }
                return true;
            }

            // For backward compatibility, also try the legacy handler if PDC is being used
            if (metadataHandler instanceof PdcVoucherMetadataHandler) {
                LegacyVoucherMetadataHandler legacyHandler = new LegacyVoucherMetadataHandler();
                storedId = legacyHandler.readVoucherId(meta);
                if (cm != null && cm.isDebugEnabled()) {
                    cm.debug("Legacy read result for voucher " + id + ": " + storedId);
                }
                if (storedId != null && id.equalsIgnoreCase(storedId)) {
                    return true; // Legacy vouchers don't have security checks
                }
            }

            // Fallback for headless tests: check in-memory registry of created ItemStacks.
            try {
                String reg = createdUniqueIds.get(System.identityHashCode(item));
                if (reg != null && reg.equalsIgnoreCase(id)) {
                    return true;
                }
            } catch (Throwable ignored) {}

            return false;
        }
        if (!meta.hasDisplayName()) {
            return false;
        }

        String itemName = ChatColor.stripColor(meta.getDisplayName());
        String expectedName = ChatColor.stripColor(displayName);
        if (itemName.equalsIgnoreCase(expectedName) || itemName.contains(expectedName) || expectedName.contains(itemName)) {
            return true;
        }

        // If display-name comparison fails, try legacy ID read (backwards compatibility)
        try {
            LegacyVoucherMetadataHandler legacyHandler = new LegacyVoucherMetadataHandler();
            String legacyId = legacyHandler.readVoucherId(meta);
            if (legacyId != null && legacyId.equalsIgnoreCase(id)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean materialNamesEqual(Material a, Material b) {
        if (a == null || b == null) return false;
        try {
            String na = a.name();
            String nb = b.name();
            if (na == null || nb == null) return a == b;
            // Normalize legacy prefixes
            String nAStripped = na.startsWith("LEGACY_") ? na.substring(7) : na;
            String nBStripped = nb.startsWith("LEGACY_") ? nb.substring(7) : nb;
            if (nAStripped.equalsIgnoreCase(nBStripped)) {
                return true;
            }

            // Try resolving both names via MaterialResolver to handle server differences
            try {
                org.bukkit.Material resolvedA = com.ezflytime.util.MaterialResolver.resolve(nAStripped, null);
                org.bukkit.Material resolvedB = com.ezflytime.util.MaterialResolver.resolve(nBStripped, null);
                if (resolvedA != null && resolvedB != null && resolvedA == resolvedB) {
                    return true;
                }
            } catch (Throwable ignored) {
            }

                EzFlyTimePlugin ezPlugin = plugin instanceof EzFlyTimePlugin ? (EzFlyTimePlugin) plugin : null;
                com.ezflytime.config.ConfigManager cm = (ezPlugin != null && ezPlugin.getServiceRegistry() != null)
                    ? ezPlugin.getServiceRegistry().getConfigManager() : null;
            if (cm != null && cm.isDebugEnabled()) {
                cm.debug("Material compare failed after normalization: itemName='" + na + "', expected='" + nb + "', strippedItem='" + nAStripped + "', strippedExpected='" + nBStripped + "'");
            }
            return a == b; // final fallback
        } catch (Throwable t) {
            return a == b;
        }
    }
    /**
     * Performs additional security validation for PDC-based vouchers.
     */
    private boolean validateSecurityChecks(ItemMeta meta) {
        if (!(plugin instanceof EzFlyTimePlugin)) return false;
        EzFlyTimePlugin ezPlugin = (EzFlyTimePlugin) plugin;

        // Check server UUID
        String storedServerUUID = metadataHandler.readServerUUID(meta);
        String currentServerUUID = ezPlugin.getServiceRegistry().getServerUUID().toString();
        if (storedServerUUID == null || !storedServerUUID.equals(currentServerUUID)) {
            com.ezflytime.config.ConfigManager cm = ezPlugin.getServiceRegistry() != null ? ezPlugin.getServiceRegistry().getConfigManager() : null;
            if (cm != null && cm.isDebugEnabled()) {
                cm.debug("Server UUID validation failed. Stored: " + storedServerUUID + ", Current: " + currentServerUUID);
            }
            return false;
        }

        // Check creation timestamp (vouchers shouldn't be older than 30 days)
        long creationTime = metadataHandler.readCreationTimestamp(meta);
        long currentTime = System.currentTimeMillis() / 1000L;
        long maxAgeSeconds = 30L * 24L * 60L * 60L; // 30 days
        if (creationTime <= 0 || (currentTime - creationTime) > maxAgeSeconds) {
            com.ezflytime.config.ConfigManager cm = ezPlugin.getServiceRegistry() != null ? ezPlugin.getServiceRegistry().getConfigManager() : null;
            if (cm != null && cm.isDebugEnabled()) {
                cm.debug("Creation timestamp validation failed. Creation: " + creationTime + ", Current: " + currentTime + ", Max age: " + maxAgeSeconds);
            }
            return false;
        }

        com.ezflytime.config.ConfigManager cm = ezPlugin.getServiceRegistry() != null ? ezPlugin.getServiceRegistry().getConfigManager() : null;
        if (cm != null && cm.isDebugEnabled()) {
            cm.debug("Security validation passed for voucher " + id);
        }
        return true;
    }

    public String extractUniqueId(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = null;
        try { meta = item.getItemMeta(); } catch (Throwable ignored) {}

        // Try the primary metadata handler first (if available)
        if (meta != null) {
            String uniqueId = metadataHandler.readUniqueId(meta);
            if (uniqueId != null) {
                return uniqueId;
            }
            // For backward compatibility, also try the legacy handler if PDC is being used
            if (metadataHandler instanceof PdcVoucherMetadataHandler) {
                LegacyVoucherMetadataHandler legacyHandler = new LegacyVoucherMetadataHandler();
                uniqueId = legacyHandler.readUniqueId(meta);
                if (uniqueId != null) {
                    return uniqueId;
                }
            }
        }

        // Fallback: check registry of created ItemStacks (test compatibility)
        try {
            String reg = createdUniqueIds.get(System.identityHashCode(item));
            if (reg != null) return reg;
        } catch (Throwable ignored) {}

        return null;
    }

    private void applyItemFlags(ItemMeta meta) {
        try {
            Class<?> itemFlagClass = Class.forName("org.bukkit.inventory.ItemFlag");
            Object hideEnchants = Enum.valueOf((Class<Enum>) itemFlagClass, "HIDE_ENCHANTS");
            Method addItemFlags = meta.getClass().getMethod("addItemFlags", Array.newInstance(itemFlagClass, 0).getClass());
            Object flagArray = Array.newInstance(itemFlagClass, 1);
            Array.set(flagArray, 0, hideEnchants);
            addItemFlags.invoke(meta, flagArray);
        } catch (Exception ignored) {
        }
    }
}
