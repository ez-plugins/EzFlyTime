package com.ezflytime.voucher;

import org.bukkit.ChatColor;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

class LegacyVoucherMetadataHandler implements VoucherMetadataHandler {
    @Override
    public int readVoucherDuration(ItemMeta meta) {
        String[] data = readData(meta);
        if (data != null && data.length > 1) {
            try {
                return Integer.parseInt(data[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private static final String RAW_PREFIX = "EzFlyTime:";

    @Override
    public void apply(ItemMeta meta, String voucherId, int durationSeconds, String uniqueId) {
        if (meta == null) {
            return;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }
        String dataLine = ChatColor.DARK_GRAY + RAW_PREFIX + voucherId + ":" + durationSeconds + ":" + uniqueId;
        try {
            org.bukkit.Bukkit.getLogger().fine("[EzFlyTime] [LegacyHandler] apply called; adding dataLine='" + ChatColor.stripColor(dataLine) + "' to lore (before size=" + lore.size() + ")");
        } catch (Throwable ignored) {}
        lore.add(dataLine);
        meta.setLore(lore);
    }

    @Override
    public String readVoucherId(ItemMeta meta) {
        String[] data = readData(meta);
        String result = data != null ? data[0] : null;
        // Add logging if we can get a logger somehow - but legacy handler doesn't have plugin
        return result;
    }

    @Override
    public String readUniqueId(ItemMeta meta) {
        String[] data = readData(meta);
        return data != null ? data[2] : null;
    }

    @Override
    public String readServerUUID(ItemMeta meta) {
        // Legacy format doesn't store server UUID
        return null;
    }

    @Override
    public long readCreationTimestamp(ItemMeta meta) {
        // Legacy format doesn't store creation timestamp
        return -1;
    }

    private String[] readData(ItemMeta meta) {
        if (meta == null) {
            try { org.bukkit.Bukkit.getLogger().fine("[EzFlyTime] [LegacyHandler] readData: meta is null"); } catch (Throwable ignored) {}
            return null;
        }
        List<String> lore = null;
        try {
            lore = meta.getLore();
        } catch (Throwable ignored) {}
        if (lore == null) {
            try { org.bukkit.Bukkit.getLogger().fine("[EzFlyTime] [LegacyHandler] readData: lore==null"); } catch (Throwable ignored) {}
            return null;
        }
        for (String line : lore) {
            try { org.bukkit.Bukkit.getLogger().fine("[EzFlyTime] [LegacyHandler] readData: examining lore line='" + ChatColor.stripColor(String.valueOf(line)) + "'"); } catch (Throwable ignored) {}
            if (line == null) {
                continue;
            }
            String stripped = ChatColor.stripColor(line);
            if (stripped == null || !stripped.startsWith(RAW_PREFIX)) {
                continue;
            }
            String payload = stripped.substring(RAW_PREFIX.length());
            String[] parts = payload.split(":", 3);
            if (parts.length == 3) {
                return parts;
            }
        }
        return null;
    }
}
