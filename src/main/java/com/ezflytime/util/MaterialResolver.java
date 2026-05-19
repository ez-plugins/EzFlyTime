package com.ezflytime.util;

import org.bukkit.Material;

import java.lang.reflect.Method;
import java.util.Locale;

public final class MaterialResolver {

    private static final Method MATCH_MATERIAL = resolveMatchMaterial();

    private MaterialResolver() {
    }

    public static Material resolve(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        Material resolved = null;
        // Prefer direct lookup first which is safer across implementations
        try {
            resolved = Material.getMaterial(name.trim().toUpperCase(Locale.ROOT));
        } catch (Throwable ignored) {
            resolved = null;
        }

        // Fallback to matchMaterial via reflection if direct lookup failed
        if (resolved == null && MATCH_MATERIAL != null) {
            try {
                resolved = (Material) MATCH_MATERIAL.invoke(null, name);
            } catch (Throwable ignored) {
                resolved = null;
            }
        }
        return resolved != null ? resolved : fallback;
    }

    private static Method resolveMatchMaterial() {
        try {
            return Material.class.getMethod("matchMaterial", String.class);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
