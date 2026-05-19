package com.ezflytime.util;

import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.Method;

public final class InteractionHandResolver {

    private static final Method GET_HAND_METHOD = resolveGetHandMethod();

    private InteractionHandResolver() {
    }

    public static Hand resolve(PlayerInteractEvent event) {
        if (GET_HAND_METHOD == null || event == null) {
            return Hand.MAIN_HAND;
        }
        try {
            Object hand = GET_HAND_METHOD.invoke(event);
            if (hand != null && "OFF_HAND".equalsIgnoreCase(hand.toString())) {
                return Hand.OFF_HAND;
            }
        } catch (Exception ignored) {
        }
        return Hand.MAIN_HAND;
    }

    private static Method resolveGetHandMethod() {
        try {
            return PlayerInteractEvent.class.getMethod("getHand");
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
