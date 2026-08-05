package com.ezflytime.command;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.ezflytime.util.TimeFormatter.formatCompact;

public class EzFlyTimeCommand implements CommandExecutor, TabCompleter {

    private final EzFlyTimePlugin plugin;

    public EzFlyTimeCommand(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "top":
                return handleTop(sender);
            case "maxsingle":
                return handleMaxSingleBypass(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleTop(CommandSender sender) {
        if (!sender.hasPermission("ezflytime.top")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }
        // Get FlyTimeManager and fetch top players
        com.ezflytime.flight.FlyTimeManager flyTimeManager = plugin.getServiceRegistry().getFlyTimeManager();
        java.util.Map<java.util.UUID, Integer> flyTimes = flyTimeManager.snapshotRemainingSeconds();
        java.util.List<java.util.Map.Entry<java.util.UUID, Integer>> sorted = new java.util.ArrayList<>(flyTimes.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int max = Math.min(10, sorted.size());
        sender.sendMessage(plugin.getMessage("messages.flytime-top-header"));
        for (int i = 0; i < max; i++) {
            java.util.UUID uuid = sorted.get(i).getKey();
            int seconds = sorted.get(i).getValue();
            org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            String name = player.getName() != null ? player.getName() : uuid.toString();
            String time = formatTime(seconds);
            sender.sendMessage(plugin.getMessage("messages.flytime-top-entry")
                    .replace("{rank}", String.valueOf(i + 1))
                    .replace("{name}", name)
                    .replace("{time}", time));
        }
        return true;
    }

    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ezflytime.reload")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }

        try {
            plugin.reloadPluginConfiguration();
            sender.sendMessage(plugin.getMessage("messages.config-reloaded"));
        } catch (Exception e) {
            sender.sendMessage(plugin.getMessage("messages.reload-error"));
            plugin.getLogger().severe("Error reloading plugin configuration: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean handleMaxSingleBypass(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezflytime.maxsingle.manage")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("messages.max-single-bypass-usage"));
            return true;
        }

        org.bukkit.entity.Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessage("messages.player-not-found"));
            return true;
        }

        com.ezflytime.flight.FlyTimeManager flyTimeManager = plugin.getServiceRegistry().getFlyTimeManager();
        if (flyTimeManager == null) {
            sender.sendMessage(plugin.getMessage("messages.flytime-error"));
            return true;
        }

        String action = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "toggle";
        boolean enabled;
        switch (action) {
            case "on":
            case "enable":
                enabled = flyTimeManager.setMaxSingleFlightBypass(target.getUniqueId(), true);
                break;
            case "off":
            case "disable":
                enabled = flyTimeManager.setMaxSingleFlightBypass(target.getUniqueId(), false);
                break;
            case "toggle":
                enabled = flyTimeManager.toggleMaxSingleFlightBypass(target.getUniqueId());
                break;
            default:
                sender.sendMessage(plugin.getMessage("messages.max-single-bypass-usage"));
                return true;
        }

        flyTimeManager.resumeCountdown(target);
        if (enabled) {
            sender.sendMessage(plugin.getMessage("messages.max-single-bypass-enabled")
                    .replace("{player}", target.getName()));
            if (!sender.equals(target)) {
                target.sendMessage(plugin.getMessage("messages.max-single-bypass-enabled-self"));
            }
        } else {
            sender.sendMessage(plugin.getMessage("messages.max-single-bypass-disabled")
                    .replace("{player}", target.getName()));
            if (!sender.equals(target)) {
                target.sendMessage(plugin.getMessage("messages.max-single-bypass-disabled-self"));
            }
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezflytime.info")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("messages.info-usage"));
            return true;
        }

