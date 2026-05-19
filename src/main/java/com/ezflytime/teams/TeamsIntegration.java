package com.ezflytime.teams;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.flight.FlyTimeManager;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.model.Team;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.Optional;

/**
 * Optional integration with TeamsAPI.
 *
 * <p>Registers a {@code /f fly} subcommand in the team plugin's command tree so
 * players can toggle EzFlyTime flight without leaving the team-command context.</p>
 *
 * <p>When {@code teams.claimed-chunks-only} is enabled in {@code config.yml} this
 * class also acts as a Bukkit {@link Listener} to prevent EzFlyTime-managed flight
 * outside the player's team-claimed chunks.</p>
 */
public class TeamsIntegration implements Listener {

    private final EzFlyTimePlugin plugin;
    private FlySubcommand flySubcommand;
    private boolean listenerRegistered = false;

    public TeamsIntegration(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the TeamsAPI integration if TeamsAPI is present and enabled in config.
     * Safe to call when TeamsAPI is absent - degrades gracefully with an info log.
     */
    public void registerIfAvailable() {
        if (!plugin.getConfig().getBoolean("teams.enabled", true)) {
            plugin.getLogger().info("TeamsAPI integration is disabled in config. Skipping.");
            return;
        }

        if (plugin.getServer().getPluginManager().getPlugin("TeamsAPI") == null) {
            plugin.getLogger().info("TeamsAPI not found. Team subcommand and claimed-chunks restriction disabled.");
            return;
        }

        try {
            flySubcommand = new FlySubcommand(plugin);
            TeamsAPI.registerSubcommand(plugin, flySubcommand);
            plugin.getLogger().info("Registered TeamsAPI /fly subcommand.");
        } catch (Throwable t) {
            plugin.getLogger().warning("Error registering TeamsAPI subcommand: " + t.getMessage());
            flySubcommand = null;
            return;
        }

        if (plugin.getConfig().getBoolean("teams.claimed-chunks-only", false)) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
            plugin.getLogger().info("TeamsAPI claimed-chunks-only flight restriction enabled.");
        }
    }

    /**
     * Unregisters the TeamsAPI subcommand and Bukkit listener. Call from {@code onDisable}.
     */
    public void unregister() {
        if (flySubcommand != null) {
            try {
                TeamsAPI.unregisterSubcommand(flySubcommand);
            } catch (Throwable t) {
                plugin.getLogger().warning("Error unregistering TeamsAPI subcommand: " + t.getMessage());
            }
            flySubcommand = null;
        }
        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers (only active when claimed-chunks-only is enabled)
    // -------------------------------------------------------------------------

    /**
     * Prevents EzFlyTime-managed players from enabling flight in an unclaimed chunk.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("ezflytime.teams.fly.bypass")) return;

        FlyTimeManager ftm = getFlyTimeManager();
        if (ftm == null || ftm.getRemainingSeconds(player) <= 0) return;

        if (!isFlightAllowedAt(player, player.getLocation().getChunk())) {
            event.setCancelled(true);
            String msg = isInAnyTeam(player)
                    ? plugin.getMessage("messages.teams-no-fly-unclaimed")
                    : plugin.getMessage("messages.teams-not-in-team");
            player.sendMessage(msg);
        }
    }

    /**
     * Stops EzFlyTime-managed flight when a flying player crosses into an unclaimed chunk.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Only act on chunk-boundary crossings to keep overhead low.
        if (from.getChunk().getX() == to.getChunk().getX()
                && from.getChunk().getZ() == to.getChunk().getZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isFlying()) return;
        if (player.hasPermission("ezflytime.teams.fly.bypass")) return;

        FlyTimeManager ftm = getFlyTimeManager();
        if (ftm == null || ftm.getRemainingSeconds(player) <= 0) return;

        if (!isFlightAllowedAt(player, to.getChunk())) {
            player.setFlying(false);
            player.sendMessage(plugin.getMessage("messages.teams-fly-zone-left"));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the player is allowed to use EzFlyTime flight in the
     * given chunk, according to the {@code teams.claimed-chunks-only} rule.
     *
     * <ul>
     *   <li>If TeamsAPI or its claim service is not available, flight is always allowed
     *       (fail-open keeps the server playable when the dependencies are missing).</li>
     *   <li>If the player is not in a team they may not fly in claimed territory.</li>
     *   <li>If the chunk is wilderness (unclaimed) the player may not fly.</li>
     *   <li>Flight is only allowed when the chunk is claimed by the player's own team.</li>
     * </ul>
     */
    private boolean isFlightAllowedAt(Player player, Chunk chunk) {
        if (!TeamsAPI.isAvailable()) return true;

        TeamsClaimService claimService = TeamsAPI.getClaimService();
        if (claimService == null) return true; // No claim provider registered yet

        TeamsService teamsService = TeamsAPI.getService();
        if (teamsService == null) return true;

        Optional<Team> team = teamsService.getPlayerTeam(player.getUniqueId());
        if (!team.isPresent()) return false; // Not in a team - cannot fly in claimed territory

        return claimService.isClaimedBy(
                team.get().getId(),
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ()
        );
    }

    /** Returns {@code true} when TeamsAPI is available and the player belongs to any team. */
    private boolean isInAnyTeam(Player player) {
        if (!TeamsAPI.isAvailable()) return false;
        TeamsService service = TeamsAPI.getService();
        if (service == null) return false;
        return service.getPlayerTeam(player.getUniqueId()).isPresent();
    }

    private FlyTimeManager getFlyTimeManager() {
        if (plugin.getServiceRegistry() == null) return null;
        return plugin.getServiceRegistry().getFlyTimeManager();
    }
}
