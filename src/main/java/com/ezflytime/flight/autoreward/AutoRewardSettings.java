package com.ezflytime.flight.autoreward;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

class AutoRewardSettings {

    private final boolean enabled;
    private final int intervalSeconds;
    private final int baseSeconds;
    private final boolean notifyPlayers;
    private final List<RankReward> rankRewards;
    private final List<SkillReward> skillRewards;

    private AutoRewardSettings(boolean enabled,
                               int intervalSeconds,
                               int baseSeconds,
                               boolean notifyPlayers,
                               List<RankReward> rankRewards,
                               List<SkillReward> skillRewards) {
        this.enabled = enabled;
        this.intervalSeconds = intervalSeconds;
        this.baseSeconds = baseSeconds;
        this.notifyPlayers = notifyPlayers;
        this.rankRewards = rankRewards;
        this.skillRewards = skillRewards;
    }

    static AutoRewardSettings fromConfiguration(EzFlyTimePlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("auto-flight-rewards");
        if (section == null) {
            return disabled();
        }

        boolean enabled = section.getBoolean("enabled", false);
        int intervalSeconds = Math.max(0, section.getInt("interval-seconds", 600));
        int baseSeconds = Math.max(0, section.getInt("base-seconds", 0));
        boolean notifyPlayers = section.getBoolean("notify-players", true);

        Logger logger = plugin.getLogger();
        List<RankReward> rankRewards = new ArrayList<>(parseRanks(section.getConfigurationSection("ranks"), logger));
        rankRewards.addAll(parsePermissionNodes(section.getConfigurationSection("permission-nodes"), logger));
        // Prefer dedicated mcmmo.yml when present; fall back to config.yml's auto-flight-rewards.mcmmo
        List<SkillReward> skillRewards;
        try {
            com.ezflytime.bootstrap.ServiceRegistry sr = plugin.getServiceRegistry();
            com.ezflytime.config.ConfigManager cm = sr != null ? sr.getConfigManager() : null;
            com.ezflytime.mcmmo.McMMOConfig mmc = cm != null ? cm.getMcMMOConfig() : null;
            if (mmc != null && mmc.getConfig() != null) {
                skillRewards = parseSkills(mmc.getConfig(), logger);
            } else {
                skillRewards = parseSkills(section.getConfigurationSection("mcmmo"), logger);
            }
        } catch (Exception ex) {
            // Be resilient: fallback to old location on any error
            skillRewards = parseSkills(section.getConfigurationSection("mcmmo"), logger);
        }

        return new AutoRewardSettings(enabled, intervalSeconds, baseSeconds, notifyPlayers, rankRewards, skillRewards);
    }

    private static AutoRewardSettings disabled() {
        return new AutoRewardSettings(false, 0, 0, true, Collections.emptyList(), Collections.emptyList());
    }

    private static List<RankReward> parseRanks(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return Collections.emptyList();
        }

        List<RankReward> rewards = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            if (rankSection == null) {
                continue;
            }

            String permission = rankSection.getString("permission");
            if (permission == null || permission.trim().isEmpty()) {
                logger.warning("Rank reward '" + key + "' is missing a permission value. Skipping.");
                continue;
            }

            int seconds = Math.max(0, rankSection.getInt("seconds", 0));
            if (seconds <= 0) {
                continue;
            }

