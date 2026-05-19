package com.ezflytime.util;

import java.util.ArrayList;
import java.util.List;

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
}
