package com.ezflytime.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoneyFormatterTest {

    @Test
    void parseSimpleNumber() {
        assertEquals(123.0, MoneyFormatter.parse("123"), 0.0001);
    }

    @Test
    void parseWithSuffixes() {
        assertEquals(1_500.0, MoneyFormatter.parse("1.5k"), 0.0001);
        assertEquals(2_000_000.0, MoneyFormatter.parse("2m"), 0.0001);
        assertEquals(3_000_000_000.0, MoneyFormatter.parse("3b"), 0.0001);
    }

    @Test
    void formatSmallAndLargeValues() {
        assertEquals("123", MoneyFormatter.format(123));
        assertEquals("1.5k", MoneyFormatter.format(1500));
        assertEquals("2m", MoneyFormatter.format(2_000_000));
        assertEquals("1.2b", MoneyFormatter.format(1_200_000_000));
    }

    @Test
    void parseNullThrows() {
        assertThrows(NumberFormatException.class, () -> MoneyFormatter.parse(null));
    }
}
