package com.ezflytime.util;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaterialResolverTest {

    @Test
    void resolvesKnownMaterial() {
        Material m = MaterialResolver.resolve("PAPER", Material.STONE);
        assertEquals(Material.PAPER, m);
    }

    @Test
    void unknownReturnsFallback() {
        Material m = MaterialResolver.resolve("NOT_A_MATERIAL", Material.STONE);
        assertEquals(Material.STONE, m);
    }

    @Test
    void nullOrEmptyReturnsFallback() {
        assertEquals(Material.STONE, MaterialResolver.resolve(null, Material.STONE));
        assertEquals(Material.STONE, MaterialResolver.resolve("  ", Material.STONE));
    }
}
