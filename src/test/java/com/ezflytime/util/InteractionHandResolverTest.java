package com.ezflytime.util;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InteractionHandResolverTest {

    @Test
    void nullEventReturnsMain() {
        assertEquals(Hand.MAIN_HAND, InteractionHandResolver.resolve(null));
    }

    @Test
    void offHandIsRecognizedWhenPresent() throws Exception {
        PlayerInteractEvent ev = mock(PlayerInteractEvent.class);
        when(ev.getHand()).thenReturn(EquipmentSlot.OFF_HAND);

        assertEquals(Hand.OFF_HAND, InteractionHandResolver.resolve(ev));
    }

    @Test
    void mainHandIsDefault() throws Exception {
        PlayerInteractEvent ev = mock(PlayerInteractEvent.class);
        when(ev.getHand()).thenReturn(EquipmentSlot.HAND);

        assertEquals(Hand.MAIN_HAND, InteractionHandResolver.resolve(ev));
    }
}
