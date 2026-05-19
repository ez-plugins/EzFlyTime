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
}
