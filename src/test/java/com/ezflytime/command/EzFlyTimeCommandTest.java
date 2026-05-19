package com.ezflytime.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class EzFlyTimeCommandTest extends CommandTestBase {

    @Test
    void reloadSubcommandCallsReloadWhenPermitted() {
        // Arrange
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.reload")).thenReturn(true);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        EzFlyTimeCommand subject = new EzFlyTimeCommand(plugin);

        // Act
        boolean result = subject.onCommand(sender, command, "flytime", new String[]{"reload"});

        // Assert
        assert result;
        verify(plugin).reloadPluginConfiguration();
        verify(sender).sendMessage(plugin.getMessage("messages.config-reloaded"));
    }

    @Test
    void helpIsShownWhenNoArgs() {
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);
        when(command.getName()).thenReturn("ezflytime");

        EzFlyTimeCommand subject = new EzFlyTimeCommand(plugin);

        boolean result = subject.onCommand(sender, command, "ezflytime", new String[0]);

        assert result;
        verify(sender, times(4)).sendMessage(anyString());
    }

    @Test
    void topCommandShowsTopWhenPermitted() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.top")).thenReturn(true);

        com.ezflytime.flight.FlyTimeManager ftm = mock(com.ezflytime.flight.FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(ftm);

        java.util.UUID uuid = java.util.UUID.randomUUID();
        java.util.Map<java.util.UUID, Integer> map = new java.util.HashMap<>();
        map.put(uuid, 3600);
        when(ftm.snapshotRemainingSeconds()).thenReturn(map);

        org.bukkit.OfflinePlayer offline = mock(org.bukkit.OfflinePlayer.class);
        when(offline.getName()).thenReturn("alice");
        mockedBukkit.when(() -> org.bukkit.Bukkit.getOfflinePlayer(uuid)).thenReturn(offline);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("ezflytime");

        EzFlyTimeCommand subject = new EzFlyTimeCommand(plugin);

        boolean result = subject.onCommand(sender, command, "ezflytime", new String[]{"top"});

        assert result;
        verify(sender).sendMessage("messages.flytime-top-header");
        verify(sender, atLeastOnce()).sendMessage(startsWith("messages.flytime-top-entry"));
    }

    @Test
    void maxSingleToggleInvokesManagerAndNotifies() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.maxsingle.manage")).thenReturn(true);

        com.ezflytime.flight.FlyTimeManager ftm = mock(com.ezflytime.flight.FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(ftm);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("target");
        when(plugin.getServer().getPlayer("target")).thenReturn(target);

        when(ftm.toggleMaxSingleFlightBypass(target.getUniqueId())).thenReturn(true);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("ezflytime");

        EzFlyTimeCommand subject = new EzFlyTimeCommand(plugin);

        boolean result = subject.onCommand(sender, command, "ezflytime", new String[]{"maxsingle", "target"});

        assert result;
        verify(ftm).toggleMaxSingleFlightBypass(target.getUniqueId());
        verify(sender).sendMessage(anyString());
        verify(target).sendMessage(anyString());
    }
}
