package com.ezflytime.messages;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.bootstrap.ServiceRegistry;
import com.ezflytime.config.ConfigManager;
import com.ezflytime.update.UpdateCheckResult;
import com.ezflytime.update.UpdateNotifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MultiLineMessageFeatureTest {

    private EzFlyTimePlugin plugin;
    private ServiceRegistry serviceRegistry;
    private Player player;

    @BeforeEach
    void setUp() {
        plugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);
        serviceRegistry = mock(ServiceRegistry.class, Answers.RETURNS_DEEP_STUBS);

        when(plugin.getServiceRegistry()).thenReturn(serviceRegistry);
        doCallRealMethod().when(plugin).sendMessage(any(Player.class), anyString());

        player = mock(Player.class, Answers.RETURNS_DEEP_STUBS);
        when(player.hasPermission("ezflytime.update")).thenReturn(true);
    }

    @Test
    void configManager_getMessage_convertsNewlineEscapesToActualNewlines() throws Exception {
        EzFlyTimePlugin pluginMock = mock(EzFlyTimePlugin.class);
        ConfigManager configManager = new ConfigManager(pluginMock);

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("messages.prefix", "");
        yaml.set("messages.test", "Line 1\\nLine 2\\nLine 3");

        java.lang.reflect.Field field = ConfigManager.class.getDeclaredField("messageConfig");
        field.setAccessible(true);
        field.set(configManager, yaml);

        String result = configManager.getMessage("messages.test");

        assertEquals("Line 1\nLine 2\nLine 3", result);
    }

    @Test
    void sendMessage_splitsMultiLineMessageAndSendsEachLine() {
        String multiLine = "§eLine 1\n§7Line 2\n§aLine 3";
        plugin.sendMessage(player, multiLine);

        verify(player).sendMessage("§eLine 1");
        verify(player).sendMessage("§7Line 2");
        verify(player).sendMessage("§aLine 3");
        verify(player, times(3)).sendMessage(anyString());
    }

    @Test
    void sendMessage_skipsEmptyLines() {
        String messageWithEmptyLines = "Line 1\n\nLine 2\n\n";
        plugin.sendMessage(player, messageWithEmptyLines);

        verify(player).sendMessage("Line 1");
        verify(player).sendMessage("Line 2");
        verify(player, times(2)).sendMessage(anyString());
    }

    @Test
    void sendMessage_singleLineStillWorks() {
        String singleLine = "§aSingle line message";
        plugin.sendMessage(player, singleLine);

        verify(player).sendMessage("§aSingle line message");
        verify(player, times(1)).sendMessage(anyString());
    }

    @Test
    void sendMessage_doesNothingForNullMessage() {
        plugin.sendMessage(player, null);
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void sendMessage_doesNothingForEmptyMessage() {
        plugin.sendMessage(player, "");
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void updateNotifier_sendsMultiLineUpdateMessage() {
        UpdateCheckResult result = UpdateCheckResult.updateAvailable("1.0", "2.0", "https://example.com");
        when(serviceRegistry.getUpdateCheckResult()).thenReturn(result);

        when(plugin.getMessage("messages.update-available"))
                .thenReturn("§eUpdate available:\n§7Current: {current}\n§7Latest: {latest}\n§eURL: {url}");

        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);

        UpdateNotifier notifier = new UpdateNotifier(plugin);
        notifier.onPlayerJoin(event);

        verify(player).sendMessage("§eUpdate available:");
        verify(player).sendMessage("§7Current: 1.0");
        verify(player).sendMessage("§7Latest: 2.0");
        verify(player).sendMessage("§eURL: https://example.com");
        verify(player, times(4)).sendMessage(anyString());
    }
}
