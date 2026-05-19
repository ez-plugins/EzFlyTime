package com.ezflytime.command;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.gui.ParticleSelectGUI;
import com.ezflytime.gui.ParticleShopGUI;
import com.ezflytime.particles.UnlockedParticlesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FlyParticlesCommand implements CommandExecutor, TabCompleter {

    private final EzFlyTimePlugin plugin;

    public FlyParticlesCommand(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("messages.player-only"));
            return true;
        }
        Player player = (Player) sender;

        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if (sub.equals("shop")) {
            ParticleShopGUI shop = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getParticleShopGUI() : null;
            if (shop == null) {
                player.sendMessage(plugin.getMessage("messages.shop-disabled"));
                return true;
            }
            shop.open(player);
            return true;
        }

        if (sub.equals("select") || sub.isEmpty()) {
            ParticleSelectGUI select = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getParticleSelectGUI() : null;
            if (select == null) {
                player.sendMessage(plugin.getMessage("messages.particles-disabled"));
                return true;
            }
            select.open(player);
            return true;
        }

        if (sub.equals("toggle-autoequip")) {
            UnlockedParticlesManager um = plugin.getServiceRegistry() != null ? plugin.getServiceRegistry().getUnlockedParticlesManager() : null;
            if (um == null) {
                player.sendMessage(plugin.getMessage("messages.particles-error"));
                return true;
            }
            boolean newVal = !um.isAutoEquip(player.getUniqueId());
            um.setAutoEquip(player.getUniqueId(), newVal);
            player.sendMessage(plugin.getMessage(newVal ? "messages.auto-equip-enabled" : "messages.auto-equip-disabled"));
            return true;
        }

        // default help
        player.sendMessage(plugin.getMessage("messages.particles-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = List.of("shop", "select", "toggle-autoequip");
            List<String> res = new ArrayList<>();
            for (String o : opts) if (o.startsWith(args[0].toLowerCase(Locale.ROOT))) res.add(o);
            return res;
        }
        return Collections.emptyList();
    }
}
