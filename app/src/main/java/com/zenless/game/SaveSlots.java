package com.zenless.game;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/** Three save slots, each its own SharedPreferences file. Slot 0 is the throwaway tutorial practice save. */
public final class SaveSlots {
    public static final int COUNT = 3;
    public static final int PRACTICE = 0;
    private static final String OLD_SINGLE_SAVE = "zenless_save";

    private SaveSlots() {}

    public static String prefsName(int slot) {
        return slot == PRACTICE ? "zenless_practice" : "zenless_save_" + slot;
    }

    public static boolean exists(Context ctx, int slot) {
        return ctx.getSharedPreferences(prefsName(slot), Context.MODE_PRIVATE).contains("version");
    }

    public static void delete(Context ctx, int slot) {
        ctx.getSharedPreferences(prefsName(slot), Context.MODE_PRIVATE).edit().clear().commit();
    }

    /** The single save from older versions becomes slot 1. */
    @SuppressWarnings("unchecked")
    public static void migrate(Context ctx) {
        SharedPreferences old = ctx.getSharedPreferences(OLD_SINGLE_SAVE, Context.MODE_PRIVATE);
        if (!old.contains("version") || exists(ctx, 1)) return;
        SharedPreferences.Editor e = ctx.getSharedPreferences(prefsName(1), Context.MODE_PRIVATE).edit();
        for (Map.Entry<String, ?> en : old.getAll().entrySet()) {
            Object v = en.getValue();
            if (v instanceof Long) e.putLong(en.getKey(), (Long) v);
            else if (v instanceof Integer) e.putInt(en.getKey(), (Integer) v);
            else if (v instanceof Boolean) e.putBoolean(en.getKey(), (Boolean) v);
            else if (v instanceof Float) e.putFloat(en.getKey(), (Float) v);
            else if (v instanceof String) e.putString(en.getKey(), (String) v);
        }
        e.commit();
        old.edit().clear().commit();
    }

    public static GameState load(Context ctx, int slot) {
        GameState s = new GameState(prefsName(slot));
        s.load(ctx);
        return s;
    }
}
