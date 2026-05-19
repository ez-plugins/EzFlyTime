package com.ezflytime.placeholder;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.flight.FlyTimeManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class FlyTimePlaceholderExpansion extends PlaceholderExpansion {

    private final EzFlyTimePlugin plugin;

    public FlyTimePlaceholderExpansion(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ezflytime";
    }

    @Override
    public @NotNull String getAuthor() {
        List<String> authors = plugin.getDescription().getAuthors();
        if (!authors.isEmpty()) {
            return String.join(", ", authors);
        }
        return plugin.getDescription().getName();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        return handleRequest(player == null ? null : player.getUniqueId(), params);
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return handleRequest(player == null ? null : player.getUniqueId(), params);
    }

    @Nullable
    private String handleRequest(@Nullable UUID uuid, String params) {
        if (uuid == null) {
            return "";
        }

        FlyTimeManager flyTimeManager = plugin.getServiceRegistry().getFlyTimeManager();
        if (flyTimeManager == null) {
            return "";
        }

        int remainingSeconds = flyTimeManager.getRemainingSeconds(uuid);
        switch (params.toLowerCase(Locale.ROOT)) {
            case "seconds":
            case "time_remaining_raw":
                return String.valueOf(remainingSeconds);
            case "minutes":
                return String.valueOf(remainingSeconds / 60);
            case "formatted":
            case "time_remaining":
                return formatSeconds(remainingSeconds);
            default:
                return null;
        }
    }

    private String formatSeconds(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "00:00";
        }

        Duration duration = Duration.ofSeconds(totalSeconds);
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }
}
