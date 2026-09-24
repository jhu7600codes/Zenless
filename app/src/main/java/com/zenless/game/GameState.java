package com.zenless.game;

import android.content.Context;
import android.content.SharedPreferences;

/** Everything that gets saved. One instance lives for the whole app. */
public final class GameState {
    private static final String PREFS = "zenless_save";
    private static final int SAVE_VERSION = 1;

    // current run
    public double holos;
    public double runEarned;
    public long runTaps;
    public int door;
    public int[] buildings = new int[Building.ALL.length];
    public int[] upgrades = new int[Upgrade.ALL.length];

    // permanent
    public long stItems;
    public long stLifetime;
    public int rebirths;
    public int[] meta = new int[MetaUpgrade.ALL.length];
    public boolean adminUnlocked;
    public boolean adminEnabled;
    public double lifetimeEarned;
    public int entitiesSurvived;
    public int entitiesFailed;

    public long lastSaveTime;

    public void load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        holos = getDouble(p, "holos");
        runEarned = getDouble(p, "runEarned");
        runTaps = p.getLong("runTaps", 0);
        door = p.getInt("door", 0);
        for (int i = 0; i < buildings.length; i++) buildings[i] = p.getInt("b" + i, 0);
        for (int i = 0; i < upgrades.length; i++) upgrades[i] = p.getInt("u" + i, 0);
        stItems = p.getLong("stItems", 0);
        stLifetime = p.getLong("stLifetime", 0);
        rebirths = p.getInt("rebirths", 0);
        for (int i = 0; i < meta.length; i++) meta[i] = p.getInt("m" + i, 0);
        adminUnlocked = p.getBoolean("adminUnlocked", false);
        adminEnabled = adminUnlocked && p.getBoolean("adminEnabled", false);
        lifetimeEarned = getDouble(p, "lifetimeEarned");
        entitiesSurvived = p.getInt("survived", 0);
        entitiesFailed = p.getInt("failed", 0);
        lastSaveTime = p.getLong("lastSave", 0);
    }

    public void save(Context ctx) {
        lastSaveTime = System.currentTimeMillis();
        SharedPreferences.Editor e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        e.putInt("version", SAVE_VERSION);
        putDouble(e, "holos", holos);
        putDouble(e, "runEarned", runEarned);
        e.putLong("runTaps", runTaps);
        e.putInt("door", door);
        for (int i = 0; i < buildings.length; i++) e.putInt("b" + i, buildings[i]);
        for (int i = 0; i < upgrades.length; i++) e.putInt("u" + i, upgrades[i]);
        e.putLong("stItems", stItems);
        e.putLong("stLifetime", stLifetime);
        e.putInt("rebirths", rebirths);
        for (int i = 0; i < meta.length; i++) e.putInt("m" + i, meta[i]);
        e.putBoolean("adminUnlocked", adminUnlocked);
        e.putBoolean("adminEnabled", adminEnabled);
        putDouble(e, "lifetimeEarned", lifetimeEarned);
        e.putInt("survived", entitiesSurvived);
        e.putInt("failed", entitiesFailed);
        e.putLong("lastSave", lastSaveTime);
        e.apply();
    }

    /** Wipes the current run, keeps permanent stuff and applies meta head starts. */
    public void resetRun() {
        holos = 0;
        runEarned = 0;
        runTaps = 0;
        door = 0;
        for (int i = 0; i < buildings.length; i++) buildings[i] = 0;
        for (int i = 0; i < upgrades.length; i++) upgrades[i] = 0;
        holos += 1000.0 * meta[MetaUpgrade.HEAD_START];
        buildings[1] += 5 * meta[MetaUpgrade.STARTER_DRONES];
    }

    public void earn(double amount) {
        holos += amount;
        runEarned += amount;
        lifetimeEarned += amount;
    }

    private static double getDouble(SharedPreferences p, String key) {
        return Double.longBitsToDouble(p.getLong(key, Double.doubleToRawLongBits(0)));
    }

    private static void putDouble(SharedPreferences.Editor e, String key, double v) {
        e.putLong(key, Double.doubleToRawLongBits(v));
    }
}
