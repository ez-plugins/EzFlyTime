package com.ezflytime.voucher;

import org.bukkit.plugin.Plugin;
import com.ezflytime.EzFlyTimePlugin;

final class VoucherMetadataHandlers {

    private VoucherMetadataHandlers() {
    }

    static VoucherMetadataHandler resolve(Plugin plugin) {
        // Create a handler per call to avoid leaking state between tests.
        VoucherMetadataHandler handler = createHandler(plugin);
        if (plugin instanceof EzFlyTimePlugin) {
            EzFlyTimePlugin ez = (EzFlyTimePlugin) plugin;
            if (ez.isDebugEnabled()) {
                ez.debug("Resolved metadata handler: " + handler.getClass().getSimpleName());
            }
        }
        return handler;
    }

    private static VoucherMetadataHandler createHandler(Plugin plugin) {
        if (plugin == null) {
            return new LegacyVoucherMetadataHandler();
        }
        try {
            // Only use PDC-based handler when running inside the actual EzFlyTimePlugin
            // to avoid requiring EzFlyTime-specific service registry in headless tests.
            Class.forName("org.bukkit.persistence.PersistentDataContainer");
            if (plugin instanceof EzFlyTimePlugin) {
                return new PdcVoucherMetadataHandler(plugin);
            } else {
                return new LegacyVoucherMetadataHandler();
            }
        } catch (Throwable ignored) {
            return new LegacyVoucherMetadataHandler();
        }
    }
}
