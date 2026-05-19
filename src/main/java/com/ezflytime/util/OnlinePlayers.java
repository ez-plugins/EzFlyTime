package com.ezflytime.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class OnlinePlayers {

    private static final Method GET_ONLINE_PLAYERS = resolveGetOnlinePlayers();

    private OnlinePlayers() {
    }

    public static List<Player> getOnlinePlayers() {
        if (GET_ONLINE_PLAYERS == null) {
            return Collections.emptyList();
        }
        try {
            Object result = GET_ONLINE_PLAYERS.invoke(Bukkit.getServer());
            if (result instanceof Collection) {
                return new ArrayList<>((Collection<Player>) result);
            }
            if (result instanceof Player[]) {
                return Arrays.asList((Player[]) result);
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private static Method resolveGetOnlinePlayers() {
        try {
            return Bukkit.getServer().getClass().getMethod("getOnlinePlayers");
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
