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
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.reload")).thenReturn(true);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        EzFlyTimeCommand subject = new EzFlyTimeCommand(plugin);

        boolean result = subject.onCommand(sender, command, "flytime", new String[]{"reload"});

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
        verify(sender, times(5)).sendMessage(anyString());
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

    @Test
    void infoCommandShowsPlayerInfoWhenPermitted() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.info")).thenReturn(true);

        com.ezflytime.flight.FlyTimeManager ftm = mock(com.ezflytime.flight.FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(ftm);
        when(ftm.getRemainingSeconds(any(org.bukkit.entity.Player.class))).thenReturn(900);
        java.util.Map<java.util.UUID, Boolean> activeStates = new java.util.HashMap<>();
        java.util.UUID targetUuid = java.util.UUID.randomUUID();
        activeStates.put(targetUuid, true);
        when(ftm.snapshotActiveStates()).thenReturn(activeStates);
        when(ftm.hasMaxSingleFlightBypass(any(org.bukkit.entity.Player.class))).thenReturn(false);

        com.ezflytime.voucher.VoucherManager vm = mock(com.ezflytime.voucher.VoucherManager.class);
        when(serviceRegistry.getVoucherManager()).thenReturn(vm);
        com.ezflytime.voucher.FlyVoucher voucher = mock(com.ezflytime.voucher.FlyVoucher.class);
        when(voucher.getDisplayName()).thenReturn("&a15min Voucher");
        when(voucher.getDurationSeconds()).thenReturn(900);
        when(vm.getActiveVoucher(eq(targetUuid))).thenReturn(voucher);

        com.ezflytime.config.ConfigManager cm = mock(com.ezflytime.config.ConfigManager.class);
        when(serviceRegistry.getConfigManager()).thenReturn(cm);
        when(cm.hasBypassUnlimitedFlight(any(org.bukkit.entity.Player.class))).thenReturn(false);
        when(cm.isCreativeModeTreatedAsUnlimited()).thenReturn(true);
        when(cm.isSpectatorModeTreatedAsUnlimited()).thenReturn(true);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("nicj");
        when(target.getUniqueId()).thenReturn(targetUuid);
        when(target.getGameMode()).thenReturn(org.bukkit.GameMode.CREATIVE);
        when(plugin.getServer().getPlayer("nicj")).thenReturn(target);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("ezflytime");

        EzFlyTimeCommand subject = new EzFlyTimeCommand(plugin);

        boolean result = subject.onCommand(sender, command, "ezflytime", new String[]{"info", "nicj"});

        assert result;
        verify(sender).sendMessage("messages.info-header");
        verify(sender).sendMessage("messages.info-remaining-time");
        verify(sender).sendMessage("messages.info-active-voucher");
        verify(sender).sendMessage("messages.info-voucher-duration");
        verify(sender).sendMessage("messages.info-flying-status");
        verify(sender).sendMessage("messages.info-unlimited-flight");
        verify(sender).sendMessage("messages.info-maxsingle-bypass");
    }

    @Test
    void infoCommandRequiresPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.info")).thenReturn(false);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("ezflytime");

        EzFlyTimeCommand subject = new EzFlyTimeCommand(plugin);

        boolean result = subject.onCommand(sender, command, "ezflytime", new String[]{"info", "nicj"});

        assert result;
        verify(sender).sendMessage(plugin.getMessage("messages.no-permission"));
    }

    @Test
    void infoCommandRequiresPlayerArgument() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.info")).thenReturn(true);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("ezflytime");

        EzFlyTimeCommand subject = new EzFlyTimeCommand(plugin);

        boolean result = subject.onCommand(sender, command, "ezflytime", new String[]{"info"});

        assert result;
        verify(sender).sendMessage(plugin.getMessage("messages.info-usage"));
    }
}
