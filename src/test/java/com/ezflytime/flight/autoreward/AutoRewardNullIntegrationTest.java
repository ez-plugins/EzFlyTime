package com.ezflytime.flight.autoreward;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the NPE in {@link AutoRewardSettings#skillReward(Player, McMMOIntegration)}
 * when {@code mcMMOIntegration} is {@code null}.
 *
 * <h2>Root cause</h2>
 * {@link AutoFlightTimeDistributor} explicitly sets {@code mcMMOIntegration = null}
 * when the mcMMO integration is disabled via {@code mcmmo.yml}.  The {@code runDistribution}
 * loop then calls {@code settings.skillReward(player, mcMMOIntegration)} passing the
 * {@code null} reference.  Inside {@code skillReward}, the first line is:
 * <pre>
 *   if (!mcMMOIntegration.isAvailable()) { ... }
 * </pre>
 * which dereferences {@code null} → {@link NullPointerException}.
 *
 * <h2>Precondition</h2>
 * {@code settings.hasSkillRewards()} must be {@code true}.  The guard in
 * {@code runDistribution} only logs a warning but does NOT skip the call, so any
 * server that disables mcMMO but keeps skill-reward config entries will hit this crash
 * on every auto-reward distribution cycle.
 *
 * <h2>Expected fix</h2>
 * Add a {@code null} guard at the top of {@link AutoRewardSettings#skillReward}:
 * <pre>
 *   if (mcMMOIntegration == null || !mcMMOIntegration.isAvailable()) { return 0; }
 * </pre>
 *
 * <h2>Current status</h2>
 * The test FAILS with {@link NullPointerException}.
 */
class AutoRewardNullIntegrationTest {

    @Test
    void skillRewardWithNullIntegrationShouldReturnZeroNotThrowNpe() {
        // Arrange – plugin with RETURNS_DEEP_STUBS so serviceRegistry / configManager
        // return non-null mocks by default, avoiding NPEs in fromConfiguration itself.
        EzFlyTimePlugin plugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);

        YamlConfiguration config = new YamlConfiguration();
        config.set("auto-flight-rewards.enabled", true);
        config.set("auto-flight-rewards.interval-seconds", 600);
        config.set("auto-flight-rewards.base-seconds", 0);
        config.set("auto-flight-rewards.notify-players", false);
        // Configure a skill reward in the embedded mcmmo block so hasSkillRewards() is true.
        config.set("auto-flight-rewards.mcmmo.enabled", true);
        config.set("auto-flight-rewards.mcmmo.skills.mining.thresholds.100", 30);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AutoRewardNullIntegrationTest"));
        // Return null for getMcMMOConfig() so fromConfiguration falls back to the
        // embedded mcmmo block in config.yml (the path that sets up skill rewards).
        when(plugin.getServiceRegistry().getConfigManager().getMcMMOConfig()).thenReturn(null);

        AutoRewardSettings settings = AutoRewardSettings.fromConfiguration(plugin);

        // Pre-condition: the config must have parsed at least one skill reward.
        // If this fails the test setup is wrong, not the production code.
        assert settings.hasSkillRewards()
                : "test setup error: no skill rewards parsed from config";

        Player player = mock(Player.class);

        // Act & Assert – passing null mirrors what AutoFlightTimeDistributor does when
        // mcMMO is disabled.  The call must not throw NPE; it must return 0.
        int result = assertDoesNotThrow(
                () -> settings.skillReward(player, null),
                "skillReward should not throw NullPointerException when integration is null"
        );
        assertEquals(0, result, "skillReward with a null integration should return 0");
    }
}
