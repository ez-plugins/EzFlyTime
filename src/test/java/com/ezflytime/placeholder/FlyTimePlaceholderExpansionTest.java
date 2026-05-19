package com.ezflytime.placeholder;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.flight.FlyTimeManager;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlyTimePlaceholderExpansionTest {

    @Mock
    private EzFlyTimePlugin plugin;

    @Mock
    private FlyTimeManager flyTimeManager;

    @Mock
    private com.ezflytime.bootstrap.ServiceRegistry registry;

    @Mock
    private OfflinePlayer offlinePlayer;

    private FlyTimePlaceholderExpansion expansion;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        playerUuid = UUID.randomUUID();

        when(plugin.getServiceRegistry()).thenReturn(registry);
        when(registry.getFlyTimeManager()).thenReturn(flyTimeManager);
        when(offlinePlayer.getUniqueId()).thenReturn(playerUuid);

        expansion = new FlyTimePlaceholderExpansion(plugin);
    }

    @Test
    void timeRemainingAliasReturnsFormattedTime() {
        when(flyTimeManager.getRemainingSeconds(playerUuid)).thenReturn(125);

        String result = expansion.onRequest(offlinePlayer, "time_remaining");

        assertEquals("02:05", result);
    }

    @Test
    void timeRemainingRawAliasReturnsRawSeconds() {
        when(flyTimeManager.getRemainingSeconds(playerUuid)).thenReturn(125);

        String result = expansion.onRequest(offlinePlayer, "time_remaining_raw");

        assertEquals("125", result);
    }
}
