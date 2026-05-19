package com.ezflytime.command;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.flight.FlyTimeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FlyCommandTest extends CommandTestBase {

    @Test
    void toggleEnablesFlightWhenPlayerHasTime() {
        // Arrange
        FlyTimeManager flyTimeManager = mock(FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(flyTimeManager);

        Player player = mock(Player.class, Answers.RETURNS_DEEP_STUBS);
        when(player.hasPermission("ezflytime.fly")).thenReturn(true);
        when(player.getAllowFlight()).thenReturn(false);
        when(player.isFlying()).thenReturn(false);
        when(player.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);

        when(flyTimeManager.getRemainingSeconds(player)).thenReturn(300);

        when(plugin.getMessage("messages.flight-enabled")).thenReturn("enabled {time}");

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("fly");

        FlyCommand subject = new FlyCommand(plugin);

        // Act
        boolean result = subject.onCommand(player, command, "fly", new String[0]);

        // Assert
        assert result;
        verify(player).setAllowFlight(true);
        verify(player).setFlying(true);
        verify(flyTimeManager).resumeCountdown(player);
        verify(player).sendMessage("enabled 5m");
    }

    @Test
    void nonPlayerSenderReceivesPlayerOnlyMessage() {
        // Arrange
        CommandSender sender = mock(CommandSender.class);
        when(plugin.getMessage("messages.player-only")).thenReturn("player-only");

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("fly");

        FlyCommand subject = new FlyCommand(plugin);

        // Act
        boolean result = subject.onCommand(sender, command, "fly", new String[0]);

        // Assert
        assert result;
        verify(sender).sendMessage("player-only");
    }

    @Test
    void adminGiveSubcommandAddsTimeToTarget() {
        // Arrange
        FlyTimeManager flyTimeManager = mock(FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(flyTimeManager);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.admin")).thenReturn(true);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("target");
        when(server.getPlayer("target")).thenReturn(target);

        when(plugin.getMessage("messages.flytime-given")).thenReturn("given {player} {time}");
        when(plugin.getMessage("messages.flytime-received")).thenReturn("received {time}");

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        FlyCommand subject = new FlyCommand(plugin);

        // Act
        boolean result = subject.onCommand(sender, command, "flytime", new String[]{"give", "target", "5m"});

        // Assert
        assert result;
        verify(flyTimeManager).addTime(target, 300, false);
        verify(sender).sendMessage("given target 5m");
        verify(target).sendMessage("received 5m");
    }

    @Test
    void invalidTimeStringReturnsErrorMessage() {
        // Arrange
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.admin")).thenReturn(true);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        FlyCommand subject = new FlyCommand(plugin);

        // Act
        boolean result = subject.onCommand(sender, command, "flytime", new String[]{"give", "target", "5x"});

        // Assert
        assert result;
        verify(sender).sendMessage("messages.flytime-invalid-time");
    }

    @Test
    void tabCompleteSuggestsTimeForFly() {
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);
        when(command.getName()).thenReturn("fly");

        FlyCommand subject = new FlyCommand(plugin);

        // Act
        java.util.List<String> completions = subject.onTabComplete(sender, command, "fly", new String[]{"t"});

        // Assert
        assert completions.contains("time");
    }

    @Test
    void tabCompleteFlytimeAdminShowsSubcommandsAndPlayersAndTimes() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.admin")).thenReturn(true);
        when(sender.hasPermission("ezflytime.reload")).thenReturn(true);

        Player p = mock(Player.class);
        when(p.getName()).thenReturn("bob");
        when(server.getOnlinePlayers()).thenAnswer(invocation -> java.util.List.of(p));

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        FlyCommand subject = new FlyCommand(plugin);

        // subcommand suggestions
        java.util.List<String> subs = subject.onTabComplete(sender, command, "flytime", new String[]{""});
        // expect admin subcommands present
        assert subs.contains("give") || subs.contains("reload");

        // player name suggestions for args length 2
        java.util.List<String> players = subject.onTabComplete(sender, command, "flytime", new String[]{"give", ""});
        assert players.contains("bob");

        // time suggestions for args length 3
        java.util.List<String> times = subject.onTabComplete(sender, command, "flytime", new String[]{"give", "bob", ""});
        assert times.contains("1m") || times.contains("30s");
    }

    // =========================================================================
    // Issue 3 – /flytime give sends two messages to the target
    // =========================================================================

    /**
     * {@code FlyCommand.handleTimeManagementCommand} sends
     * {@code messages.flytime-received} to the target player itself.  It also
     * calls {@code flyTimeManager.addTime(target, seconds)} using the 2-arg
     * overload, which defaults {@code notify} to {@code true} and causes
     * {@link com.ezflytime.flight.FlyTimeManager} to also send
     * {@code messages.flight-added}.  The target therefore receives two
     * messages for a single admin grant.
     *
     * <p><b>Expected fix</b>: change the call to
     * {@code addTime(target, seconds, false)} so that {@code FlyCommand} is
     * the sole sender of the notification.
     *
     * <p><b>Current status</b>: test FAILS — the 3-arg overload with
     * {@code notify=false} is never invoked.
     */
    @Test
    void giveSubcommandShouldCallAddTimeWithNotifyDisabled() {
        FlyTimeManager flyTimeManager = mock(FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(flyTimeManager);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.admin")).thenReturn(true);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("target");
        when(server.getPlayer("target")).thenReturn(target);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        FlyCommand subject = new FlyCommand(plugin);
        subject.onCommand(sender, command, "flytime", new String[]{"give", "target", "5m"});

        // FlyCommand sends messages.flytime-received to the target already.
        // Using the 2-arg addTime (notify=true default) causes a second
        // messages.flight-added notification — addTime must be called with
        // notify=false to avoid the double message.
        verify(flyTimeManager).addTime(target, 300, false);
    }

    // =========================================================================
    // Issue 4 – /flytime set sends two messages to the target
    // =========================================================================

    /**
     * The same double-message issue exists for the {@code set} subcommand:
     * {@code FlyCommand} sends {@code messages.flytime-set-received} and also
     * calls {@code flyTimeManager.setTime(target, seconds)} (2-arg, notify=true)
     * which fires {@code messages.flight-time-set}.
     *
     * <p><b>Expected fix</b>: call {@code setTime(target, seconds, false)}.
     *
     * <p><b>Current status</b>: test FAILS — the 3-arg overload with
     * {@code notify=false} is never invoked.
     */
    @Test
    void setSubcommandShouldCallSetTimeWithNotifyDisabled() {
        FlyTimeManager flyTimeManager = mock(FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(flyTimeManager);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.admin")).thenReturn(true);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("target");
        when(server.getPlayer("target")).thenReturn(target);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        FlyCommand subject = new FlyCommand(plugin);
        subject.onCommand(sender, command, "flytime", new String[]{"set", "target", "10m"});

        // FlyCommand sends messages.flytime-set-received to the target already.
        // setTime(target, 600) with default notify=true would also send
        // messages.flight-time-set — use notify=false to prevent the double
        // message.
        verify(flyTimeManager).setTime(target, 600, false);
    }

    // =========================================================================
    // Issue 5 – parseTimeString boundary ambiguity at 60
    // =========================================================================

    /**
     * When a plain number (no unit suffix) is provided, {@code parseTimeString}
     * treats values {@code <= 60} as <em>minutes</em> and values {@code > 60}
     * as <em>seconds</em>.  This creates a silent, 60× discontinuity at the
     * boundary:
     *
     * <pre>
     *   /flytime give player 60   →  3 600 s  (1 hour — treated as minutes)
     *   /flytime give player 61   →     61 s  (treated as seconds)
     * </pre>
     *
     * An admin who types {@code 60} intending to grant 60 seconds actually
     * grants one full hour.
     *
     * <p><b>Current status</b>: both assertions PASS — this test documents the
     * existing (surprising) behaviour so it can be caught when a fix is applied.
     */
    @Test
    void parseTimeStringAmbiguousBoundaryAt60() {
        FlyTimeManager flyTimeManager = mock(FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(flyTimeManager);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.admin")).thenReturn(true);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("target");
        when(server.getPlayer("target")).thenReturn(target);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        FlyCommand subject = new FlyCommand(plugin);

        // "60" with no unit: ≤ 60 threshold → treated as 60 minutes = 3 600 s
        subject.onCommand(sender, command, "flytime", new String[]{"give", "target", "60"});
        verify(flyTimeManager).addTime(target, 3600, false);

        // "61" with no unit: > 60 threshold → treated as 61 seconds
        subject.onCommand(sender, command, "flytime", new String[]{"give", "target", "61"});
        verify(flyTimeManager).addTime(target, 61, false);
        // The two results are 3 600 s vs 61 s — a 58× difference for adjacent
        // inputs, which is likely unintentional.
    }

    // =========================================================================
    // Issue 6 – /flytime remove sends two messages to the target
    // =========================================================================

    /**
     * The {@code remove} subcommand suffers the same double-message problem as
     * {@code give} and {@code set}.  It calls
     * {@code flyTimeManager.setTime(target, remaining)} using the 2-arg
     * overload (notify=true), which fires either {@code messages.flight-time-set}
     * or {@code messages.flight-time-expired} depending on the resulting value.
     * The command then also sends {@code messages.flytime-removed-received} to
     * the target, resulting in two back-to-back notifications.
     *
     * <p><b>Expected fix</b>: call {@code setTime(target, remaining, false)}.
     *
     * <p><b>Current status</b>: test FAILS — the 3-arg overload with
     * {@code notify=false} is never invoked.
     */
    @Test
    void removeSubcommandShouldCallSetTimeWithNotifyDisabled() {
        FlyTimeManager flyTimeManager = mock(FlyTimeManager.class);
        when(serviceRegistry.getFlyTimeManager()).thenReturn(flyTimeManager);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.admin")).thenReturn(true);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("target");
        when(server.getPlayer("target")).thenReturn(target);

        // Player has 10 minutes (600 s) remaining; remove 5 minutes (300 s)
        when(flyTimeManager.getRemainingSeconds(target)).thenReturn(600);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flytime");

        FlyCommand subject = new FlyCommand(plugin);
        subject.onCommand(sender, command, "flytime", new String[]{"remove", "target", "5m"});

        // FlyCommand sends messages.flytime-removed-received to the target already.
        // The 2-arg setTime(target, 300) with notify=true default would also fire
        // messages.flight-time-set (or messages.flight-time-expired when result is 0),
        // giving the target two notifications.  Fix: call setTime(target, 300, false).
        verify(flyTimeManager).setTime(target, 300, false);
    }
}
