package com.ezflytime.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatterTest {

    @Test
    void formatsHoursMinutesSecondsCompactly() {
        assertEquals("1h 5s", TimeFormatter.formatCompact(3605));
    }

    @Test
    void formatsMinutesAndSeconds() {
        assertEquals("1m 5s", TimeFormatter.formatCompact(65));
    }

    @Test
    void formatsZeroAsSeconds() {
        assertEquals("0s", TimeFormatter.formatCompact(0));
    }

    @Test
    void formatsClockWithSecondsOnly() {
        assertEquals("02:05", TimeFormatter.formatClock(125));
    }

    @Test
    void formatsClockWithHours() {
        assertEquals("01:01:01", TimeFormatter.formatClock(3661));
    }

    @Test
    void formatsClockZeroAsZeroZero() {
        assertEquals("00:00", TimeFormatter.formatClock(0));
    }

    @Test
    void formatsClockMinutesOnly() {
        assertEquals("01:05", TimeFormatter.formatClock(65));
    }

    @Test
    void formatsPatternHoursMinutesSeconds() {
        assertEquals("01:01:01", TimeFormatter.formatPattern(3661, "HH:MM:SS"));
    }

    @Test
    void formatsPatternMinutesSeconds() {
        assertEquals("02:05", TimeFormatter.formatPattern(125, "MM:SS"));
    }

    @Test
    void formatsPatternUnpadded() {
        assertEquals("1:1:1", TimeFormatter.formatPattern(3661, "H:M:S"));
    }

    @Test
    void formatsPatternMixed() {
        assertEquals("1h 1m 1s", TimeFormatter.formatPattern(3661, "Hh Mm Ss"));
    }

    @Test
    void formatsPatternZero() {
        assertEquals("00:00", TimeFormatter.formatPattern(0, "MM:SS"));
    }

    @Test
    void formatsPatternDaysPadded() {
        assertEquals("01:00:00:00", TimeFormatter.formatPattern(86400, "DD:HH:MM:SS"));
    }

    @Test
    void formatsPatternDaysUnpadded() {
        assertEquals("1d 0h 0m 0s", TimeFormatter.formatPattern(86400, "Dd Hh Mm Ss"));
    }

    @Test
    void formatsPatternDaysOnly() {
        assertEquals("03", TimeFormatter.formatPattern(259200, "DD"));
    }

    @Test
    void dispatcherUsesCompactForCompactKeyword() {
        assertEquals("1m 5s", TimeFormatter.format(65, "compact"));
    }

    @Test
    void dispatcherUsesClockForClockKeyword() {
        assertEquals("02:05", TimeFormatter.format(125, "clock"));
    }

    @Test
    void dispatcherTreatsUnknownAsPattern() {
        assertEquals("02:05", TimeFormatter.format(125, "MM:SS"));
    }

    @Test
    void dispatcherFallsBackToCompactForNull() {
        assertEquals("1m 5s", TimeFormatter.format(65, null));
    }

    @Test
    void dispatcherFallsBackToCompactForEmpty() {
        assertEquals("1m 5s", TimeFormatter.format(65, ""));
    }
}
