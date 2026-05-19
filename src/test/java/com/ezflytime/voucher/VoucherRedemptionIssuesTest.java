package com.ezflytime.voucher;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.bootstrap.ServiceRegistry;
import com.ezflytime.flight.FlyTimeManager;
import com.ezflytime.storage.VoucherStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
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
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Feature tests that confirm known issues in the voucher redemption flow.
 *
 * <ol>
 *   <li><b>Cooldown missing event cancellation</b>: when the 1-second redemption
 *       cooldown prevents a repeat redemption, {@code onVoucherUse} returns early
 *       without calling {@code event.setCancelled(true)}.  The right-click therefore
 *       propagates to the server even though no voucher was consumed.</li>
 *
 *   <li><b>Double message on redemption</b>: {@code VoucherManager.onVoucherUse}
 *       calls {@code addTime(player, seconds)} (2-arg overload, {@code notify=true}),
 *       which makes {@link FlyTimeManager} send {@code messages.flight-added} to the
 *       player.  The manager then also sends {@code messages.voucher-redeemed},
 *       resulting in two messages per redemption.  The fix is to call
 *       {@code addTime(player, seconds, false)} so that the manager is the sole
 *       sender of the notification.</li>
 * </ol>
 *
 * <p>Both tests are expected to <em>fail</em> with the current production code,
 * confirming the bugs are present.  They will pass once the issues are fixed.
 */
class VoucherRedemptionIssuesTest {

    private MockedStatic<Bukkit> mockedBukkit;
    private EzFlyTimePlugin ezPlugin;
    private FlyTimeManager flyTimeManager;
    private VoucherManager manager;
    private FlyVoucher voucher;
    private ItemStack voucherItem;
    private Player player;
    private PlayerInventory inventory;

    @BeforeEach
    void setUp() {
        // --- headless Bukkit / ItemFactory mock --------------------------------
        mockedBukkit = mockStatic(Bukkit.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        mockedBukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);

        // One reusable ItemMeta mock per Material so every getItemMeta() call
        // returns the same instance (avoids state loss between calls in the
        // headless environment).
        Map<Material, ItemMeta> metaMap = new HashMap<>();
        when(itemFactory.getItemMeta(any(Material.class))).thenAnswer(inv -> {
            Material mat = inv.getArgument(0);
            return metaMap.computeIfAbsent(mat, m -> {
                ItemMeta meta = mock(ItemMeta.class);
                AtomicReference<String> nameRef = new AtomicReference<>();
                AtomicReference<List<String>> loreRef = new AtomicReference<>();
                doAnswer(a -> { nameRef.set(a.getArgument(0)); return null; })
                        .when(meta).setDisplayName(anyString());
                doAnswer(a -> { loreRef.set(a.getArgument(0)); return null; })
                        .when(meta).setLore(anyList());
                when(meta.hasDisplayName()).thenAnswer(a -> nameRef.get() != null);
                when(meta.getDisplayName()).thenAnswer(a -> nameRef.get());
                when(meta.getLore()).thenAnswer(a -> loreRef.get());
                return meta;
            });
        });

        Server dummyServer = mock(Server.class);
        when(dummyServer.getOnlinePlayers()).thenReturn(Collections.emptyList());
        mockedBukkit.when(Bukkit::getServer).thenReturn(dummyServer);

        // --- plugin / service registry mock -----------------------------------
        ezPlugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);
        YamlConfiguration configuration = new YamlConfiguration();
        // disable dupe detection so item-id checks do not interfere
        configuration.set("detect-voucher-dupes", false);
        configuration.set("vouchers.basic.material", "PAPER");
        configuration.set("vouchers.basic.name", "&aBasic Voucher");
        configuration.set("vouchers.basic.duration-seconds", 120);
        configuration.set("vouchers.basic.lore", Collections.emptyList());
        when(ezPlugin.getConfig()).thenReturn(configuration);
        when(ezPlugin.getLogger()).thenReturn(Logger.getLogger("VoucherRedemptionIssuesTest"));
        when(ezPlugin.getMessage(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class));
        when(ezPlugin.getName()).thenReturn("EzFlyTimeTest");

