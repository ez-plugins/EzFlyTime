package com.ezflytime.update;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SpigotUpdateChecker {

    private static final String API_URL = "https://api.spigotmc.org/legacy/update.php?resource=";
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final EzFlyTimePlugin plugin;
    private final int resourceId;

    public SpigotUpdateChecker(EzFlyTimePlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void checkAsync(UpdateCheckCallback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UpdateCheckResult result = check();
            Bukkit.getScheduler().runTask(plugin, () -> callback.onResult(result));
        });
    }

    public UpdateCheckResult check() {
        String currentVersion = plugin.getDescription().getVersion();
        String downloadUrl = "https://www.spigotmc.org/resources/" + resourceId;
        try {
            URL url = new URL(API_URL + resourceId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(DEFAULT_TIMEOUT_MS);
            connection.setReadTimeout(DEFAULT_TIMEOUT_MS);
            String latestVersion;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                latestVersion = reader.readLine();
            }
            if (latestVersion == null || latestVersion.trim().isEmpty()) {
                return UpdateCheckResult.failed(currentVersion, "Empty update response", downloadUrl);
            }
            latestVersion = latestVersion.trim();
            if (isNewer(latestVersion, currentVersion)) {
                return UpdateCheckResult.updateAvailable(currentVersion, latestVersion, downloadUrl);
            }
            return UpdateCheckResult.upToDate(currentVersion, latestVersion, downloadUrl);
        } catch (Exception ex) {
            return UpdateCheckResult.failed(currentVersion, ex.getMessage(), downloadUrl);
        }
    }

    private boolean isNewer(String latest, String current) {
        if (latest == null || current == null) {
            return false;
        }
        int comparison = compareVersions(latest, current);
        if (comparison == 0) {
            return !latest.equalsIgnoreCase(current);
        }
        return comparison > 0;
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = splitVersion(left);
        String[] rightParts = splitVersion(right);
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            int leftValue = parseVersionPart(leftParts, i);
            int rightValue = parseVersionPart(rightParts, i);
            if (leftValue != rightValue) {
                return leftValue - rightValue;
            }
        }
        return 0;
    }

    private String[] splitVersion(String version) {
        String normalized = version.toLowerCase(Locale.ROOT).replaceAll("[^0-9]+", ".");
        normalized = normalized.replaceAll("\\.+", ".");
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            return new String[0];
        }
        return normalized.split("\\.");
    }

    private int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public interface UpdateCheckCallback {
        void onResult(UpdateCheckResult result);
    }
}
