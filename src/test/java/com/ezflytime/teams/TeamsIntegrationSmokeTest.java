package com.ezflytime.teams;

import com.ezflytime.EzFlyTimePlugin;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Smoke tests for the TeamsAPI soft-dependency integration.
 *
 * <p>Parameterized across every server-software / Minecraft-version combination
 * that EzFlyTime supports, these tests guard against a recurrence of the
 * {@code NoClassDefFoundError: com/skyblockexp/teamsapi/api/TeamsSubcommand} crash
 * that occurred on startup when TeamsAPI was absent.</p>
 *
 * <h2>Root cause (fixed in 3.0.1 / 3.0.2)</h2>
 * The JVM's bytecode verifier resolves {@code FlySubcommand} (and transitively
 * {@code TeamsSubcommand}) the moment {@code TeamsIntegration} is first loaded or
 * its methods are first invoked — before any runtime {@code getPlugin("TeamsAPI")}
 * guard inside the class can fire.  {@code StartupBootstrap} now guards
 * {@code new TeamsIntegration(...)} behind a plugin-presence check so
 * {@code TeamsIntegration} (and its TeamsAPI dependencies) is never class-loaded
 * when TeamsAPI is absent.
 *
 * <h2>What is tested</h2>
 * <ul>
 *   <li><b>TeamsAPI absent</b> — {@link TeamsIntegration#registerIfAvailable()} and
 *       {@link TeamsIntegration#unregister()} must not throw any exception.</li>
 *   <li><b>TeamsAPI present</b> — {@link TeamsIntegration#registerIfAvailable()} must
 *       not throw; any internal failure is caught by the {@code catch (Throwable)}
 *       inside the method.</li>
 *   <li><b>Integration disabled in config</b> — {@link TeamsIntegration#registerIfAvailable()}
 *       must not throw and must never touch the {@link PluginManager}.</li>
 * </ul>
 *
 * <h2>Coverage matrix</h2>
 * <pre>
 *  Server software : Paper, Spigot, Purpur, Folia
 *  Minecraft       : 1.16.5, 1.17.1, 1.18.2, 1.19.4, 1.20.6, 1.21.4, 1.21.5
 * </pre>
 */
@DisplayName("TeamsIntegration smoke test – all server software × MC versions")
class TeamsIntegrationSmokeTest {

    // -------------------------------------------------------------------------
    // Compatibility matrix
    // -------------------------------------------------------------------------

    /**
     * Cross-product of every server software / Minecraft version combination that
     * EzFlyTime declares as compatible and on which TeamsAPI can be installed.
     */
    static Stream<Arguments> compatibleServerMatrix() {
        String[] software = {"Paper", "Spigot", "Purpur", "Folia"};
        String[] versions  = {"1.16.5", "1.17.1", "1.18.2", "1.19.4", "1.20.6", "1.21.4", "1.21.5"};
        Stream.Builder<Arguments> builder = Stream.builder();
        for (String sw : software) {
            for (String v : versions) {
                builder.add(Arguments.of(sw, v));
            }
        }
        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Mock factory
    // -------------------------------------------------------------------------

    /**
     * Builds a minimal plugin mock for the given server/version combination.
     *
     * @param serverSoftware      display name returned by {@code Server.getName()}
     * @param mcVersion           version string returned by {@code Server.getVersion()}
     * @param teamsApiPresent     whether {@code getPlugin("TeamsAPI")} should return a plugin
     * @param teamsEnabledInConfig value of {@code teams.enabled} in config.yml
     * @return configured plugin mock and the captured {@link PluginManager} mock
     */
    private MockSetup buildMock(String serverSoftware, String mcVersion,
                                boolean teamsApiPresent, boolean teamsEnabledInConfig) {
        EzFlyTimePlugin plugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);
        Server server          = mock(Server.class, Answers.RETURNS_DEEP_STUBS);
        PluginManager pm       = mock(PluginManager.class);

        when(plugin.getServer()).thenReturn(server);
        when(server.getName()).thenReturn(serverSoftware);
        when(server.getVersion()).thenReturn("git-" + serverSoftware + "-" + mcVersion);
        when(server.getPluginManager()).thenReturn(pm);
        when(pm.getPlugin("TeamsAPI")).thenReturn(teamsApiPresent ? mock(Plugin.class) : null);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("TeamsIntegrationSmokeTest"));

        YamlConfiguration config = new YamlConfiguration();
        config.set("teams.enabled", teamsEnabledInConfig);
        config.set("teams.claimed-chunks-only", false);
        when(plugin.getConfig()).thenReturn(config);

        return new MockSetup(plugin, pm);
    }

    private record MockSetup(EzFlyTimePlugin plugin, PluginManager pluginManager) {}

    // -------------------------------------------------------------------------
    // TeamsAPI absent
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{0} / MC {1}]")
    @MethodSource("compatibleServerMatrix")
    @DisplayName("registerIfAvailable does not throw when TeamsAPI is absent")
    void registerIfAvailableDoesNotThrowWhenTeamsApiIsAbsent(String software, String mcVersion) {
        MockSetup mocks = buildMock(software, mcVersion, /* teamsApiPresent= */ false, true);
        TeamsIntegration integration = new TeamsIntegration(mocks.plugin());

        assertDoesNotThrow(
                integration::registerIfAvailable,
                software + " " + mcVersion + ": registerIfAvailable() must not throw when TeamsAPI is absent");
    }

    @ParameterizedTest(name = "[{0} / MC {1}]")
    @MethodSource("compatibleServerMatrix")
    @DisplayName("unregister does not throw after a no-op register when TeamsAPI is absent")
    void unregisterDoesNotThrowAfterAbsentRegistration(String software, String mcVersion) {
        MockSetup mocks = buildMock(software, mcVersion, false, true);
        TeamsIntegration integration = new TeamsIntegration(mocks.plugin());
        integration.registerIfAvailable(); // no-op – TeamsAPI absent

        assertDoesNotThrow(
                integration::unregister,
                software + " " + mcVersion + ": unregister() must not throw after a no-op registerIfAvailable()");
    }

    // -------------------------------------------------------------------------
    // TeamsAPI present
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{0} / MC {1}]")
    @MethodSource("compatibleServerMatrix")
    @DisplayName("registerIfAvailable does not throw when TeamsAPI is present")
    void registerIfAvailableDoesNotThrowWhenTeamsApiIsPresent(String software, String mcVersion) {
        MockSetup mocks = buildMock(software, mcVersion, /* teamsApiPresent= */ true, true);
        TeamsIntegration integration = new TeamsIntegration(mocks.plugin());

        // TeamsAPI.registerSubcommand() is the real static method on the test classpath.
        // Any internal failure (e.g. no Bukkit ServicesManager at test time) is wrapped in
        // catch(Throwable) inside registerIfAvailable(), so it must never propagate here.
        assertDoesNotThrow(
                integration::registerIfAvailable,
                software + " " + mcVersion + ": registerIfAvailable() must not throw when TeamsAPI is present");

        // Clean up: remove any subcommand registered into the static TeamsAPI state to
        // prevent cross-test interference.
        assertDoesNotThrow(integration::unregister);
    }

    // -------------------------------------------------------------------------
    // Integration disabled in config
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{0} / MC {1}]")
    @MethodSource("compatibleServerMatrix")
    @DisplayName("registerIfAvailable does not touch PluginManager when teams.enabled is false")
    void registerIfAvailableSkipsPluginManagerWhenDisabledInConfig(String software, String mcVersion) {
        MockSetup mocks = buildMock(software, mcVersion, false, /* teamsEnabledInConfig= */ false);
        TeamsIntegration integration = new TeamsIntegration(mocks.plugin());

        assertDoesNotThrow(
                integration::registerIfAvailable,
                software + " " + mcVersion + ": registerIfAvailable() with teams.enabled=false must not throw");

        // When teams.enabled=false the method must return before ever consulting the
        // plugin manager, regardless of whether TeamsAPI is installed.
        verify(mocks.pluginManager(), never()).getPlugin(anyString());
    }
}
