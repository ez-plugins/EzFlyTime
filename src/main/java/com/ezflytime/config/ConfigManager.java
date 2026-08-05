package com.ezflytime.config;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.mcmmo.McMMOConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ConfigManager {
    private final EzFlyTimePlugin plugin;
    private FileConfiguration messageConfig;
    private FileConfiguration englishMessageConfig;
    private boolean debugEnabled;
    private McMMOConfig mcmmoConfig;

    public ConfigManager(EzFlyTimePlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.saveDefaultConfig();
        saveDefaultMessages();
        saveDefaultParticles();
        saveResourceIfAbsent("mcmmo.yml");
        this.mcmmoConfig = new McMMOConfig(plugin);
        plugin.reloadConfig();
        refreshDebugState();
        loadMessageConfiguration();
    }

    public void reload() {
        plugin.reloadConfig();
        refreshDebugState();
        saveDefaultMessages();
        saveDefaultParticles();
        saveDefaultVoucherGUI();
        saveResourceIfAbsent("mcmmo.yml");
        if (this.mcmmoConfig == null) {
            this.mcmmoConfig = new McMMOConfig(plugin);
        } else {
            this.mcmmoConfig.reload();
        }
        loadMessageConfiguration();
    }

    public McMMOConfig getMcMMOConfig() {
        return this.mcmmoConfig;
    }

    public boolean isFuelModeEnabled() {
        String mode = plugin.getConfig().getString("display.flytime-mode", "time");
        return "fuel".equalsIgnoreCase(mode);
    }

    public boolean isVoucherShopEnabled() {
        return plugin.getConfig().getBoolean("voucher-shop.enabled", true);
    }

    public boolean isVoucherHideNbtEnabled() {
        return plugin.getConfig().getBoolean("hide-voucher-nbt", true);
    }

    public String getMessage(String path) {
        String prefix = getMessageValue("messages.prefix");
        String message = getMessageValue(path);

        if ("messages.prefix".equals(path)) {
            return translateColorCodes(prefix);
        }

        if (prefix == null) {
            prefix = "";
        }

        if (message == null) {
            message = path;
        }

        return translateColorCodes(prefix + message);
    }

    private String getMessageValue(String path) {
        if (messageConfig != null) {
            String value = messageConfig.getString(path);
            if (value != null) {
                return value;
            }
        }

        if (englishMessageConfig != null && messageConfig != englishMessageConfig) {
            String fallback = englishMessageConfig.getString(path);
            if (fallback != null) {
                return fallback;
            }
        }

        return null;
    }

    private String translateColorCodes(String input) {
        return input == null ? "" : input.replace("&", "§");
    }

    private void saveDefaultMessages() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for messages.");
        }
        // Save all bundled language files from the bundled messages/ directory.
        // This discovers files both when running from the IDE (filesystem)
        // and when packaged inside a JAR (iterate jar entries).
        saveAllResourcesInDirectory("messages");
    }

    private void saveAllResourcesInDirectory(String dirPath) {
        try {
            // Try to locate the resources directory via classloader
            java.net.URL url = getClass().getClassLoader().getResource(dirPath);
            if (url != null) {
                String protocol = url.getProtocol();
                if ("file".equalsIgnoreCase(protocol)) {
                    try {
                        java.nio.file.Path path = java.nio.file.Paths.get(url.toURI());
                        java.nio.file.Files.list(path).forEach(p -> {
                            String name = p.getFileName().toString();
                            if (!name.endsWith("/")) {
                                saveResourceIfAbsent(dirPath + "/" + name);
                            }
                        });
                        return;
                    } catch (Exception ignored) {
                        // fall through to jar-based scanning
                    }
                }
            }

            // Fallback: scan the containing JAR for entries starting with dirPath/
            java.security.CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                java.net.URI location = codeSource.getLocation().toURI();
                java.nio.file.Path locPath = java.nio.file.Paths.get(location);
                java.io.File locFile = locPath.toFile();
                if (locFile.isFile()) {
                    try (java.util.jar.JarFile jar = new java.util.jar.JarFile(locFile)) {
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            java.util.jar.JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (!entry.isDirectory() && name.startsWith(dirPath + "/")) {
                                saveResourceIfAbsent(name);
                            }
                        }
                        return;
                    }
                } else if (locFile.isDirectory()) {
                    // Running from exploded classes; list files under resources in classes directory
                    java.nio.file.Path messagesDir = locFile.toPath().resolve(dirPath);
                    if (java.nio.file.Files.exists(messagesDir)) {
                        java.nio.file.Files.list(messagesDir).forEach(p -> {
                            String name = p.getFileName().toString();
                            saveResourceIfAbsent(dirPath + "/" + name);
                        });
                        return;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to enumerate bundled messages/ resources: " + e.getMessage());

            // As a final fallback, attempt to save a few common names to remain safe
            saveResourceIfAbsent("messages/messages_en.yml");
            saveResourceIfAbsent("messages/messages_nl.yml");
            saveResourceIfAbsent("messages/messages_es.yml");
            saveResourceIfAbsent("messages/messages_de.yml");
        }
    }

    private void saveDefaultParticles() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for particles.");
        }
        saveResourceIfAbsent("particles.yml");
    }

    public void saveDefaultVoucherGUI() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for voucher GUI.");
        }
        saveResourceIfAbsent("voucher-gui.yml");
    }

    private void saveResourceIfAbsent(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void loadMessageConfiguration() {
        englishMessageConfig = loadLanguageFile("en");
        String configuredLanguage = plugin.getConfig().getString("language", "en");
        String lang = configuredLanguage == null ? "en" : configuredLanguage.trim();

        // If the setting looks like a filename/path, allow the user to point to a custom file
        boolean looksLikePath = lang.startsWith("messages/") || lang.startsWith("messages_") || lang.endsWith(".yml") || lang.contains("/");

        if (looksLikePath) {
            String path = lang;
            if (!path.startsWith("messages/")) {
                if (path.startsWith("messages_")) {
                    path = "messages/" + path.substring("messages_".length());
                } else {
                    path = "messages/" + path;
                }
            }
            if (!path.endsWith(".yml")) {
                path = path + ".yml";
            }
            messageConfig = loadLanguageFileByResourcePath(path);
            if (messageConfig == null) {
                messageConfig = englishMessageConfig;
            }
        } else {
            String languageCode = normalizeLanguageCode(lang);
            if ("en".equals(languageCode)) {
                messageConfig = englishMessageConfig;
            } else {
                messageConfig = loadLanguageFile(languageCode);
            }
        }
    }

    private FileConfiguration loadLanguageFile(String languageCode) {
        String fileName = "messages/messages_" + languageCode + ".yml";
        return loadLanguageFileByResourcePath(fileName);
    }

    /**
     * Loads a language file by resource path (e.g. "messages/messages_de.yml").
     * Preference order:
     * - If a file exists in the plugin data folder, load it.
     * - Else if a bundled resource with that path exists, copy it to data folder and load it (so users can edit it).
     * - Else return an empty configuration (null is used by caller to fall back to English).
     */
    private FileConfiguration loadLanguageFileByResourcePath(String resourcePath) {
        try {
            File file = new File(plugin.getDataFolder(), resourcePath);

            // If file doesn't exist in data folder, only copy bundled default if present in plugin resources
            if (!file.exists()) {
                InputStream bundled = plugin.getResource(resourcePath);
                if (bundled != null) {
                    saveResourceIfAbsent(resourcePath);
                }
            }

            if (file.exists()) {
                FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                InputStream resource = plugin.getResource(resourcePath);
                if (resource != null) {
                    try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                        configuration.setDefaults(defaults);
                    }
                }
                return configuration;
            } else {
                // No file in data folder and no bundled resource -> allow caller to fallback
                return null;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to load messages for " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    private String normalizeLanguageCode(String languageSetting) {
        if (languageSetting == null) {
            return "en";
        }
        String normalized = languageSetting.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("nl") || normalized.contains("dutch")) {
            return "nl";
        }
        if (normalized.startsWith("es") || normalized.contains("spanish")) {
            return "es";
        }
        if (normalized.startsWith("de") || normalized.contains("german")) {
            return "de";
        }
        if (normalized.startsWith("fr") || normalized.contains("french")) {
            return "fr";
        }
        if (normalized.startsWith("ru") || normalized.contains("russian")) {
            return "ru";
        }
        if (normalized.startsWith("tr") || normalized.contains("turkish")) {
            return "tr";
        }
        if (normalized.startsWith("zh") || normalized.contains("chinese") || normalized.contains("cn")) {
            return "zh";
        }
        // If we don't explicitly recognize it, return the raw normalized value so it can be used
        // to load messages/messages_<value>.yml (this allows custom language codes)
        return normalized;
    }

    public String resolveStorageBackend() {
        String configured = plugin.getConfig().getString("storage.type", "yaml");
        if (configured == null || configured.trim().isEmpty()) {
            return "unknown";
        }
        return configured.toLowerCase(Locale.ROOT);
    }

    public String resolveAutoFlightRewardsStatus() {
        boolean enabled = plugin.getConfig().getBoolean("auto-flight-rewards.enabled", false);
        return enabled ? "enabled" : "disabled";
    }

    public boolean isBossBarEnabled() {
        org.bukkit.configuration.ConfigurationSection bossBarSection = plugin.getConfig().getConfigurationSection("bossbar");
        return bossBarSection == null || bossBarSection.getBoolean("enabled", true);
    }

    public String resolveActivationMode() {
        String configured = plugin.getConfig().getString("flight.activation-mode", "normal");
        if (configured == null || configured.trim().isEmpty()) {
            return "unknown";
        }
        return configured.toLowerCase(Locale.ROOT);
    }

    public boolean isBypassUnlimitedFlightEnabled() {
        return plugin.getConfig().getBoolean("flight.bypass-grants-unlimited", true);
    }

    public boolean hasBypassUnlimitedFlight(org.bukkit.entity.Player player) {
        return isBypassUnlimitedFlightEnabled() && player.hasPermission("ezflytime.bypass");
    }

    /**
     * Whether players in CREATIVE should be treated as having unlimited flight by default.
     * Defaults to true to preserve previous behavior.
     */
    public boolean isCreativeModeTreatedAsUnlimited() {
        return plugin.getConfig().getBoolean("flight.allow-creative-bypass", true);
    }

    /**
     * Whether players in SPECTATOR should be treated as having unlimited flight by default.
     * Defaults to true to preserve previous behavior.
     */
    public boolean isSpectatorModeTreatedAsUnlimited() {
        return plugin.getConfig().getBoolean("flight.allow-spectator-bypass", true);
    }

    /**
     * Maximum allowed seconds for a single continuous flight session.
     * Returns 0 when disabled.
     */
    public int getMaxSingleFlightSeconds() {
        return plugin.getConfig().getInt("flight.max-single-flight-seconds", 0);
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void debug(String message) {
        if (!debugEnabled) {
            return;
        }
        plugin.getLogger().info("[Debug] " + message);
    }

    private void refreshDebugState() {
        boolean previous = this.debugEnabled;
        this.debugEnabled = plugin.getConfig().getBoolean("debug", false);
        if (debugEnabled && !previous) {
            plugin.getLogger().info("Debug logging enabled.");
        }
    }
}