        flyTimeManager = mock(FlyTimeManager.class);
        ServiceRegistry registry = mock(ServiceRegistry.class);
        when(ezPlugin.getServiceRegistry()).thenReturn(registry);
        when(registry.getFlyTimeManager()).thenReturn(flyTimeManager);

        VoucherStorage storage = mock(VoucherStorage.class);
        when(storage.loadConsumedVoucherIds()).thenReturn(Collections.emptySet());

        // --- real VoucherManager / FlyVoucher ---------------------------------
        manager = new VoucherManager(ezPlugin, storage);
        voucher = manager.getVoucher("basic");
        assertNotNull(voucher, "Voucher 'basic' must be loaded from configuration");

        voucherItem = voucher.createItem();
        // Give the stack a quantity > 1 so the first redemption does not clear
        // the item slot entirely (the mock inventory keeps returning the same
        // item reference regardless, but keeping amount > 1 avoids the
        // emptyAction path and keeps the test focused on the issue under test).
        voucherItem.setAmount(3);

        inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(voucherItem);

        player = mock(Player.class, Answers.RETURNS_DEEP_STUBS);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
    }

    @AfterEach
    void tearDown() {
        if (mockedBukkit != null) {
            mockedBukkit.close();
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private PlayerInteractEvent buildRightClickMainHandEvent() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getItem()).thenReturn(voucherItem);
        when(event.getPlayer()).thenReturn(player);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        return event;
    }

    // =========================================================================
    // Issue 1 – redemption cooldown does not cancel the event
    // =========================================================================

    /**
     * After a successful voucher redemption the manager starts a 1-second
     * cooldown.  Any subsequent right-click within that window is rejected by
     * an early {@code return} in {@code onVoucherUse} — but the event is
     * <em>not</em> cancelled before returning.  This allows the right-click to
     * propagate and potentially interact with blocks or consume the item in
     * unintended ways.
     *
     * <p><b>Expected fix</b>: call {@code event.setCancelled(true)} before
     * returning from the cooldown guard.
     *
     * <p><b>Current status</b>: test FAILS — the bug is present.
     */
    @Test
    void redemptionCooldownShouldCancelInteractionEvent() {
        PlayerInteractEvent firstEvent = buildRightClickMainHandEvent();
        manager.onVoucherUse(firstEvent);
        // Confirm the first redemption was processed
        verify(flyTimeManager, times(1)).addTime(any(Player.class), anyInt(), eq(false));

        // Second right-click within the 1-second cooldown window
        PlayerInteractEvent secondEvent = buildRightClickMainHandEvent();
        manager.onVoucherUse(secondEvent);

        // No additional flight time must be granted ...
        verify(flyTimeManager, times(1)).addTime(any(Player.class), anyInt(), eq(false));

        // ... AND the event must be cancelled so the interaction does not
        // propagate. BUG: the cooldown guard currently returns early without
        // calling event.setCancelled(true).
        verify(secondEvent).setCancelled(true);
    }

    // =========================================================================
    // Issue 2 – two messages sent to the player on a single voucher redemption
    // =========================================================================

    /**
     * {@code VoucherManager.onVoucherUse} calls
     * {@code addTime(player, seconds)} (2-arg overload) whose {@code notify}
     * parameter defaults to {@code true}.  This causes
     * {@link FlyTimeManager#addTime(Player, int)} to send
     * {@code messages.flight-added} to the player.  The manager then sends a
     * second, separate {@code messages.voucher-redeemed} notification, so the
     * player receives <em>two</em> messages for a single redemption.
     *
     * <p><b>Expected fix</b>: change the call to
     * {@code addTime(player, seconds, false)} so {@code VoucherManager} is the
     * sole sender of the redemption notification.
     *
     * <p><b>Current status</b>: test FAILS — {@code addTime} is invoked with
     * its default {@code notify=true}, not with {@code notify=false}.
     */
    @Test
    void voucherRedemptionShouldCallAddTimeWithNotifyDisabled() {
        PlayerInteractEvent event = buildRightClickMainHandEvent();
        manager.onVoucherUse(event);

        // VoucherManager already sends messages.voucher-redeemed itself.
        // To avoid a double message the 3-arg overload must be used with
        // notify=false.  BUG: currently the 2-arg overload (notify=true) is
        // called, causing messages.flight-added to be sent as well.
        verify(flyTimeManager).addTime(player, 120, false);
    }
}
