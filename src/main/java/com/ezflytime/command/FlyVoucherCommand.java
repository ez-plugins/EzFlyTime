
package com.ezflytime.command;
import net.milkbowl.vault.economy.Economy;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.voucher.FlyVoucher;
import com.ezflytime.util.MoneyFormatter;
import com.ezflytime.util.OnlinePlayers;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlyVoucherCommand implements CommandExecutor, TabCompleter {

    private final EzFlyTimePlugin plugin;

    public FlyVoucherCommand(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle GUI subcommand
        if (args.length >= 1 && args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessage("messages.player-only"));
                return true;
            }
            Player player = (Player) sender;
            com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
            if (cm != null && cm.isVoucherShopEnabled() && plugin.getServiceRegistry().getVoucherGUI() != null) {
                plugin.getServiceRegistry().getVoucherGUI().openGUI(player);
            } else {
                player.sendMessage(plugin.getMessage("messages.shop-disabled"));
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("buy")) {
            // /flyvoucher buy <voucherId> [amount]
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessage("messages.buy-player-only"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("ezflytime.buy")) {
                player.sendMessage(plugin.getMessage("messages.buy-no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getMessage("messages.buy-usage"));
                return true;
            }
            FlyVoucher voucher = plugin.getServiceRegistry().getVoucherManager().getVoucher(args[1]);
            if (voucher == null || voucher.getPrice() < 0) {
                player.sendMessage(plugin.getMessage("messages.buy-invalid-voucher"));
                return true;
            }
            int amount = 1;
            if (args.length >= 3) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException ex) {
                    player.sendMessage(plugin.getMessage("messages.buy-invalid-amount"));
                    return true;
                }
            }
            Economy econ = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getEconomy() : null;
            if (econ == null) {
                player.sendMessage(plugin.getMessage("messages.buy-economy-unavailable"));
                return true;
            }
            double totalPrice = voucher.getPrice() * amount;
            if (econ.getBalance(player) < totalPrice) {
                player.sendMessage(plugin.getMessage("messages.buy-insufficient-funds")
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{voucher}", voucher.getDisplayName())
                        .replace("{price}", MoneyFormatter.format(totalPrice))
                        .replace("{currency}", econ.currencyNamePlural()));
                return true;
            }
            econ.withdrawPlayer(player, totalPrice);
            List<ItemStack> leftovers = new ArrayList<>();
            ItemStack[] items = voucher.createItems(amount);
            for (ItemStack voucherItem : items) {
                Map<Integer, ItemStack> result = player.getInventory().addItem(voucherItem);
                leftovers.addAll(result.values());
            }
            if (!leftovers.isEmpty()) {
                leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                int leftoverAmount = leftovers.stream().mapToInt(ItemStack::getAmount).sum();
                player.sendMessage(plugin.getMessage("messages.buy-inventory-full")
                        .replace("{amount}", String.valueOf(leftoverAmount))
                        .replace("{voucher}", voucher.getDisplayName()));
            }
                player.sendMessage(plugin.getMessage("messages.buy-success")
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{voucher}", voucher.getDisplayName())
                    .replace("{price}", MoneyFormatter.format(totalPrice))
                    .replace("{currency}", econ.currencyNamePlural()));
            return true;
        }

        // ...existing code for give command...
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(plugin.getMessage("messages.usage"));
            return true;
        }

        if (!sender.hasPermission("ezflytime.give")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }

        Player target = null;
        for (Player onlinePlayer : OnlinePlayers.getOnlinePlayers()) {
            if (onlinePlayer.getName().equalsIgnoreCase(args[1])) {
                target = onlinePlayer;
                break;
            }
        }
        if (target == null) {
            sender.sendMessage(plugin.getMessage("messages.player-not-found"));
            return true;
        }

        FlyVoucher voucher = plugin.getServiceRegistry().getVoucherManager().getVoucher(args[2]);
        if (voucher == null) {
            sender.sendMessage(plugin.getMessage("messages.invalid-voucher"));
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(plugin.getMessage("messages.invalid-amount"));
                return true;
            }
        }


        final Player finalTarget = target;
        final List<ItemStack> leftovers = new ArrayList<>();
        ItemStack[] items = voucher.createItems(amount);
        for (ItemStack voucherItem : items) {
            Map<Integer, ItemStack> result = finalTarget.getInventory().addItem(voucherItem);
            leftovers.addAll(result.values());
        }


        if (!leftovers.isEmpty()) {
            leftovers.forEach(item -> finalTarget.getWorld().dropItemNaturally(finalTarget.getLocation(), item));

            int leftoverAmount = leftovers.stream()
                .mapToInt(ItemStack::getAmount)
                .sum();

            sender.sendMessage(plugin.getMessage("messages.inventory-full-sender")
                    .replace("{player}", target.getName())
                    .replace("{amount}", String.valueOf(leftoverAmount))
                    .replace("{voucher}", voucher.getDisplayName()));
            target.sendMessage(plugin.getMessage("messages.inventory-full-target")
                    .replace("{amount}", String.valueOf(leftoverAmount))
                    .replace("{voucher}", voucher.getDisplayName()));
        }

        sender.sendMessage(plugin.getMessage("messages.gave-voucher")
                .replace("{player}", target.getName())
                .replace("{amount}", String.valueOf(amount))
                .replace("{voucher}", voucher.getDisplayName()));
        target.sendMessage(plugin.getMessage("messages.received-voucher")
                .replace("{amount}", String.valueOf(amount))
                .replace("{voucher}", voucher.getDisplayName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            if (sender.hasPermission("ezflytime.give")) {
                sub.add("give");
            }
            if (sender.hasPermission("ezflytime.buy")) {
                sub.add("buy");
            }
            com.ezflytime.config.ConfigManager cm = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getConfigManager() : null;
            if (cm != null && cm.isVoucherShopEnabled()) {
                sub.add("gui");
            }
            return sub.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("ezflytime.give")) {
                return OnlinePlayers.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("buy") && sender.hasPermission("ezflytime.buy")) {
                return plugin.getServiceRegistry().getVoucherManager().getVoucherIds().stream()
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("ezflytime.give")) {
                return plugin.getServiceRegistry().getVoucherManager().getVoucherIds().stream()
                        .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("buy") && sender.hasPermission("ezflytime.buy")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("1");
                suggestions.add("2");
                suggestions.add("5");
                suggestions.add("10");
                return suggestions.stream().filter(s -> s.startsWith(args[2])).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
