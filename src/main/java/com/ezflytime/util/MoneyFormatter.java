package com.ezflytime.util;

import java.text.DecimalFormat;

public final class MoneyFormatter {

    private MoneyFormatter() {}

    public static double parse(String input) throws NumberFormatException {
        if (input == null) throw new NumberFormatException("null");
        String s = input.trim().toLowerCase().replace(",", "");
        if (s.isEmpty()) throw new NumberFormatException("empty");

        char last = s.charAt(s.length() - 1);
        double multiplier = 1.0;
        if (last == 'k' || last == 'm' || last == 'b' || last == 't') {
            String num = s.substring(0, s.length() - 1).trim();
            if (num.isEmpty()) throw new NumberFormatException(input);
            switch (last) {
                case 'k': multiplier = 1_000d; break;
                case 'm': multiplier = 1_000_000d; break;
                case 'b': multiplier = 1_000_000_000d; break;
                case 't': multiplier = 1_000_000_000_000d; break;
            }
            return Double.parseDouble(num) * multiplier;
        }

        return Double.parseDouble(s);
    }

    public static String format(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return String.valueOf(value);
        double abs = Math.abs(value);
        String suffix = "";
        double v = value;
        if (abs >= 1_000_000_000_000d) { v = value / 1_000_000_000_000d; suffix = "t"; }
        else if (abs >= 1_000_000_000d) { v = value / 1_000_000_000d; suffix = "b"; }
        else if (abs >= 1_000_000d) { v = value / 1_000_000d; suffix = "m"; }
        else if (abs >= 1_000d) { v = value / 1_000d; suffix = "k"; }

        if (!suffix.isEmpty()) {
            DecimalFormat df = new DecimalFormat("0.#");
            return df.format(v) + suffix;
        }

        // Small values: show without suffix, trim .0 when integer
        if (value == (long) value) return String.format("%d", (long) value);
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(value);
    }
}
