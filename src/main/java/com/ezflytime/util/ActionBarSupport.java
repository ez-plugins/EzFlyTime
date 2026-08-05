package com.ezflytime.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;

public class ActionBarSupport {

    private static boolean supported;
    private static Constructor<?> packetConstructor;
    private static Class<?> chatMessageTypeClass;

    static {
        try {
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.CraftPlayer");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server.PlayerEntity");
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server.PlayerConnection");
            Class<?> packetClass = Class.forName("net.minecraft.server.Packet");
            Class<?> chatBaseComponentClass = Class.forName("net.minecraft.server.IChatBaseComponent");
            Class<?> chatComponentTextClass = Class.forName("net.minecraft.server.ChatComponentText");
            chatMessageTypeClass = Class.forName("net.minecraft.server.ChatMessageType");

            packetConstructor = Class.forName("net.minecraft.server.PacketPlayOutChat")
                    .getConstructor(chatBaseComponentClass, chatMessageTypeClass);

            supported = true;
        } catch (Exception ex) {
            supported = false;
        }
    }

    public static boolean isSupported() {
        return supported;
    }

    public static void sendActionBar(Player player, String message) {
        if (!supported || player == null || !player.isOnline()) {
            return;
        }
        try {
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object entityPlayer = craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);

            Class<?> chatComponentTextClass = Class.forName("net.minecraft.server.ChatComponentText");
            Class<?> chatBaseComponentClass = Class.forName("net.minecraft.server.IChatBaseComponent");
            Object chatComponent = chatComponentTextClass.getConstructor(String.class).newInstance(message);

            Object gameInfo = Enum.valueOf((Class<Enum>) chatMessageTypeClass, "GAME_INFO");
            Object packet = packetConstructor.newInstance(chatComponent, gameInfo);

            playerConnection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server.Packet")).invoke(playerConnection, packet);
        } catch (Exception ex) {
            // Silent fail
        }
    }
}
