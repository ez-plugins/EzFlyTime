package com.ezflytime.util;

public final class BossBarSupport {

    private BossBarSupport() {
    }

    public static boolean isSupported() {
        try {
            Class.forName("org.bukkit.boss.BossBar");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
