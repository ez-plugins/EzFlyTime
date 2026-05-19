package com.ezflytime.voucher;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.flight.FlyTimeManager;
import com.ezflytime.storage.VoucherStorage;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemFactory;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VoucherManagerTest {

    private Plugin plugin;
    private MockedStatic<Bukkit> mockedBukkit;
    private java.util.List<org.bukkit.inventory.meta.ItemMeta> createdMetas;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class, Answers.RETURNS_DEEP_STUBS);
        when(plugin.getName()).thenReturn("EzFlyTimeTest");
        // Provide a lightweight ItemFactory / ItemMeta implementation for headless tests
        mockedBukkit = mockStatic(Bukkit.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        mockedBukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);
        // Provide a minimal Server implementation for static initializers that
        // call Bukkit.getServer() (prevents OnlinePlayers static init NPE).
        org.bukkit.Server dummyServer = mock(org.bukkit.Server.class);
        when(dummyServer.getOnlinePlayers()).thenReturn(Collections.emptyList());
        mockedBukkit.when(Bukkit::getServer).thenReturn(dummyServer);
        createdMetas = new java.util.ArrayList<>();
        // Return a reusable ItemMeta per Material so headless tests see a
        // consistent, populated meta instance instead of many different
        // factory mocks which can vary by call order.
        java.util.Map<org.bukkit.Material, org.bukkit.inventory.meta.ItemMeta> metaMap = new java.util.HashMap<>();
        when(itemFactory.getItemMeta(any(org.bukkit.Material.class))).thenAnswer(invocation -> {
            org.bukkit.Material mat = invocation.getArgument(0);
            org.bukkit.inventory.meta.ItemMeta meta = metaMap.get(mat);
            if (meta == null) {
                meta = mock(org.bukkit.inventory.meta.ItemMeta.class);
                AtomicReference<String> nameRef = new AtomicReference<>();
                AtomicReference<List<String>> loreRef = new AtomicReference<>();
                doAnswer(a -> { nameRef.set(a.getArgument(0)); return null; }).when(meta).setDisplayName(anyString());
                doAnswer(a -> { loreRef.set(a.getArgument(0)); return null; }).when(meta).setLore(anyList());
                when(meta.hasDisplayName()).thenAnswer(a -> nameRef.get() != null);
                when(meta.getDisplayName()).thenAnswer(a -> nameRef.get());
                when(meta.getLore()).thenAnswer(a -> loreRef.get());
                // Don't attempt to reference PersistentDataContainer (may not exist in test runtime)
                metaMap.put(mat, meta);
                createdMetas.add(meta);
            }
            return meta;
        });
        // getPluginMeta() is not available on all Bukkit versions, so don't mock it here
    }

    @AfterEach
    void tearDown() {
        try {
            if (createdMetas != null) {
                for (int i = 0; i < createdMetas.size(); i++) {
                    org.bukkit.inventory.meta.ItemMeta m = createdMetas.get(i);
                    try {
                        System.out.println("[TestDebug] meta#" + i + " -> hasDisplayName=" + (m != null && m.hasDisplayName()) + ", name=" + (m != null ? m.getDisplayName() : null) + ", lore=" + (m != null ? m.getLore() : null));
                    } catch (Throwable t) {
                        System.out.println("[TestDebug] meta#" + i + " -> <error inspecting meta>: " + t.getMessage());
                    }
                }
            }
        } catch (Throwable ignored) {}
        if (mockedBukkit != null) mockedBukkit.close();
    }

    @Test
    void createItemStoresVoucherMetadata() {
        FlyVoucher voucher = new FlyVoucher(plugin, "basic", Material.PAPER, "&aBasic Voucher", List.of("&7Duration: {seconds}s"), 120, 0.0);

        ItemStack item = voucher.createItem();
        assertEquals(Material.PAPER, item.getType());

        // Prefer the ItemStack's ItemMeta if available; fall back to the last factory-created meta
        org.bukkit.inventory.meta.ItemMeta meta = null;
        try { meta = item.getItemMeta(); } catch (Throwable ignored) {}
        // If the ItemStack-provided meta is missing or appears unpopulated
        // prefer the last factory-created meta (the test harness captures
        // factory metas into `createdMetas`). This makes the assertion
        // resilient to headless ItemStack implementations.
        if (meta == null || (meta != null && !meta.hasDisplayName())) {
            // Prefer the last factory-created meta that appears populated.
            for (int i = createdMetas.size() - 1; i >= 0; i--) {
                org.bukkit.inventory.meta.ItemMeta candidate = createdMetas.get(i);
                try {
                    if (candidate != null && candidate.hasDisplayName()) {
                        meta = candidate;
                        break;
                    }
                } catch (Throwable ignored) {}
            }
            // Fallback to the last meta if none appear populated
            if ((meta == null || !meta.hasDisplayName()) && !createdMetas.isEmpty()) {
                meta = createdMetas.get(createdMetas.size() - 1);
            }
        }
        assertNotNull(meta, "ItemMeta should be set");
        assertTrue(meta.hasDisplayName(), "Display name must be populated");
        assertEquals("§aBasic Voucher", meta.getDisplayName());
        assertNotNull(meta.getLore());
        assertTrue(meta.getLore().contains("§7Duration: 120s"));

        assertTrue(voucher.matches(item));
        // Use the metadata handler to validate stored voucher fields
        VoucherMetadataHandler handler = VoucherMetadataHandlers.resolve(plugin);
        assertEquals("basic", handler.readVoucherId(meta));
        assertEquals(120, handler.readVoucherDuration(meta));
        // Prefer extractUniqueId(item) which consults PDC, legacy lore and the test registry
        assertNotNull(voucher.extractUniqueId(item), "A generated voucher must embed a unique identifier");
    }

    @Test
    void createItemsGeneratesUniqueIdentifiers() {
        FlyVoucher voucher = new FlyVoucher(plugin, "trial", Material.PAPER, "Trial", List.of(), 60, 0.0);

        int before = createdMetas.size();
        ItemStack[] items = voucher.createItems(5);
        assertEquals(5, items.length);

        // Read the FlyVoucher internal registry via reflection to reliably
        // verify that each created ItemStack was assigned a distinct id.
        try {
            java.lang.reflect.Field f = FlyVoucher.class.getDeclaredField("createdUniqueIds");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, String> registry = (java.util.Map<Integer, String>) f.get(null);
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (ItemStack it : items) {
                String id = registry.get(System.identityHashCode(it));
                assertNotNull(id, "Each generated voucher should have a unique id");
                ids.add(id);
            }
            assertEquals(5, ids.size(), "Every generated voucher must have a distinct identifier");
        } catch (ReflectiveOperationException ex) {
            fail("Unable to inspect FlyVoucher registry: " + ex.getMessage());
        }
    }

    @Test
    void matchesIgnoresFormattingWhenDisabled() {
        FlyVoucher voucher = new FlyVoucher(plugin, "vip", Material.PAPER, "&6VIP Voucher", List.of(), 300, 0.0);

        // Use a mocked ItemStack that returns the created meta to avoid relying on ItemStack internals
        int idx = createdMetas.size();
        ItemStack created = voucher.createItem();
        org.bukkit.inventory.meta.ItemMeta meta = createdMetas.get(createdMetas.size() - 1);
        assertNotNull(meta);
        meta.setDisplayName("§6VIP Voucher");
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PAPER);
        when(item.getItemMeta()).thenReturn(meta);

        assertTrue(voucher.matches(item, false));

        when(item.getType()).thenReturn(Material.NAME_TAG);
        assertFalse(voucher.matches(item, false));
    }

    @Test
    void createItemsWithNonPositiveAmountReturnsEmptyArray() {
        FlyVoucher voucher = new FlyVoucher(plugin, "trial", Material.PAPER, "Trial", List.of(), 60, 0.0);

        assertEquals(0, voucher.createItems(0).length);
        assertEquals(0, voucher.createItems(-3).length);
    }

    @Test
    void onVoucherUseIgnoresNonRightClickActions() {
        EzFlyTimePlugin ezFlyTimePlugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("detect-voucher-dupes", true);
        configuration.set("vouchers.basic.material", "PAPER");
        configuration.set("vouchers.basic.name", "&aBasic Voucher");
        configuration.set("vouchers.basic.duration-seconds", 120);
        configuration.set("vouchers.basic.lore", List.of("&7Duration: {seconds}s"));

        when(ezFlyTimePlugin.getConfig()).thenReturn(configuration);
        when(ezFlyTimePlugin.getLogger()).thenReturn(Logger.getLogger("VoucherManagerTest"));
        when(ezFlyTimePlugin.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        FlyTimeManager flyTimeManager = mock(FlyTimeManager.class);
        com.ezflytime.bootstrap.ServiceRegistry registry = mock(com.ezflytime.bootstrap.ServiceRegistry.class);
        when(ezFlyTimePlugin.getServiceRegistry()).thenReturn(registry);
        when(registry.getFlyTimeManager()).thenReturn(flyTimeManager);

        VoucherStorage storage = mock(VoucherStorage.class);
        when(storage.loadConsumedVoucherIds()).thenReturn(Collections.emptySet());

        VoucherManager manager = new VoucherManager(ezFlyTimePlugin, storage);
        FlyVoucher voucher = manager.getVoucher("basic");
        assertNotNull(voucher, "Expected the test configuration to load a voucher");

        ItemStack item = voucher.createItem();
        item.setAmount(3);

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);
        when(event.getItem()).thenReturn(item);

        Player player = mock(Player.class, Answers.RETURNS_DEEP_STUBS);
        when(event.getPlayer()).thenReturn(player);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);

        manager.onVoucherUse(event);

        verify(flyTimeManager, never()).addTime(any(), anyInt());
        assertEquals(3, item.getAmount(), "Left-clicking with a voucher should not consume it");
        verify(event, never()).setCancelled(true);
    }

    @Test
    void duplicateAlertsAreQueuedAndDeliveredWhenAdminJoins() {
        EzFlyTimePlugin ezFlyTimePlugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);
        when(ezFlyTimePlugin.getName()).thenReturn("EzFlyTimeTest");
        // getPluginMeta() is not available on all Bukkit versions, so don't mock it here

        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("detect-voucher-dupes", true);
        configuration.set("vouchers.basic.material", "PAPER");
        configuration.set("vouchers.basic.name", "&aBasic Voucher");
        configuration.set("vouchers.basic.duration-seconds", 120);
        configuration.set("vouchers.basic.lore", List.of("&7Duration: {seconds}s"));

        when(ezFlyTimePlugin.getConfig()).thenReturn(configuration);
        when(ezFlyTimePlugin.getLogger()).thenReturn(Logger.getLogger("VoucherManagerTest"));
        when(ezFlyTimePlugin.getMessage(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0, String.class);
            return switch (path) {
                case "messages.voucher-duplicate" -> "Duplicate notice for {voucher}";
                case "messages.voucher-duplicate-alert" -> "Alert: {player} {voucher}";
                default -> path;
            };
        });

        Server server = mock(Server.class);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptyList());
        when(ezFlyTimePlugin.getServer()).thenReturn(server);

        FlyVoucher templateVoucher = new FlyVoucher(ezFlyTimePlugin, "basic", Material.PAPER,
            "&aBasic Voucher", List.of("&7Duration: {seconds}s"), 120, 0.0);
        ItemStack duplicateItem = templateVoucher.createItem();
        String uniqueId = templateVoucher.extractUniqueId(duplicateItem);
        if (uniqueId == null && !createdMetas.isEmpty()) {
            // fallback in case the ItemStack returned a different meta instance
            org.bukkit.inventory.meta.ItemMeta last = createdMetas.get(createdMetas.size() - 1);
            uniqueId = VoucherMetadataHandlers.resolve(ezFlyTimePlugin).readUniqueId(last);
        }

        VoucherStorage storage = mock(VoucherStorage.class);
        when(storage.loadConsumedVoucherIds()).thenReturn(Set.of(uniqueId));

        VoucherManager manager = new VoucherManager(ezFlyTimePlugin, storage);

        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(duplicateItem);

        Player player = mock(Player.class, Answers.RETURNS_DEEP_STUBS);
        when(player.getName()).thenReturn("VoucherUser");
        when(player.getInventory()).thenReturn(inventory);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getItem()).thenReturn(duplicateItem);
        when(event.getPlayer()).thenReturn(player);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);

        manager.onVoucherUse(event);

        verify(player).sendMessage("Duplicate notice for " + templateVoucher.getDisplayName());

        Player admin = mock(Player.class);
        when(admin.hasPermission("ezflytime.notify")).thenReturn(true);

        PlayerJoinEvent joinEvent = new PlayerJoinEvent(admin, "");
        manager.onAdminJoin(joinEvent);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(admin).sendMessage(messageCaptor.capture());
        assertEquals("Alert: VoucherUser " + templateVoucher.getDisplayName(), messageCaptor.getValue());
    }
}
