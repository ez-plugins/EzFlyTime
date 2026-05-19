package com.ezflytime.flight;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.storage.FlyTimeStorage;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;

class FlyTimeManagerTest {

    private EzFlyTimePlugin plugin;
    private FlyTimeStorage storage;
    private FileConfiguration configuration;
    private Server server;
    private PluginManager pluginManager;
    private BukkitScheduler scheduler;

    @BeforeEach
    void setUp() {
        plugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);
        storage = mock(FlyTimeStorage.class);
        configuration = new YamlConfiguration();
        server = mock(Server.class, Answers.RETURNS_DEEP_STUBS);
        pluginManager = mock(PluginManager.class);
        scheduler = mock(BukkitScheduler.class);

        when(plugin.getConfig()).thenReturn(configuration);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("FlyTimeManagerTest"));
        when(plugin.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.loadFlyTimes()).thenReturn(Collections.emptyMap());
    }

    @Test
    void autoSaveTaskPersistsDataAtConfiguredInterval() {
        configuration.set("auto-save.interval-seconds", 2);

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

            List<ScheduledTask> scheduledTasks = new ArrayList<>();
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                    .thenAnswer(invocation -> {
                        Runnable runnable = invocation.getArgument(1);
                        long period = invocation.getArgument(3);
                        BukkitTask task = mock(BukkitTask.class);
                        scheduledTasks.add(new ScheduledTask(runnable, period, task));
                        return task;
                    });

            FlyTimeManager manager = new FlyTimeManager(plugin, storage);
            manager.start();

            long expectedAutoSavePeriod = 2L * 20L;
                Runnable autoSaveRunnable = scheduledTasks.stream()
                    .filter(task -> task.period() == expectedAutoSavePeriod)
                    .findFirst()
                    .map(ScheduledTask::runnable)
                    .orElseThrow();

            autoSaveRunnable.run();

            verify(storage, times(1)).saveFlyTimes(anyMap());

            manager.stop();
        }
    }

    @Test
    void stoppingManagerCancelsAutoSaveTask() {
        configuration.set("auto-save.interval-seconds", 3);

        long expectedAutoSavePeriod = 3L * 20L;
        AtomicReference<BukkitTask> autoSaveTaskRef = new AtomicReference<>();

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                    .thenAnswer(invocation -> {
                        long period = invocation.getArgument(3);
                        BukkitTask task = mock(BukkitTask.class);
                        // Capture the last created task (headless scheduler may call differently)
                        autoSaveTaskRef.set(task);
                        return task;
                    });

            FlyTimeManager manager = new FlyTimeManager(plugin, storage);
            manager.start();

            manager.stop();

            verify(autoSaveTaskRef.get(), times(1)).cancel();
        }
    }

    private record ScheduledTask(Runnable runnable, long period, BukkitTask task) {
    }

    // =========================================================================
    // Issue 7 – setTime(player, 0) does not revoke allowFlight for a grounded player
    // =========================================================================

    /**
     * When an admin runs {@code /flytime set <player> 0} (or the remove command
     * reduces remaining time to zero), {@link FlyTimeManager#setTime(Player, int, boolean)}
     * is called with {@code seconds = 0}.
     *
     * <h2>Bug</h2>
     * The revocation code is guarded by {@code activelyFlying.getOrDefault(uuid, false)}.
     * A player who was granted flight but has not yet taken off is <em>not</em>
     * marked as actively flying.  For such a player the guard evaluates to
     * {@code false}, so neither {@code player.setFlying(false)} nor
     * {@code player.setAllowFlight(false)} is ever called.  The player retains
     * their flight permission even though their time has been set to zero, and
     * can start flying until the next countdown tick removes them from the map.
     *
     * <h2>Expected fix</h2>
     * Revoke flight whenever {@code seconds <= 0}, regardless of
     * {@code activelyFlying} state:
     * <pre>
     *   if (seconds <= 0) {
     *       player.setFlying(false);
     *       player.setAllowFlight(false);
     *       activelyFlying.put(uuid, false);
     *   }
     * </pre>
     *
     * <h2>Current status</h2>
     * The test FAILS — {@code verify(player).setAllowFlight(false)} is never
     * satisfied because the production code skips that path for grounded players.
     */
    @Test
    void setTimeToZeroForGroundedPlayerShouldRevokeAllowFlight() {
        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                    .thenAnswer(invocation -> mock(BukkitTask.class));

            FlyTimeManager manager = new FlyTimeManager(plugin, storage);

            // A player who received flight time but has not yet taken off: isFlying() = false.
            Player player = mock(Player.class, Answers.RETURNS_DEEP_STUBS);
            UUID uuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(uuid);
            when(player.isOnline()).thenReturn(true);
            when(player.isFlying()).thenReturn(false);   // grounded — not actively flying
            when(player.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);

            // Grant time without notification so the player enters the map but stays grounded.
            manager.addTime(player, 300, false);

            // Clear invocations recorded during addTime so we only assert on setTime effects.
            clearInvocations(player);

            // An admin zeroes out the player's time.  Flight should be revoked immediately.
            manager.setTime(player, 0, false);

            // Bug: the condition `activelyFlying.getOrDefault(uuid, false)` is false for a
            // grounded player, so setAllowFlight(false) is never called.
            // Fix: revoke allowFlight whenever seconds <= 0, regardless of flying state.
            verify(player).setAllowFlight(false);

            manager.stop();
        }
    }
}
