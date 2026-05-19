package com.ezflytime.voucher;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import com.ezflytime.EzFlyTimePlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class PdcVoucherMetadataHandler implements VoucherMetadataHandler {

    private final Plugin plugin;
    private final Method getPersistentDataContainer;
    private final Method setMethod;
    private final Method getMethod;
    private final Constructor<?> namespacedKeyConstructor;
    private final Object stringType;
    private final Object integerType;

    PdcVoucherMetadataHandler(Plugin plugin) throws Exception {
        this.plugin = plugin;
        Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
        Class<?> persistentDataContainerClass = Class.forName("org.bukkit.persistence.PersistentDataContainer");
        Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");

        this.getPersistentDataContainer = ItemMeta.class.getMethod("getPersistentDataContainer");
        this.setMethod = persistentDataContainerClass.getMethod("set", namespacedKeyClass, persistentDataTypeClass, Object.class);
        this.getMethod = persistentDataContainerClass.getMethod("get", namespacedKeyClass, persistentDataTypeClass);
        this.namespacedKeyConstructor = namespacedKeyClass.getConstructor(Plugin.class, String.class);

        Field stringField = persistentDataTypeClass.getField("STRING");
        Field integerField = persistentDataTypeClass.getField("INTEGER");
        this.stringType = stringField.get(null);
        this.integerType = integerField.get(null);
    }

    @Override
    public void apply(ItemMeta meta, String voucherId, int durationSeconds, String uniqueId) {
        if (meta == null) {
            return;
        }
        boolean pdcSucceeded = false;
        try {
            Object container = getPersistentDataContainer.invoke(meta);
            setMethod.invoke(container, createKey("ezflytime.voucher.id"), stringType, voucherId);
            setMethod.invoke(container, createKey("ezflytime.voucher.duration"), integerType, durationSeconds);
            setMethod.invoke(container, createKey("ezflytime.voucher.unique_id"), stringType, uniqueId);
            setMethod.invoke(container, createKey("ezflytime.voucher.server_uuid"), stringType, ((EzFlyTimePlugin) plugin).getServiceRegistry().getServerUUID().toString());
            setMethod.invoke(container, createKey("ezflytime.voucher.created_at"), integerType, (int) (System.currentTimeMillis() / 1000L));
            pdcSucceeded = true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply PDC metadata: " + e.getMessage());
        }

        // Always also write legacy lore so headless tests and older servers
        // can reliably observe voucher metadata. This avoids relying on the
        // PDC implementation details in test environments.
        try {
            new LegacyVoucherMetadataHandler().apply(meta, voucherId, durationSeconds, uniqueId);
        } catch (Throwable t) {
            // swallow
        }
    }

    @Override
    public String readVoucherId(ItemMeta meta) {
        if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
            ((EzFlyTimePlugin) plugin).debug("PDC readVoucherId called");
        }
        Object value = read(meta, "ezflytime.voucher.id", stringType);
        String result = value != null ? value.toString() : null;
        if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
            ((EzFlyTimePlugin) plugin).debug("PDC readVoucherId result: " + result);
        }
        return result;
    }

    @Override
    public int readVoucherDuration(ItemMeta meta) {
        Object value = read(meta, "ezflytime.voucher.duration", integerType);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return -1;
    }

    @Override
    public String readUniqueId(ItemMeta meta) {
        Object value = read(meta, "ezflytime.voucher.unique_id", stringType);
        return value != null ? value.toString() : null;
    }

    @Override
    public String readServerUUID(ItemMeta meta) {
        Object value = read(meta, "ezflytime.voucher.server_uuid", stringType);
        return value != null ? value.toString() : null;
    }

    @Override
    public long readCreationTimestamp(ItemMeta meta) {
        Object value = read(meta, "ezflytime.voucher.created_at", integerType);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return -1;
    }

    private Object read(ItemMeta meta, String key, Object type) {
        if (meta == null) {
            return null;
        }
        try {
            Object container = getPersistentDataContainer.invoke(meta);
            Object result = getMethod.invoke(container, createKey(key), type);
            if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
                ((EzFlyTimePlugin) plugin).debug("PDC read " + key + " result: " + result);
            }
            return result;
        } catch (Exception e) {
            if (((EzFlyTimePlugin) plugin).isDebugEnabled()) {
                ((EzFlyTimePlugin) plugin).debug("PDC read failed for " + key + ": " + e.getMessage());
            }
            return null;
        }
    }

    private Object createKey(String value) throws Exception {
        return namespacedKeyConstructor.newInstance(plugin, value);
    }
}
