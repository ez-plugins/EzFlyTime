package com.ezflytime.command;

import com.ezflytime.voucher.VoucherManager;
import com.ezflytime.voucher.FlyVoucher;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlyVoucherCommandTest extends CommandTestBase {

    @Test
    void giveSubcommandAddsVoucherWhenAdmin() {
        // Arrange
        VoucherManager vm = mock(VoucherManager.class);
        when(serviceRegistry.getVoucherManager()).thenReturn(vm);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.give")).thenReturn(true);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("target");
        when(server.getOnlinePlayers()).thenAnswer(invocation -> java.util.List.of(target));

        FlyVoucher voucher = mock(FlyVoucher.class);
        when(vm.getVoucher("basic")).thenReturn(voucher);
        when(voucher.getDisplayName()).thenReturn("Basic Voucher");
        when(voucher.createItems(1)).thenReturn(new org.bukkit.inventory.ItemStack[0]);

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyvoucher");

        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        // Act
        boolean result = subject.onCommand(sender, command, "flyvoucher", new String[]{"give", "target", "basic"});

        // Assert
        assert result;
        verify(vm).getVoucher("basic");
        String expectedSender = plugin.getMessage("messages.gave-voucher")
            .replace("{player}", "target")
            .replace("{amount}", "1")
            .replace("{voucher}", "Basic Voucher");
        String expectedTarget = plugin.getMessage("messages.received-voucher")
            .replace("{amount}", "1")
            .replace("{voucher}", "Basic Voucher");
        verify(sender).sendMessage(expectedSender);
        verify(target).sendMessage(expectedTarget);
    }

    @Test
    void buyNonPlayerSenderGetsError() {
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyvoucher");

        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        boolean result = subject.onCommand(sender, command, "flyvoucher", new String[]{"buy", "basic"});

        assert result;
        verify(sender).sendMessage("messages.buy-player-only");
    }

    @Test
    void buyInsufficientFundsShowsMessage() {
        Player player = mock(Player.class);
        when(player.hasPermission("ezflytime.buy")).thenReturn(true);

        VoucherManager vm = mock(VoucherManager.class);
        when(serviceRegistry.getVoucherManager()).thenReturn(vm);

        FlyVoucher voucher = mock(FlyVoucher.class);
        when(vm.getVoucher("basic")).thenReturn(voucher);
        when(voucher.getPrice()).thenReturn(10.0);
        when(voucher.getDisplayName()).thenReturn("Basic");

        net.milkbowl.vault.economy.Economy econ = mock(net.milkbowl.vault.economy.Economy.class);
        when(serviceRegistry.getEconomy()).thenReturn(econ);
        when(econ.getBalance(player)).thenReturn(0.0);
        when(econ.currencyNamePlural()).thenReturn("dollars");

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyvoucher");

        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        boolean result = subject.onCommand(player, command, "flyvoucher", new String[]{"buy", "basic"});

        assert result;
        String expected = plugin.getMessage("messages.buy-insufficient-funds")
            .replace("{amount}", "1")
            .replace("{voucher}", "Basic")
            .replace("{price}", com.ezflytime.util.MoneyFormatter.format(10.0))
            .replace("{currency}", "dollars");
        verify(player).sendMessage(expected);
    }

    @Test
    void buySuccessWithdrawsAndSendsSuccess() {
        Player player = mock(Player.class, Answers.RETURNS_DEEP_STUBS);
        when(player.hasPermission("ezflytime.buy")).thenReturn(true);

        VoucherManager vm = mock(VoucherManager.class);
        when(serviceRegistry.getVoucherManager()).thenReturn(vm);

        FlyVoucher voucher = mock(FlyVoucher.class);
        when(vm.getVoucher("basic")).thenReturn(voucher);
        when(voucher.getPrice()).thenReturn(2.5);
        when(voucher.getDisplayName()).thenReturn("Basic");
        when(voucher.createItems(1)).thenReturn(new org.bukkit.inventory.ItemStack[0]);

        net.milkbowl.vault.economy.Economy econ = mock(net.milkbowl.vault.economy.Economy.class);
        when(serviceRegistry.getEconomy()).thenReturn(econ);
        when(econ.getBalance(player)).thenReturn(10.0);
        when(econ.currencyNamePlural()).thenReturn("dollars");

        when(player.getInventory().addItem((org.bukkit.inventory.ItemStack[]) any()))
            .thenAnswer(invocation -> java.util.Collections.<Integer, org.bukkit.inventory.ItemStack>emptyMap());

        Command command = mock(Command.class);
        when(command.getName()).thenReturn("flyvoucher");

        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        boolean result = subject.onCommand(player, command, "flyvoucher", new String[]{"buy", "basic"});

        assert result;
        verify(econ).withdrawPlayer(player, 2.5);
        String expectedSuccess = plugin.getMessage("messages.buy-success")
            .replace("{amount}", "1")
            .replace("{voucher}", "Basic")
            .replace("{price}", com.ezflytime.util.MoneyFormatter.format(2.5))
            .replace("{currency}", "dollars");
        verify(player).sendMessage(expectedSuccess);
    }

    // --- tab-complete permission gating ---

    @Test
    void tabCompleteArg1_buyOnlyPermission_showsBuyNotGive() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.buy")).thenReturn(true);
        when(sender.hasPermission("ezflytime.give")).thenReturn(false);
        when(serviceRegistry.getConfigManager().isVoucherShopEnabled()).thenReturn(false);

        Command command = mock(Command.class);
        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        List<String> result = subject.onTabComplete(sender, command, "flyvoucher", new String[]{""});

        assertTrue(result.contains("buy"), "expected 'buy' in completions");
        assertFalse(result.contains("give"), "expected 'give' not in completions");
    }

    @Test
    void tabCompleteArg1_giveOnlyPermission_showsGiveNotBuy() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.buy")).thenReturn(false);
        when(sender.hasPermission("ezflytime.give")).thenReturn(true);
        when(serviceRegistry.getConfigManager().isVoucherShopEnabled()).thenReturn(false);

        Command command = mock(Command.class);
        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        List<String> result = subject.onTabComplete(sender, command, "flyvoucher", new String[]{""});

        assertTrue(result.contains("give"), "expected 'give' in completions");
        assertFalse(result.contains("buy"), "expected 'buy' not in completions");
    }

    @Test
    void tabCompleteArg1_noPermission_returnsEmpty() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.buy")).thenReturn(false);
        when(sender.hasPermission("ezflytime.give")).thenReturn(false);
        when(serviceRegistry.getConfigManager().isVoucherShopEnabled()).thenReturn(false);

        Command command = mock(Command.class);
        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        List<String> result = subject.onTabComplete(sender, command, "flyvoucher", new String[]{""});

        assertTrue(result.isEmpty(), "expected empty completions");
    }

    @Test
    void tabCompleteArg2_buyPermission_showsVoucherIds() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.buy")).thenReturn(true);
        when(sender.hasPermission("ezflytime.give")).thenReturn(false);

        VoucherManager vm = mock(VoucherManager.class);
        when(serviceRegistry.getVoucherManager()).thenReturn(vm);
        when(vm.getVoucherIds()).thenReturn(Set.of("basic", "premium"));

        Command command = mock(Command.class);
        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        List<String> result = subject.onTabComplete(sender, command, "flyvoucher", new String[]{"buy", ""});

        assertTrue(result.containsAll(List.of("basic", "premium")) && result.size() == 2, "expected voucher IDs in completions");
    }

    @Test
    void tabCompleteArg2_noBuyPermission_returnsEmptyForBuySubcommand() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("ezflytime.buy")).thenReturn(false);
        when(sender.hasPermission("ezflytime.give")).thenReturn(false);

        Command command = mock(Command.class);
        FlyVoucherCommand subject = new FlyVoucherCommand(plugin);

        List<String> result = subject.onTabComplete(sender, command, "flyvoucher", new String[]{"buy", ""});

        assertTrue(result.isEmpty(), "expected empty completions");
    }
}
