package com.ezflytime.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class FlightSupport {

    private static final Method IS_GLIDING = resolveMethod(Player.class, "isGliding");
    private static final Method SET_GLIDING = resolveMethod(Player.class, "setGliding", boolean.class);

    private FlightSupport() {
    }

    public static boolean supportsGliding() {
        return IS_GLIDING != null && SET_GLIDING != null;
    }

    public static boolean isGliding(Player player) {
        if (IS_GLIDING == null || player == null) {
            return false;
        }
        try {
            return (boolean) IS_GLIDING.invoke(player);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void setGliding(Player player, boolean gliding) {
        if (SET_GLIDING == null || player == null) {
            return;
        }
        try {
            SET_GLIDING.invoke(player, gliding);
        } catch (Exception ignored) {
        }
    }

    public static boolean supportsElytra() {
        return Material.getMaterial("ELYTRA") != null;
    }

    private static Method resolveMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
