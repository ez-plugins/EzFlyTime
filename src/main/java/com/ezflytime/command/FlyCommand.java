package com.ezflytime.command;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.config.ConfigManager;
import com.ezflytime.flight.FlyTimeManager;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.ezflytime.util.TimeFormatter.formatCompact;

public class FlyCommand implements CommandExecutor, TabCompleter {

    private final EzFlyTimePlugin plugin;
    public FlyCommand(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isFlyTimeCommand = command.getName().equalsIgnoreCase("flytime");

        // Handle admin subcommands for flytime command
        if (isFlyTimeCommand && args.length > 0) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);

            // Handle reload subcommand
            if (subCommand.equals("reload")) {
                if (!sender.hasPermission("ezflytime.reload")) {
                    sender.sendMessage(plugin.getMessage("messages.no-permission"));
                    return true;
                }
                plugin.reloadPluginConfiguration();
                sender.sendMessage(plugin.getMessage("messages.config-reloaded"));
                return true;
            }

            // Handle time management subcommands
            if (subCommand.equals("give") || subCommand.equals("set") || subCommand.equals("remove")) {
                return handleTimeManagementCommand(sender, args, subCommand);
            }
        }

        // Handle player commands (flytime without subcommands, or fly command)
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("messages.player-only"));
            return true;
        }

        Player player = (Player) sender;

        FlyTimeManager flyTimeManager = plugin.getServiceRegistry().getFlyTimeManager();
        if (flyTimeManager == null) {
            plugin.getLogger().warning("Fly time manager was unavailable when handling the fly command.");
            return true;
        }

        if (isFlyTimeCommand || (args.length > 0 && args[0].equalsIgnoreCase("time"))) {
            // If no arguments provided for /flytime, open the voucher GUI when available.
            // Permission enforcement (ezflytime.buy) is handled inside VoucherGUI.openGUI().
            if (isFlyTimeCommand && args.length == 0) {
                ConfigManager cmGui = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
                if (cmGui != null && cmGui.isVoucherShopEnabled() && plugin.getServiceRegistry().getVoucherGUI() != null) {
                    try {
                        plugin.getServiceRegistry().getVoucherGUI().openGUI(player);
                        return true;
                    } catch (Exception ignored) {
                        // If GUI fails, fall back to showing remaining time
                    }
                }
            }
            if (!player.hasPermission("ezflytime.flytime") && !player.hasPermission("ezflytime.fly")) {
                player.sendMessage(plugin.getMessage("messages.no-permission"));
                return true;
            }
            sendRemainingTime(player, flyTimeManager);
            return true;
        }

        if (!player.hasPermission("ezflytime.fly")) {
            player.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }

        toggleFlight(player, flyTimeManager);
        return true;
    }
    private int parseTimeString(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return -1;
        }

        timeString = timeString.trim().toLowerCase(Locale.ROOT);

        try {
            // Handle formats like "5m30s", "10m", "300s", "5"
            int totalSeconds = 0;
            StringBuilder numberBuilder = new StringBuilder();

            for (char c : timeString.toCharArray()) {
                if (Character.isDigit(c)) {
                    numberBuilder.append(c);
                } else {
                    if (numberBuilder.length() > 0) {
                        int value = Integer.parseInt(numberBuilder.toString());
                        switch (c) {
                            case 'h':
                                totalSeconds += value * 3600; // hours to seconds
                                break;
                            case 'm':
                                totalSeconds += value * 60; // minutes to seconds
                                break;
                            case 's':
                                totalSeconds += value; // seconds
                                break;
                            default:
                                return -1; // invalid unit
                        }
                        numberBuilder.setLength(0);
                    }
                }
            }

            // Handle trailing number (assumed to be seconds if no unit)
            if (numberBuilder.length() > 0) {
                int value = Integer.parseInt(numberBuilder.toString());
                // If no unit specified and it's a reasonable number, assume seconds
                // If it's a small number (1-60), could be seconds or minutes - assume minutes for usability
                if (value <= 60 && !timeString.contains("s") && !timeString.contains("m") && !timeString.contains("h")) {
                    totalSeconds += value * 60; // assume minutes
                } else {
                    totalSeconds += value; // assume seconds
                }
            }

            return totalSeconds;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    private void toggleFlight(Player player, FlyTimeManager flyTimeManager) {
        if (player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            flyTimeManager.pauseCountdown(player);
            int seconds = flyTimeManager.getRemainingSeconds(player);
            player.sendMessage(plugin.getMessage("messages.flight-disabled")
                    .replace("{time}", formatCompact(seconds)));
            return;
        }

        ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        boolean bypassTop = cm != null && cm.hasBypassUnlimitedFlight(player);
        if (!bypassTop && !hasUnlimitedFlight(player)) {
            int seconds = flyTimeManager.getRemainingSeconds(player);
            if (seconds <= 0) {
                player.sendMessage(plugin.getMessage("messages.flight-no-time"));
                return;
            }
            boolean bypassInner = cm != null && cm.hasBypassUnlimitedFlight(player);
            if (bypassInner) {
                enableFlight(player);
                flyTimeManager.resumeCountdown(player);
                player.sendMessage(plugin.getMessage("messages.flight-enabled-unlimited"));
                return;
            }
            enableFlight(player);
            flyTimeManager.resumeCountdown(player);
            player.sendMessage(plugin.getMessage("messages.flight-enabled")
                    .replace("{time}", formatCompact(seconds)));
            return;
        }

        enableFlight(player);
        flyTimeManager.resumeCountdown(player);
        player.sendMessage(plugin.getMessage("messages.flight-enabled-unlimited"));
    }

    private void enableFlight(Player player) {
        player.setAllowFlight(true);
        if (!player.isFlying()) {
            player.setFlying(true);
        }
    }

    private boolean handleTimeManagementCommand(CommandSender sender, String[] args, String subCommand) {
        // Check permission for time management commands
        if (!sender.hasPermission("ezflytime.admin")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }

        // Validate arguments
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessage("messages.flytime-usage")
                    .replace("{command}", subCommand));
            return true;
        }

        String targetPlayerName = args[1];
        String timeString = args[2];

        // Parse time value
        int seconds = parseTimeString(timeString);
        if (seconds < 0) {
            sender.sendMessage(plugin.getMessage("messages.flytime-invalid-time"));
            return true;
        }

        // Find target player
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.getMessage("messages.flytime-player-not-found")
                    .replace("{player}", targetPlayerName));
            return true;
        }

        FlyTimeManager flyTimeManager = plugin.getServiceRegistry().getFlyTimeManager();
        if (flyTimeManager == null) {
            sender.sendMessage(plugin.getMessage("messages.flytime-error"));
            return true;
        }

        // Execute the command
        switch (subCommand) {
            case "give":
                flyTimeManager.addTime(targetPlayer, seconds, false);
                sender.sendMessage(plugin.getMessage("messages.flytime-given")
                        .replace("{player}", targetPlayer.getName())
                        .replace("{time}", formatCompact(seconds)));
                targetPlayer.sendMessage(plugin.getMessage("messages.flytime-received")
                        .replace("{time}", formatCompact(seconds)));
                break;

            case "set":
                flyTimeManager.setTime(targetPlayer, seconds, false);
                sender.sendMessage(plugin.getMessage("messages.flytime-set")
                        .replace("{player}", targetPlayer.getName())
                        .replace("{time}", formatCompact(seconds)));
                targetPlayer.sendMessage(plugin.getMessage("messages.flytime-set-received")
                        .replace("{time}", formatCompact(seconds)));
                break;

            case "remove":
                int currentTime = flyTimeManager.getRemainingSeconds(targetPlayer);
                int actualRemoved = Math.min(currentTime, seconds);
                flyTimeManager.setTime(targetPlayer, Math.max(0, currentTime - seconds), false);
                sender.sendMessage(plugin.getMessage("messages.flytime-removed")
                        .replace("{player}", targetPlayer.getName())
                        .replace("{time}", formatCompact(actualRemoved)));
                targetPlayer.sendMessage(plugin.getMessage("messages.flytime-removed-received")
                        .replace("{time}", formatCompact(actualRemoved)));
                break;
        }

        return true;
    }

    private boolean hasUnlimitedFlight(Player player) {
        com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        if (cm != null && cm.hasBypassUnlimitedFlight(player)) {
            return true;
        }

        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE) {
            if (cm == null) return true;
            return cm.isCreativeModeTreatedAsUnlimited() || player.hasPermission("ezflytime.bypass.creative");
        }
        if (mode == GameMode.SPECTATOR) {
            if (cm == null) return true;
            return cm.isSpectatorModeTreatedAsUnlimited() || player.hasPermission("ezflytime.bypass.spectator");
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("fly") && args.length == 1) {
            List<String> completions = new ArrayList<>();
            addPartialMatches(args[0], Collections.singletonList("time"), completions);
            return completions;
        }

        if (command.getName().equalsIgnoreCase("flytime")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                List<String> subCommands = new ArrayList<>();

                // Add reload if sender has permission
                if (sender.hasPermission("ezflytime.reload")) {
                    subCommands.add("reload");
                }

                // Add time management commands if sender has admin permission
                if (sender.hasPermission("ezflytime.admin")) {
                    subCommands.addAll(List.of("give", "set", "remove"));
                }

                addPartialMatches(args[0], subCommands, completions);
                return completions;
            }

            // Tab complete player names for time management commands
            if (args.length == 2 && sender.hasPermission("ezflytime.admin")) {
                String subCommand = args[0].toLowerCase(Locale.ROOT);
                if (subCommand.equals("give") || subCommand.equals("set") || subCommand.equals("remove")) {
                    List<String> playerNames = new ArrayList<>();
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        playerNames.add(player.getName());
                    }
                    List<String> completions = new ArrayList<>();
                    addPartialMatches(args[1], playerNames, completions);
                    return completions;
                }
            }

            // Tab complete time suggestions for time management commands
            if (args.length == 3 && sender.hasPermission("ezflytime.admin")) {
                String subCommand = args[0].toLowerCase(Locale.ROOT);
                if (subCommand.equals("give") || subCommand.equals("set") || subCommand.equals("remove")) {
                    List<String> timeSuggestions = List.of("30s", "1m", "5m", "10m", "30m", "1h", "2h");
                    List<String> completions = new ArrayList<>();
                    addPartialMatches(args[2], timeSuggestions, completions);
                    return completions;
                }
            }
        }

        return Collections.emptyList();
    }

    private void addPartialMatches(String token, List<String> candidates, List<String> completions) {
        if (token == null) {
            completions.addAll(candidates);
            return;
        }
        String lowerToken = token.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerToken)) {
                completions.add(candidate);
            }
        }
    }

    private void sendRemainingTime(Player player, FlyTimeManager flyTimeManager) {
        // Get remaining seconds from the manager
        int remainingSeconds = flyTimeManager.getRemainingSeconds(player);
        
        if (hasUnlimitedFlight(player)) {
            player.sendMessage(plugin.getMessage("messages.flight-time-unlimited"));
            return;
        }

        ConfigManager cmFuel = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        if (cmFuel != null && cmFuel.isFuelModeEnabled()) {
            // Fuel mode: show as percentage
            int maxFuel = plugin.getConfig().getInt("fuel.max-fuel", 100);
            int currentFuel = Math.min(remainingSeconds, maxFuel);
            int fuelPercent = maxFuel > 0 ? (currentFuel * 100) / maxFuel : 0;
            player.sendMessage(plugin.getMessage("messages.flight-fuel-remaining")
                    .replace("{fuel}", String.valueOf(fuelPercent)));
        } else {
            // Time mode: show as formatted time
            String formattedTime = formatCompact(remainingSeconds);
            player.sendMessage(plugin.getMessage("messages.flight-time-remaining")
                    .replace("{time}", formattedTime));
        }
    }
}
