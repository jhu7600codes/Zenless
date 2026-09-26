package com.zenless.game;

import android.content.Context;
import android.content.SharedPreferences;

/** App wide settings, shared by every save. */
public final class Settings {
    private static final String PREFS = "zenless_global";

    public static boolean sound = true;
    public static boolean haptics = true;
    public static boolean reduceFlashing = false;

    private Settings() {}

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void load(Context ctx) {
        SharedPreferences p = prefs(ctx);
        sound = p.getBoolean("sound", true);
        haptics = p.getBoolean("haptics", true);
        reduceFlashing = p.getBoolean("reduceFlashing", false);
    }

    public static void save(Context ctx) {
        prefs(ctx).edit()
                .putBoolean("sound", sound)
                .putBoolean("haptics", haptics)
                .putBoolean("reduceFlashing", reduceFlashing)
                .apply();
    }
}