            rewards.add(new RankReward(permission.trim(), seconds));
        }
        return rewards;
    }

    private static List<RankReward> parsePermissionNodes(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return Collections.emptyList();
        }

        List<RankReward> rewards = new ArrayList<>();
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String permission = key;
            int seconds;

            if (value instanceof Number number) {
                seconds = number.intValue();
            } else if (value instanceof String stringValue) {
                try {
                    seconds = Integer.parseInt(stringValue.trim());
                } catch (NumberFormatException ex) {
                    logger.warning("Permission reward '" + key + "' has a non-numeric string value. Skipping.");
                    continue;
                }
            } else if (value instanceof ConfigurationSection valueSection) {
                permission = valueSection.getString("permission", permission);
                Object secondsObject = valueSection.get("seconds");
                if (secondsObject instanceof Number number) {
                    seconds = number.intValue();
                } else if (secondsObject instanceof String secondsString) {
                    try {
                        seconds = Integer.parseInt(secondsString.trim());
                    } catch (NumberFormatException ex) {
                        logger.warning("Permission reward '" + key + "' has a non-numeric seconds value. Skipping.");
                        continue;
                    }
                } else {
                    seconds = 0;
                }
            } else {
                logger.warning("Permission reward '" + key + "' has an invalid value. Skipping.");
                continue;
            }

            if (permission == null || permission.trim().isEmpty()) {
                logger.warning("Permission reward '" + key + "' is missing a permission value. Skipping.");
                continue;
            }

            seconds = Math.max(0, seconds);
            if (seconds <= 0) {
                continue;
            }

            rewards.add(new RankReward(permission.trim(), seconds));
        }
        return rewards;
    }

    private static List<SkillReward> parseSkills(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return Collections.emptyList();
        }

        boolean enabled = section.getBoolean("enabled", true);
        if (!enabled) {
            return Collections.emptyList();
        }

        ConfigurationSection skillsSection = section.getConfigurationSection("skills");
        if (skillsSection == null) {
            return Collections.emptyList();
        }

        List<SkillReward> rewards = new ArrayList<>();
        for (String key : skillsSection.getKeys(false)) {
            ConfigurationSection skillSection = skillsSection.getConfigurationSection(key);
            if (skillSection == null) {
                continue;
            }

            ConfigurationSection thresholdsSection = skillSection.getConfigurationSection("thresholds");
            if (thresholdsSection == null) {
                continue;
            }

            NavigableMap<Integer, Integer> thresholds = new TreeMap<>();
            for (Map.Entry<String, Object> entry : thresholdsSection.getValues(false).entrySet()) {
                try {
                    int level = Integer.parseInt(entry.getKey());
                    int seconds = Math.max(0, (entry.getValue() instanceof Number number) ? number.intValue() : Integer.parseInt(String.valueOf(entry.getValue())));
                    if (level < 0 || seconds <= 0) {
                        continue;
                    }
                    thresholds.put(level, seconds);
                } catch (NumberFormatException ex) {
                    logger.warning("Invalid mcMMO threshold key '" + entry.getKey() + "' for skill '" + key + "'.");
                }
            }

            if (thresholds.isEmpty()) {
                continue;
            }

            rewards.add(new SkillReward(key, thresholds));
        }
        return rewards;
    }

    boolean isEnabled() {
        return enabled;
    }

    int getIntervalSeconds() {
        return intervalSeconds;
    }

    int getBaseSeconds() {
        return baseSeconds;
    }

    boolean shouldNotifyPlayers() {
        return notifyPlayers;
    }

    boolean hasSkillRewards() {
        return !skillRewards.isEmpty();
    }

    int rankReward(Player player) {
        int total = 0;
        for (RankReward reward : rankRewards) {
            if (reward.appliesTo(player)) {
                total += reward.getSeconds();
            }
        }
        return total;
    }

    int skillReward(Player player, McMMOIntegration mcMMOIntegration) {
        if (mcMMOIntegration == null || !mcMMOIntegration.isAvailable()) {
            return 0;
        }
        int total = 0;
        for (SkillReward reward : skillRewards) {
            int level = mcMMOIntegration.getSkillLevel(player, reward.skillName());
            if (level < 0) {
                continue;
            }
            total += reward.secondsForLevel(level);
        }
        return total;
    }

    private static final class RankReward {
        private final String permission;
        private final int seconds;

        private RankReward(String permission, int seconds) {
            this.permission = permission;
            this.seconds = seconds;
        }

        private boolean appliesTo(Player player) {
            return player.hasPermission(permission);
        }

        private int getSeconds() {
            return seconds;
        }
    }

    private static class SkillReward {
        private final String skillName;
        private final NavigableMap<Integer, Integer> thresholds;

        SkillReward(String skillName, NavigableMap<Integer, Integer> thresholds) {
            this.skillName = skillName.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            this.thresholds = thresholds;
        }

        String skillName() {
            return skillName;
        }

        int secondsForLevel(int level) {
            Map.Entry<Integer, Integer> entry = thresholds.floorEntry(level);
            if (entry == null) {
                return 0;
            }
            return entry.getValue();
        }
    }
}
