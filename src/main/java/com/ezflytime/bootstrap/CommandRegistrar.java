package com.ezflytime.bootstrap;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.command.EzFlyTimeCommand;
import com.ezflytime.command.FlyCommand;
import com.ezflytime.command.FlyVoucherCommand;
import com.ezflytime.voucher.VoucherManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class CommandRegistrar {

    private static final Map<String, CommandMetadata> COMMAND_METADATA = new HashMap<>();

    static {
        COMMAND_METADATA.put("fly", new CommandMetadata(
                "Toggle flight or show remaining fly time",
                "/<command> [time]",
                "ezflytime.fly"));
        COMMAND_METADATA.put("flytime", new CommandMetadata(
                "Show remaining fly time or reload the plugin configuration",
                "/<command> [reload]",
                "ezflytime.flytime"));
        COMMAND_METADATA.put("flyvoucher", new CommandMetadata(
                "Give configurable fly vouchers to players",
                "/<command> give <player> <voucherId> [amount]",
            null));
        COMMAND_METADATA.put("ezflytime", new CommandMetadata(
                "EzFlyTime plugin administration commands",
            "/<command> reload|maxsingle <player> [on|off|toggle]",
                "ezflytime.admin"));
        COMMAND_METADATA.put("flyparticles", new CommandMetadata(
            "Open particle selection",
            "/<command>",
            null));
    }

    private final EzFlyTimePlugin plugin;

    public CommandRegistrar(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    public void register(VoucherManager voucherManager) {
        FlyCommand flyCommand = new FlyCommand(plugin);
        registerCommand("fly", flyCommand, flyCommand);
        registerCommand("flytime", flyCommand, flyCommand);

        FlyVoucherCommand flyVoucherCommand = new FlyVoucherCommand(plugin);
        registerCommand("flyvoucher", flyVoucherCommand, flyVoucherCommand);

        // Direct flyparticles command (opens selection and handles subcommands)
        com.ezflytime.command.FlyParticlesCommand flyParticlesCommand = new com.ezflytime.command.FlyParticlesCommand(plugin);
        registerCommand("flyparticles", flyParticlesCommand, flyParticlesCommand);

        EzFlyTimeCommand ezFlyTimeCommand = new EzFlyTimeCommand(plugin);
        registerCommand("ezflytime", ezFlyTimeCommand, ezFlyTimeCommand);

        plugin.getServer().getPluginManager().registerEvents(voucherManager, plugin);
    }

    private void registerCommand(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand pluginCommand = null;
        try {
            pluginCommand = plugin.getCommand(commandName);
        } catch (UnsupportedOperationException exception) {
            // Paper plugins throw during startup when using YAML command declarations.
        }

        if (pluginCommand != null) {
            pluginCommand.setExecutor(executor);
            pluginCommand.setTabCompleter(tabCompleter);
            return;
        }

        registerDynamically(commandName, executor, tabCompleter);
    }

    private void registerDynamically(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        CommandMetadata metadata = COMMAND_METADATA.get(commandName.toLowerCase(Locale.ROOT));
        if (metadata == null) {
            plugin.getLogger().warning("Unable to register command '" + commandName + "': missing metadata definition.");
            return;
        }

        DelegatingPluginCommand command = new DelegatingPluginCommand(commandName, plugin, executor, tabCompleter);
        command.setDescription(metadata.getDescription());
        command.setUsage(metadata.getUsage());
        if (metadata.getPermission() != null && !metadata.getPermission().trim().isEmpty()) {
            command.setPermission(metadata.getPermission());
            command.setPermissionMessage(plugin.getMessage("messages.no-permission"));
        }

        CommandMap commandMap = resolveCommandMap();
        if (commandMap == null) {
            plugin.getLogger().severe("Unable to register command '" + commandName + "': command map unavailable.");
            return;
        }

        String fallbackPrefix = plugin.getName().toLowerCase(Locale.ROOT);
        if (!commandMap.register(fallbackPrefix, command)) {
            plugin.getLogger().warning("Command '" + commandName + "' could not be registered. It may already exist.");
        }
    }

    private CommandMap resolveCommandMap() {
        try {
            Method method = plugin.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(plugin.getServer());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to access server command map for dynamic command registration", exception);
            return null;
        }
    }

    private static final class CommandMetadata {
        private final String description;
        private final String usage;
        private final String permission;

        private CommandMetadata(String description, String usage, String permission) {
            this.description = description;
            this.usage = usage;
            this.permission = permission;
        }

        private String getDescription() {
            return description;
        }

        private String getUsage() {
            return usage;
        }

        private String getPermission() {
            return permission;
        }
    }

    private static final class DelegatingPluginCommand extends Command implements PluginIdentifiableCommand {

        private final Plugin plugin;
        private final CommandExecutor executor;
        private final TabCompleter tabCompleter;

        private DelegatingPluginCommand(String name, Plugin plugin, CommandExecutor executor, TabCompleter tabCompleter) {
            super(name);
            this.plugin = plugin;
            this.executor = executor;
            this.tabCompleter = tabCompleter;
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!testPermission(sender)) {
                return true;
            }
            return executor.onCommand(sender, this, commandLabel, args);
        }

        @Override
        public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (tabCompleter == null) {
                return super.tabComplete(sender, alias, args);
            }

            java.util.List<String> completions = tabCompleter.onTabComplete(sender, this, alias, args);
            return completions != null ? completions : super.tabComplete(sender, alias, args);
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }
    }
}
