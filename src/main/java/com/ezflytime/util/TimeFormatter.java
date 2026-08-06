package com.ezflytime.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utility methods for converting durations into user-facing strings.
 */
public final class TimeFormatter {

    private TimeFormatter() {
        // Utility class
    }

    /**
     * Formats the given amount of seconds in a compact {@code 1h 2m 3s} style.
     *
     * @param totalSeconds the number of seconds to format
     * @return a human-readable representation of the duration
     */
    public static String formatCompact(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>();
        if (hours > 0) {
            parts.add(hours + "h");
        }
        if (minutes > 0) {
            parts.add(minutes + "m");
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add(seconds + "s");
        }

        return String.join(" ", parts);
    }

    /**
     * Formats the given amount of seconds as a clock-style {@code HH:MM:SS} or {@code MM:SS} string.
     *
     * @param totalSeconds the number of seconds to format
     * @return a zero-padded clock-style representation of the duration
     */
    public static String formatClock(int totalSeconds) {
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

    /**
     * Formats the given amount of seconds using a pattern string.
     *
     * <p>Supported tokens (case-sensitive):
     * <ul>
     *   <li>{@code DD} — days, zero-padded to 2 digits</li>
     *   <li>{@code D} — days, unpadded</li>
     *   <li>{@code HH} — hours, zero-padded to 2 digits</li>
     *   <li>{@code H} — hours, unpadded</li>
     *   <li>{@code MM} — minutes, zero-padded to 2 digits</li>
     *   <li>{@code M} — minutes, unpadded</li>
     *   <li>{@code SS} — seconds, zero-padded to 2 digits</li>
     *   <li>{@code S} — seconds, unpadded</li>
     * </ul>
     * All other characters are treated as literals.
     *
     * @param totalSeconds the number of seconds to format
     * @param pattern the format pattern, e.g. {@code Dd HH:MM:SS} or {@code MM:SS}
     * @return a formatted representation of the duration
     */
    public static String formatPattern(int totalSeconds, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return formatCompact(totalSeconds);
        }

        int total = Math.max(0, totalSeconds);
        int days = total / 86400;
        int hours = (total % 86400) / 3600;
        int minutes = (total % 3600) / 60;
        int seconds = total % 60;

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            if (i + 1 < pattern.length()) {
                String two = pattern.substring(i, i + 2);
                if ("DD".equals(two)) {
                    result.append(String.format("%02d", days));
                    i += 2;
                    continue;
                } else if ("HH".equals(two)) {
                    result.append(String.format("%02d", hours));
                    i += 2;
                    continue;
                } else if ("MM".equals(two)) {
                    result.append(String.format("%02d", minutes));
                    i += 2;
                    continue;
                } else if ("SS".equals(two)) {
                    result.append(String.format("%02d", seconds));
                    i += 2;
                    continue;
                }
            }

            char c = pattern.charAt(i);
            if (c == 'D') {
                result.append(days);
            } else if (c == 'H') {
                result.append(hours);
            } else if (c == 'M') {
                result.append(minutes);
            } else if (c == 'S') {
                result.append(seconds);
            } else {
                result.append(c);
            }
            i++;
        }

        return result.toString();
    }

    /**
     * Formats the given amount of seconds using the specified format.
     *
     * <p>The special values {@code compact} and {@code clock} are supported for
     * backward compatibility. Any other value is treated as a pattern string
     * (see {@link #formatPattern(int, String)}).
     *
     * @param totalSeconds the number of seconds to format
     * @param format the format name or pattern
     * @return a formatted representation of the duration
     */
    public static String format(int totalSeconds, String format) {
        if (format == null || format.trim().isEmpty()) {
            return formatCompact(totalSeconds);
        }
        String normalized = format.trim();
        if ("compact".equalsIgnoreCase(normalized)) {
            return formatCompact(totalSeconds);
        }
        if ("clock".equalsIgnoreCase(normalized)) {
            return formatClock(totalSeconds);
        }
        return formatPattern(totalSeconds, normalized);
    }
}
