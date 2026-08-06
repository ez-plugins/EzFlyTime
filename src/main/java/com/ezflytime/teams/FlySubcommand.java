package com.ezflytime.teams;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.config.ConfigManager;
import com.ezflytime.flight.FlyTimeManager;
import com.skyblockexp.teamsapi.api.TeamsSubcommand;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Registers {@code /f fly} (or the team plugin's equivalent command) so that
 * players can toggle EzFlyTime flight from within the team command hierarchy.
 */
public class FlySubcommand implements TeamsSubcommand {

    private final EzFlyTimePlugin plugin;

    public FlySubcommand(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "fly";
    }

    @Override
    public String getDescription() {
        return "Toggle EzFlyTime flight";
    }

    @Override
    public String getPermission() {
        return "ezflytime.fly";
    }

    @Override
    public String getUsage() {
        return "fly";
    }

    /**
     * args[0] is always "fly" (the subcommand name); additional arguments start at args[1].
     */
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("messages.player-only"));
            return true;
        }
        Player player = (Player) sender;

        FlyTimeManager ftm = plugin.getServiceRegistry() != null
                ? plugin.getServiceRegistry().getFlyTimeManager() : null;
        if (ftm == null) {
            return true;
        }

        toggleFlight(player, ftm);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    private void toggleFlight(Player player, FlyTimeManager ftm) {
        ConfigManager cm = plugin.getServiceRegistry() != null
                ? plugin.getServiceRegistry().getConfigManager() : null;
        String fmt = cm != null ? cm.getTimeFormat() : "compact";
        if (player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            ftm.pauseCountdown(player);
            if (hasUnlimitedFlight(player, cm)) {
                player.sendMessage(plugin.getMessage("messages.flight-disabled-unlimited"));
            } else {
                int seconds = ftm.getRemainingSeconds(player);
                player.sendMessage(plugin.getMessage("messages.flight-disabled")
                        .replace("{time}", com.ezflytime.util.TimeFormatter.format(seconds, fmt)));
            }
            return;
        }

        boolean bypassUnlimited = cm != null && cm.hasBypassUnlimitedFlight(player);

        if (!bypassUnlimited && !hasUnlimitedFlight(player, cm)) {
            int seconds = ftm.getRemainingSeconds(player);
            if (seconds <= 0) {
                player.sendMessage(plugin.getMessage("messages.flight-no-time"));
                return;
            }
            player.setAllowFlight(true);
            player.setFlying(true);
            ftm.resumeCountdown(player);
            player.sendMessage(plugin.getMessage("messages.flight-enabled")
                    .replace("{time}", com.ezflytime.util.TimeFormatter.format(seconds, fmt)));
            return;
        }

        player.setAllowFlight(true);
        player.setFlying(true);
        ftm.resumeCountdown(player);
        player.sendMessage(plugin.getMessage("messages.flight-enabled-unlimited"));
    }

    private boolean hasUnlimitedFlight(Player player, ConfigManager cm) {
        if (cm != null && cm.hasBypassUnlimitedFlight(player)) return true;
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE) {
            return cm == null || cm.isCreativeModeTreatedAsUnlimited();
        }
        if (mode == GameMode.SPECTATOR) {
            return cm == null || cm.isSpectatorModeTreatedAsUnlimited();
        }
        return false;
    }
}
