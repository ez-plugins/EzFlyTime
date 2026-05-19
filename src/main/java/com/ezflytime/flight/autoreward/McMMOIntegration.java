package com.ezflytime.flight.autoreward;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class McMMOIntegration {

    private final EzFlyTimePlugin plugin;
    private final Map<String, Object> cachedSkills = new ConcurrentHashMap<>();
    private final Set<String> missingSkills = ConcurrentHashMap.newKeySet();

    private boolean available;
    private Class<? extends Enum<?>> primarySkillEnum;
    private Method getLevelMethod;

    McMMOIntegration(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
        refresh();
    }

    void refresh() {
        cachedSkills.clear();
        missingSkills.clear();
        available = false;
        primarySkillEnum = null;
        getLevelMethod = null;
        // If the mcmmo.yml explicitly disables integration, skip detection.
        try {
            com.ezflytime.bootstrap.ServiceRegistry sr = plugin.getServiceRegistry();
            com.ezflytime.config.ConfigManager cm = sr != null ? sr.getConfigManager() : null;
            com.ezflytime.mcmmo.McMMOConfig mmc = cm != null ? cm.getMcMMOConfig() : null;
            if (mmc != null && !mmc.isEnabled()) {
                plugin.getLogger().info("mcMMO integration disabled via mcmmo.yml");
                return;
            }
        } catch (Throwable ignored) {
            // ignore and continue to detection
        }

        Plugin mcMMO = plugin.getServer().getPluginManager().getPlugin("mcMMO");
        if (mcMMO == null || !mcMMO.isEnabled()) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> primarySkillEnum = (Class<? extends Enum<?>>) Class.forName("com.gmail.nossr50.datatypes.skills.PrimarySkillType");
            Method getLevelMethod = apiClass.getMethod("getLevel", Player.class, primarySkillEnum);
            this.primarySkillEnum = primarySkillEnum;
            this.getLevelMethod = getLevelMethod;
            this.available = true;
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            plugin.getLogger().warning("Unable to initialize mcMMO integration: " + ex.getMessage());
        }
    }

    boolean isAvailable() {
        return available;
    }

    int getSkillLevel(Player player, String skillName) {
        if (!available) {
            return -1;
        }

        String normalized = normalizeSkillName(skillName);
        if (missingSkills.contains(normalized)) {
            return -1;
        }

        Object skillConstant = cachedSkills.computeIfAbsent(normalized, name -> {
            if (primarySkillEnum == null) {
                return null;
            }
            try {
                return Enum.valueOf((Class) primarySkillEnum, name);
            } catch (IllegalArgumentException ex) {
                missingSkills.add(name);
                plugin.getLogger().warning("Unknown mcMMO skill '" + name + "'.");
                return null;
            }
        });

        if (skillConstant == null) {
            return -1;
        }

        try {
            Object result = getLevelMethod.invoke(null, player, skillConstant);
            if (result instanceof Number number) {
                return number.intValue();
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            plugin.getLogger().warning("Failed to retrieve mcMMO level for skill '" + normalized + "': " + ex.getMessage());
        }
        return -1;
    }

    private String normalizeSkillName(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
