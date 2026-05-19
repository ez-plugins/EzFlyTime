package com.ezflytime.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.Method;

public final class PlayerInventoryAccessor {

    private static final Method GET_MAIN_HAND = resolveMethod(PlayerInventory.class, "getItemInMainHand");
    private static final Method SET_MAIN_HAND = resolveMethod(PlayerInventory.class, "setItemInMainHand", ItemStack.class);
    private static final Method GET_OFF_HAND = resolveMethod(PlayerInventory.class, "getItemInOffHand");
    private static final Method SET_OFF_HAND = resolveMethod(PlayerInventory.class, "setItemInOffHand", ItemStack.class);

    private PlayerInventoryAccessor() {
    }

    public static ItemStack getItem(Player player, Hand hand) {
        if (player == null) {
            return null;
        }
        PlayerInventory inventory = player.getInventory();
        if (hand == Hand.OFF_HAND) {
            ItemStack offHand = invokeItemStack(GET_OFF_HAND, inventory);
            if (offHand != null) {
                return offHand;
            }
        }
        ItemStack mainHand = invokeItemStack(GET_MAIN_HAND, inventory);
        if (mainHand != null) {
            return mainHand;
        }
        return inventory.getItemInHand();
    }

    public static void clearItem(Player player, Hand hand) {
        setItem(player, hand, null);
    }

    public static void setItem(Player player, Hand hand, ItemStack item) {
        if (player == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (hand == Hand.OFF_HAND && SET_OFF_HAND != null) {
            invokeVoid(SET_OFF_HAND, inventory, item);
            return;
        }
        if (SET_MAIN_HAND != null) {
            invokeVoid(SET_MAIN_HAND, inventory, item);
            return;
        }
        inventory.setItemInHand(item);
    }

    public static boolean supportsOffHand() {
        return GET_OFF_HAND != null && SET_OFF_HAND != null;
    }

    private static Method resolveMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static ItemStack invokeItemStack(Method method, Object target) {
        if (method == null) {
            return null;
        }
        try {
            return (ItemStack) method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void invokeVoid(Method method, Object target, Object argument) {
        if (method == null) {
            return;
        }
        try {
            method.invoke(target, argument);
        } catch (Exception ignored) {
        }
    }
}
