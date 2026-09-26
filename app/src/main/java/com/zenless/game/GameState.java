package com.zenless.game;

import android.content.Context;
import android.content.SharedPreferences;

/** Everything that gets saved in one save slot. */
public final class GameState {
    private static final int SAVE_VERSION = 2;

    public static final int RIFT_EMPTY = -1, RIFT_BUILDING = 0, RIFT_UPGRADE = 1;

    private final String prefs;

    public GameState() {
        this(SaveSlots.prefsName(1));
    }

    public GameState(String prefsName) {
        this.prefs = prefsName;
    }

    // current run
    public double holos;
    public double runEarned;
    public long runTaps;
    public int door;
    /** Difficulty ordinal, -1 until the player picks one for this run. */
    public int difficulty = -1;
    public int[] buildings = new int[Building.ALL.length];
    public int[] upgrades = new int[Upgrade.ALL.length];

    // permanent
    public long stItems;
    public long stLifetime;
    public int rebirths;
    public int[] meta = new int[MetaUpgrade.ALL.length];
    public boolean adminUnlocked;
    public boolean adminEnabled;
    /** set the first time the admin panel is switched on, never cleared: no achievements, no rift */
    public boolean adminEverUsed;
    /** one building stack or upgrade tier carried into the next run */
    public int riftKind = RIFT_EMPTY;
    public int riftIndex;
    public boolean riftEverUsed;
    public long lifetimeTaps;

    // admin panel cheats
    public boolean cheatNoEntities;
    public boolean cheatInvincible;
    public boolean cheatAutoClick;
    public int cheatIncomeMult = 1;
    public int survivedSuper;
    public double lifetimeEarned;
    public int entitiesSurvived;
    public int entitiesFailed;

    public long lastSaveTime;

    public void load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(prefs, Context.MODE_PRIVATE);
        holos = getDouble(p, "holos");
        runEarned = getDouble(p, "runEarned");
        runTaps = p.getLong("runTaps", 0);
        door = p.getInt("door", 0);
        difficulty = p.getInt("difficulty", -1);
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
        // saves from before this flag existed count as used if the panel is on right now
        adminEverUsed = p.getBoolean("adminEverUsed", adminEnabled);
        riftKind = p.getInt("riftKind", RIFT_EMPTY);
        riftIndex = p.getInt("riftIndex", 0);
        riftEverUsed = p.getBoolean("riftEverUsed", false);
        lifetimeTaps = p.getLong("lifetimeTaps", runTaps);
        survivedSuper = p.getInt("survivedSuper", 0);
        cheatNoEntities = p.getBoolean("cheatNoEntities", false);
        cheatInvincible = p.getBoolean("cheatInvincible", false);
        cheatAutoClick = p.getBoolean("cheatAutoClick", false);
        cheatIncomeMult = Math.max(1, p.getInt("cheatIncomeMult", 1));
    }

    /** Admin panel was used at some point: achievements and the rift are off for good. */
    public boolean progressionDisabled() {
        return adminEverUsed;
    }

    public void save(Context ctx) {
        lastSaveTime = System.currentTimeMillis();
        SharedPreferences.Editor e = ctx.getSharedPreferences(prefs, Context.MODE_PRIVATE).edit();
        e.putInt("version", SAVE_VERSION);
        putDouble(e, "holos", holos);
        putDouble(e, "runEarned", runEarned);
        e.putLong("runTaps", runTaps);
        e.putInt("door", door);
        e.putInt("difficulty", difficulty);
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
        e.putBoolean("adminEverUsed", adminEverUsed);
        e.putInt("riftKind", riftKind);
        e.putInt("riftIndex", riftIndex);
        e.putBoolean("riftEverUsed", riftEverUsed);
        e.putLong("lifetimeTaps", lifetimeTaps);
        e.putInt("survivedSuper", survivedSuper);
        e.putBoolean("cheatNoEntities", cheatNoEntities);
        e.putBoolean("cheatInvincible", cheatInvincible);
        e.putBoolean("cheatAutoClick", cheatAutoClick);
        e.putInt("cheatIncomeMult", cheatIncomeMult);
        e.apply();
    }

    /** What's in the rift, e.g. "Tap Drone x25", or null. */
    public String riftLabel() {
        if (riftKind == RIFT_BUILDING) return Building.ALL[riftIndex].name + " x" + buildings[riftIndex];
        if (riftKind == RIFT_UPGRADE) return Upgrade.ALL[riftIndex].name + " tier " + upgrades[riftIndex];
        return null;
    }

    /** Wipes the current run, keeps permanent stuff, applies meta head starts and empties the rift into it. */
    public void resetRun() {
        int riftAmount = riftKind == RIFT_BUILDING ? buildings[riftIndex]
                : riftKind == RIFT_UPGRADE ? upgrades[riftIndex] : 0;
        int kind = progressionDisabled() ? RIFT_EMPTY : riftKind;
        holos = 0;
        runEarned = 0;
        runTaps = 0;
        door = 0;
        difficulty = -1;
        for (int i = 0; i < buildings.length; i++) buildings[i] = 0;
        for (int i = 0; i < upgrades.length; i++) upgrades[i] = 0;
        holos += 1000.0 * meta[MetaUpgrade.HEAD_START];
        buildings[1] += 5 * meta[MetaUpgrade.STARTER_DRONES];
        if (kind == RIFT_BUILDING) buildings[riftIndex] += riftAmount;
        else if (kind == RIFT_UPGRADE) upgrades[riftIndex] = Math.max(upgrades[riftIndex], riftAmount);
        riftKind = RIFT_EMPTY;
    }

    public Difficulty diff() {
        return Difficulty.of(difficulty);
    }

    public void tapped() {
        runTaps++;
        lifetimeTaps++;
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
