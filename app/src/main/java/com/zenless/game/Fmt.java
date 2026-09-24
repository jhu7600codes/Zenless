package com.zenless.game;

import java.util.Locale;

/** Formats Holos as 1.2K, 3.4M, 5.6B ... */
public final class Fmt {
    private static final String[] SUFFIX = {
            "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"
    };

    private Fmt() {}

    public static String holos(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "∞";
        boolean neg = v < 0;
        v = Math.abs(v);
        String out;
        if (v < 1000) {
            // show one decimal only for small fractional values like 0.1/sec
            out = (v < 10 && v != Math.floor(v))
                    ? String.format(Locale.US, "%.1f", v)
                    : String.format(Locale.US, "%d", (long) Math.floor(v));
        } else {
            int tier = (int) (Math.log10(v) / 3);
            if (tier >= SUFFIX.length) {
                out = String.format(Locale.US, "%.2e", v);
            } else {
                double scaled = v / Math.pow(1000, tier);
                // 999.95K would round to 1000.0K, bump to the next suffix instead
                if (scaled >= 999.95 && tier + 1 < SUFFIX.length) {
                    tier++;
                    scaled = v / Math.pow(1000, tier);
                }
                out = String.format(Locale.US, "%.1f%s", scaled, SUFFIX[tier]);
            }
        }
        return neg ? "-" + out : out;
    }
}