        org.bukkit.entity.Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessage("messages.player-not-found")
                    .replace("{player}", args[1]));
            return true;
        }

        com.ezflytime.flight.FlyTimeManager flyTimeManager = plugin.getServiceRegistry().getFlyTimeManager();
        if (flyTimeManager == null) {
            sender.sendMessage(plugin.getMessage("messages.flytime-error"));
            return true;
        }

        sender.sendMessage(plugin.getMessage("messages.info-header")
                .replace("{player}", target.getName()));

        int remaining = flyTimeManager.getRemainingSeconds(target);
        if (remaining > 0) {
            sender.sendMessage(plugin.getMessage("messages.info-remaining-time")
                    .replace("{time}", formatCompact(remaining)));
        } else {
            sender.sendMessage(plugin.getMessage("messages.info-no-remaining-time"));
        }

        com.ezflytime.voucher.FlyVoucher activeVoucher = plugin.getServiceRegistry().getVoucherManager().getActiveVoucher(target.getUniqueId());
        if (activeVoucher != null) {
            sender.sendMessage(plugin.getMessage("messages.info-active-voucher")
                    .replace("{voucher}", activeVoucher.getDisplayName()));
            sender.sendMessage(plugin.getMessage("messages.info-voucher-duration")
                    .replace("{duration}", formatCompact(activeVoucher.getDurationSeconds())));
        } else {
            sender.sendMessage(plugin.getMessage("messages.info-no-active-voucher"));
        }

        java.util.Map<java.util.UUID, Boolean> activeStates = flyTimeManager.snapshotActiveStates();
        boolean isActive = activeStates.getOrDefault(target.getUniqueId(), false);
        sender.sendMessage(plugin.getMessage("messages.info-flying-status")
                .replace("{status}", isActive ? "Yes" : "No"));

        com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
        boolean unlimited = cm != null && cm.hasBypassUnlimitedFlight(target);
        if (!unlimited) {
            org.bukkit.GameMode mode = target.getGameMode();
            if (mode == org.bukkit.GameMode.CREATIVE && cm != null && cm.isCreativeModeTreatedAsUnlimited()) {
                unlimited = true;
            } else if (mode == org.bukkit.GameMode.SPECTATOR && cm != null && cm.isSpectatorModeTreatedAsUnlimited()) {
                unlimited = true;
            }
        }
        sender.sendMessage(plugin.getMessage("messages.info-unlimited-flight")
                .replace("{status}", unlimited ? "Yes" : "No"));

        boolean maxSingleBypass = flyTimeManager.hasMaxSingleFlightBypass(target);
        sender.sendMessage(plugin.getMessage("messages.info-maxsingle-bypass")
                .replace("{status}", maxSingleBypass ? "Yes" : "No"));

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessage("messages.help-heading"));
        sender.sendMessage(plugin.getMessage("messages.help-reload"));
        sender.sendMessage(plugin.getMessage("messages.help-maxsingle"));
        sender.sendMessage(plugin.getMessage("messages.help-info"));
        sender.sendMessage(plugin.getMessage("messages.help-help"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase(Locale.ROOT);

            if ("reload".startsWith(partial) && sender.hasPermission("ezflytime.reload")) {
                completions.add("reload");
            }
            if ("top".startsWith(partial) && sender.hasPermission("ezflytime.top")) {
                completions.add("top");
            }
            if ("maxsingle".startsWith(partial) && sender.hasPermission("ezflytime.maxsingle.manage")) {
                completions.add("maxsingle");
            }
            if ("info".startsWith(partial) && sender.hasPermission("ezflytime.info")) {
                completions.add("info");
            }
            if ("help".startsWith(partial)) {
                completions.add("help");
            }

            return completions;
        }

        if (args.length == 2) {
            if ("maxsingle".equalsIgnoreCase(args[0]) && sender.hasPermission("ezflytime.maxsingle.manage")) {
                List<String> players = new ArrayList<>();
                String partial = args[1].toLowerCase(Locale.ROOT);
                for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                    String name = player.getName();
                    if (name.toLowerCase(Locale.ROOT).startsWith(partial)) {
                        players.add(name);
                    }
                }
                Collections.sort(players);
                return players;
            }
            if ("info".equalsIgnoreCase(args[0]) && sender.hasPermission("ezflytime.info")) {
                List<String> players = new ArrayList<>();
                String partial = args[1].toLowerCase(Locale.ROOT);
                for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                    String name = player.getName();
                    if (name.toLowerCase(Locale.ROOT).startsWith(partial)) {
                        players.add(name);
                    }
                }
                Collections.sort(players);
                return players;
            }
        }

        if (args.length == 3 && "maxsingle".equalsIgnoreCase(args[0]) && sender.hasPermission("ezflytime.maxsingle.manage")) {
            List<String> actions = new ArrayList<>();
            String partial = args[2].toLowerCase(Locale.ROOT);
            for (String option : new String[]{"on", "off", "toggle"}) {
                if (option.startsWith(partial)) {
                    actions.add(option);
                }
            }
            return actions;
        }

        return Collections.emptyList();
    }
}