package com.ezflytime.voucher;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.bootstrap.ServiceRegistry;
import com.ezflytime.flight.FlyTimeManager;
import com.ezflytime.storage.VoucherStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests that on-buy-commands and on-use-commands are dispatched correctly
 * after a successful voucher purchase or redemption.
 *
 * <p>Covers:
 * <ul>
 *   <li>Console command dispatched on voucher use</li>
 *   <li>Player command dispatched on voucher use</li>
 *   <li>No dispatch when command list is empty</li>
 *   <li>Placeholder expansion ({player}, {voucher}, {duration_seconds}, {amount})</li>
 *   <li>Console command dispatched on voucher buy (via FlyVoucher.getOnBuyCommands)</li>
 * </ul>
 */
class VoucherCommandDispatchTest {

    private MockedStatic<Bukkit> mockedBukkit;
    private EzFlyTimePlugin ezPlugin;
    private Server server;
    private ConsoleCommandSender console;
    private FlyTimeManager flyTimeManager;
    private VoucherStorage storage;
    private Player player;
    private PlayerInventory inventory;
    private YamlConfiguration configuration;

    @BeforeEach
    void setUp() {
        mockedBukkit = mockStatic(Bukkit.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        mockedBukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);

        Map<Material, ItemMeta> metaMap = new HashMap<>();
        when(itemFactory.getItemMeta(any(Material.class))).thenAnswer(inv -> {
            Material mat = inv.getArgument(0);
            return metaMap.computeIfAbsent(mat, m -> {
                ItemMeta meta = mock(ItemMeta.class);
                AtomicReference<String> nameRef = new AtomicReference<>();
                AtomicReference<List<String>> loreRef = new AtomicReference<>();
                doAnswer(a -> { nameRef.set(a.getArgument(0)); return null; }).when(meta).setDisplayName(anyString());
                doAnswer(a -> { loreRef.set(a.getArgument(0)); return null; }).when(meta).setLore(anyList());
                when(meta.hasDisplayName()).thenAnswer(a -> nameRef.get() != null);
                when(meta.getDisplayName()).thenAnswer(a -> nameRef.get());
                when(meta.getLore()).thenAnswer(a -> loreRef.get());
                return meta;
            });
        });

        server = mock(Server.class, Answers.RETURNS_DEEP_STUBS);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptyList());
        console = mock(ConsoleCommandSender.class);
        when(server.getConsoleSender()).thenReturn(console);
        mockedBukkit.when(Bukkit::getServer).thenReturn(server);

        ezPlugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);
        when(ezPlugin.getServer()).thenReturn(server);
        when(ezPlugin.getLogger()).thenReturn(Logger.getLogger("VoucherCommandDispatchTest"));
        when(ezPlugin.getName()).thenReturn("EzFlyTimeTest");
        when(ezPlugin.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0, String.class));

        flyTimeManager = mock(FlyTimeManager.class);
        ServiceRegistry registry = mock(ServiceRegistry.class);
        when(ezPlugin.getServiceRegistry()).thenReturn(registry);
        when(registry.getFlyTimeManager()).thenReturn(flyTimeManager);

        storage = mock(VoucherStorage.class);
        when(storage.loadConsumedVoucherIds()).thenReturn(Collections.emptySet());

        configuration = new YamlConfiguration();
        configuration.set("detect-voucher-dupes", false);
        when(ezPlugin.getConfig()).thenReturn(configuration);

        inventory = mock(PlayerInventory.class);
        player = mock(Player.class, Answers.RETURNS_DEEP_STUBS);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
    }

    @AfterEach
    void tearDown() {
        if (mockedBukkit != null) mockedBukkit.close();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private VoucherManager buildManager(List<String> onUseCommands) {
        configuration.set("vouchers.basic.material", "PAPER");
        configuration.set("vouchers.basic.name", "&aBasic Voucher");
        configuration.set("vouchers.basic.duration-seconds", 300);
        configuration.set("vouchers.basic.lore", Collections.emptyList());
        configuration.set("vouchers.basic.price", 100.0);
        configuration.set("vouchers.basic.on-buy-commands", Collections.emptyList());
        configuration.set("vouchers.basic.on-use-commands", onUseCommands);
        return new VoucherManager(ezPlugin, storage);
    }

    private PlayerInteractEvent rightClickMainHand(ItemStack item) {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getItem()).thenReturn(item);
        when(event.getPlayer()).thenReturn(player);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        return event;
    }

    // =========================================================================
    // on-use-commands tests
    // =========================================================================

    @Test
    void useConsoleCommandDispatchedAfterRedemption() {
        VoucherManager manager = buildManager(List.of("console:say {player} redeemed {voucher}"));
        FlyVoucher voucher = manager.getVoucher("basic");
        assertNotNull(voucher);

        ItemStack item = voucher.createItem();
        item.setAmount(2);
        when(inventory.getItemInMainHand()).thenReturn(item);

        manager.onVoucherUse(rightClickMainHand(item));

        ArgumentCaptor<String> cmdCaptor = ArgumentCaptor.forClass(String.class);
        verify(server).dispatchCommand(eq(console), cmdCaptor.capture());
        assertEquals("say TestPlayer redeemed basic", cmdCaptor.getValue());
    }

    @Test
    void usePlayerCommandDispatchedAfterRedemption() {
        VoucherManager manager = buildManager(List.of("player:me just redeemed {voucher}"));
        FlyVoucher voucher = manager.getVoucher("basic");
        assertNotNull(voucher);

        ItemStack item = voucher.createItem();
        item.setAmount(2);
        when(inventory.getItemInMainHand()).thenReturn(item);

        manager.onVoucherUse(rightClickMainHand(item));

        ArgumentCaptor<String> cmdCaptor = ArgumentCaptor.forClass(String.class);
        verify(server).dispatchCommand(eq(player), cmdCaptor.capture());
        assertEquals("me just redeemed basic", cmdCaptor.getValue());
    }

    @Test
    void emptyUseCommandListDispatchesNothing() {
        VoucherManager manager = buildManager(Collections.emptyList());
        FlyVoucher voucher = manager.getVoucher("basic");
        assertNotNull(voucher);

        ItemStack item = voucher.createItem();
        item.setAmount(2);
        when(inventory.getItemInMainHand()).thenReturn(item);

        manager.onVoucherUse(rightClickMainHand(item));

        verify(server, never()).dispatchCommand(any(), anyString());
    }

    @Test
    void usePlaceholdersExpanded() {
        VoucherManager manager = buildManager(
                List.of("console:reward {player} {voucher} {duration_seconds} {amount}"));
        FlyVoucher voucher = manager.getVoucher("basic");
        assertNotNull(voucher);

        ItemStack item = voucher.createItem();
        item.setAmount(2);
        when(inventory.getItemInMainHand()).thenReturn(item);

        manager.onVoucherUse(rightClickMainHand(item));

        ArgumentCaptor<String> cmdCaptor = ArgumentCaptor.forClass(String.class);
        verify(server).dispatchCommand(eq(console), cmdCaptor.capture());
        assertEquals("reward TestPlayer basic 300 1", cmdCaptor.getValue());
    }

    // =========================================================================
    // on-buy-commands are stored on the FlyVoucher
    // =========================================================================

    @Test
    void onBuyCommandsStoredOnVoucher() {
        List<String> buyCommands = List.of("console:give {player} diamond 1", "player:msg {player} Thanks!");
        configuration.set("vouchers.vip.material", "DIAMOND");
        configuration.set("vouchers.vip.name", "&6VIP Voucher");
        configuration.set("vouchers.vip.duration-seconds", 600);
        configuration.set("vouchers.vip.lore", Collections.emptyList());
        configuration.set("vouchers.vip.price", 500.0);
        configuration.set("vouchers.vip.on-buy-commands", buyCommands);
        configuration.set("vouchers.vip.on-use-commands", Collections.emptyList());

        VoucherManager manager = new VoucherManager(ezPlugin, storage);
        FlyVoucher voucher = manager.getVoucher("vip");
        assertNotNull(voucher);
        assertEquals(buyCommands, voucher.getOnBuyCommands());
        assertTrue(voucher.getOnUseCommands().isEmpty());
    }

    @Test
    void noPrefixCommandDefaultsToConsole() {
        VoucherManager manager = buildManager(List.of("broadcast Hello {player}"));
        FlyVoucher voucher = manager.getVoucher("basic");
        assertNotNull(voucher);

        ItemStack item = voucher.createItem();
        item.setAmount(2);
        when(inventory.getItemInMainHand()).thenReturn(item);

        manager.onVoucherUse(rightClickMainHand(item));

        ArgumentCaptor<String> cmdCaptor = ArgumentCaptor.forClass(String.class);
        verify(server).dispatchCommand(eq(console), cmdCaptor.capture());
        assertEquals("broadcast Hello TestPlayer", cmdCaptor.getValue());
    }
}
