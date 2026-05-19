package com.ezflytime.messages;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Ensures all language message files contain the same keys as English.
 */
public class MessagesConsistencyTest {

    private static final Pattern KEY_PATTERN = Pattern.compile("^\\s+([^:\\s]+):");

    @Test
    public void testLanguageKeysMatchEnglish() throws IOException {
        Path resourcesDir = Paths.get("src/main/resources");
        if (!Files.exists(resourcesDir)) {
            fail("Resources folder not found: src/main/resources");
        }

        // Collect message files matching messages_*.yml from resources root and resources/messages/
        Map<String, Set<String>> fileKeys = new HashMap<>();
        List<Path> scanDirs = new java.util.ArrayList<>();
        scanDirs.add(resourcesDir);
        Path nested = resourcesDir.resolve("messages");
        if (Files.exists(nested) && Files.isDirectory(nested)) scanDirs.add(nested);
        for (Path dir : scanDirs) {
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.getFileName().toString().startsWith("messages_") && p.getFileName().toString().endsWith(".yml"))
                        .forEach(p -> {
                            try {
                                fileKeys.put(p.getFileName().toString(), parseMessageKeys(p));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }

        if (!fileKeys.containsKey("messages_en.yml")) {
            fail("Reference English messages_en.yml not found in src/main/resources");
        }

        Set<String> enKeys = fileKeys.get("messages_en.yml");
        StringBuilder errors = new StringBuilder();

        for (Map.Entry<String, Set<String>> entry : fileKeys.entrySet()) {
            String file = entry.getKey();
            if ("messages_en.yml".equals(file)) continue;
            Set<String> keys = entry.getValue();

            Set<String> missing = new HashSet<>(enKeys);
            missing.removeAll(keys);
            Set<String> extra = new HashSet<>(keys);
            extra.removeAll(enKeys);

            if (!missing.isEmpty() || !extra.isEmpty()) {
                errors.append("Language file ").append(file).append(" differences:\n");
                if (!missing.isEmpty()) {
                    errors.append("  Missing keys: ").append(missing).append("\n");
                }
                if (!extra.isEmpty()) {
                    errors.append("  Extra keys:   ").append(extra).append("\n");
                }
                errors.append("\n");
            }
        }

        if (errors.length() > 0) {
            fail(errors.toString());
        }
    }

    private Set<String> parseMessageKeys(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Set<String> keys = new HashSet<>();
        boolean inMessages = false;
        for (String raw : lines) {
            String line = raw.replaceAll("\t", "    ");
            if (!inMessages) {
                if (line.trim().startsWith("messages:")) {
                    inMessages = true;
                }
                continue;
            }
            if (line.trim().isEmpty()) continue;
            // stop if we hit a top-level non-indented section
            if (!line.startsWith(" ") && !line.startsWith("\t")) break;

            Matcher m = KEY_PATTERN.matcher(line);
            if (m.find()) {
                keys.add(m.group(1));
            }
        }
        return keys;
    }
}
