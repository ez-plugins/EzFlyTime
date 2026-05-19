package com.ezflytime.command;

import com.ezflytime.EzFlyTimePlugin;
import com.ezflytime.bootstrap.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class CommandTestBase {

    protected EzFlyTimePlugin plugin;
    protected ServiceRegistry serviceRegistry;
    protected Server server;
    protected MockedStatic<Bukkit> mockedBukkit;
    private Map<String, String> messages = new HashMap<>();

    @BeforeEach
    void baseSetUp() {
        plugin = mock(EzFlyTimePlugin.class, Answers.RETURNS_DEEP_STUBS);
        serviceRegistry = mock(ServiceRegistry.class, Answers.RETURNS_DEEP_STUBS);
        server = mock(Server.class, Answers.RETURNS_DEEP_STUBS);

        when(plugin.getServiceRegistry()).thenReturn(serviceRegistry);
        when(plugin.getServer()).thenReturn(server);

        // Default: return the message key so tests can override specific keys as needed
        // Try to load test message fixtures from src/test/resources/messages_test.yml
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("messages_test.yml")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    boolean inMessages = false;
                    while ((line = r.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        if (trimmed.startsWith("messages:")) {
                            inMessages = true;
                            continue;
                        }
                        if (inMessages && trimmed.contains(":")) {
                            int idx = trimmed.indexOf(":");
                            String key = trimmed.substring(0, idx).trim();
                            String val = trimmed.substring(idx + 1).trim();
                            if (val.startsWith("\"") && val.endsWith("\"")) {
                                val = val.substring(1, val.length() - 1);
                            }
                            messages.put(key, val);
                            messages.put("messages." + key, val);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        when(plugin.getMessage(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    return messages.getOrDefault(key, key);
                });

        // Provide a mocked Bukkit static for tests that need it
        mockedBukkit = Mockito.mockStatic(Bukkit.class);
        mockedBukkit.when(Bukkit::getServer).thenReturn(server);
    }

    @AfterEach
    void baseTearDown() {
        if (mockedBukkit != null) mockedBukkit.close();
    }
}
