package com.ezflytime.voucher;

import org.bukkit.inventory.meta.ItemMeta;

interface VoucherMetadataHandler {
    void apply(ItemMeta meta, String voucherId, int durationSeconds, String uniqueId);

    String readVoucherId(ItemMeta meta);

    String readUniqueId(ItemMeta meta);

    /**
     * Reads the voucher duration from the item meta, or returns -1 if not present.
     */
    int readVoucherDuration(ItemMeta meta);

    /**
     * Reads the server UUID from the item meta, or returns null if not present.
     */
    String readServerUUID(ItemMeta meta);

    /**
     * Reads the creation timestamp from the item meta, or returns -1 if not present.
     */
    long readCreationTimestamp(ItemMeta meta);
}
