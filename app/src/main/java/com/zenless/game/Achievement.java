package com.zenless.game;

import android.content.Context;
import android.content.SharedPreferences;

import com.zenless.game.enemy.EnemyType;

import java.util.ArrayList;
import java.util.List;

/**
 * Achievements are shared by every save. Saves where the admin panel was ever turned on can't earn them.
 * Locked ones only show their flavor line.
 */
public enum Achievement {
    PEEKABOO("Peek-a-boo", "i stopped.", "survive A-90"),
    OUT_OF_MY_WAY("Out of my way", "IM WALKIN' 'ERE!", "survive Rush"),
    BE_CAREFUL("Be careful...", "Not to make a SOUND!", "survive Figure"),
    TRAFFIC_LIGHT("Traffic light", "DONT RUN A RED LIGHT!", "survive A-90B"),
    SSHH("Sshh!", "IT HIT THE CORNER", "survive A-DVD (???)"),
    NOT_SO_SUPER("Not so super", "was that supposed to be scary?", "survive a SUPER entity"),
    GOTCHA("Gotcha", "you'll get used to it.", "get jumpscared for the first time"),
    SURVIVOR("Survivor", "they keep coming back.", "survive 50 entities"),

    BORN_AGAIN("Born again", "it's all so familiar.", "rebirth once"),
    DEJA_VU("Déjà vu", "haven't we been here before?", "rebirth 10 times"),
    GROUNDHOG("Groundhog day", "same door, different day.", "rebirth 25 times"),
    ETERNAL("Eternal", "there is no end to this.", "rebirth 50 times"),
    MASOCHIST("Masochist", "you chose this.", "rebirth on SUPER HARD MODE"),
    THROUGH_THE_RIFT("Through the rift", "something came with you.", "carry something through the rift"),

    MILLIONAIRE("Millionaire", "the circle is pleased.", "earn 1M holos in one save"),
    BILLIONAIRE("Billionaire", "the circle is very pleased.", "earn 1B holos in one save"),
    TRILLIONAIRE("Trillionaire", "how.", "earn 1T holos in one save"),

    WARMED_UP("Warmed up", "tap tap tap.", "tap the circle 1,000 times"),
    STEEL_FINGER("Finger of steel", "your screen is crying.", "tap the circle 100,000 times"),

    CHECKOUT("Checkout", "the courtyard is that way.", "reach door 100 in one run"),
    ENDLESS("Endless hallway", "did the doors ever stop?", "reach door 500 in one run");

    public final String title;
    public final String desc;
    public final String how;

    Achievement(String title, String desc, String how) {
        this.title = title;
        this.desc = desc;
        this.how = how;
    }

    public boolean isUnlocked(Context ctx) {
        return Settings.prefs(ctx).getBoolean("ach_" + name(), false);
    }

    public static int unlockedCount(Context ctx) {
        int n = 0;
        for (Achievement a : values()) if (a.isUnlocked(ctx)) n++;
        return n;
    }

    /** Surviving an entity. */
    public static Achievement forEntity(EnemyType t) {
        switch (t) {
            case A90: return PEEKABOO;
            case RUSH: return OUT_OF_MY_WAY;
            case FIGURE: return BE_CAREFUL;
            case A90B: return TRAFFIC_LIGHT;
            case SECRET: return SSHH;
            default: return null;
        }
    }

    /**
     * Unlocks whatever the save now qualifies for. Returns the newly unlocked ones (for toasts).
     * Does nothing for admin saves.
     */
    public static List<Achievement> check(Context ctx, GameState s, Achievement... extra) {
        List<Achievement> fresh = new ArrayList<>();
        if (s.progressionDisabled()) return fresh;
        List<Achievement> earned = new ArrayList<>();
        for (Achievement a : extra) if (a != null) earned.add(a);
        if (s.survivedSuper > 0) earned.add(NOT_SO_SUPER);
        if (s.entitiesFailed > 0) earned.add(GOTCHA);
        if (s.entitiesSurvived >= 50) earned.add(SURVIVOR);
        if (s.rebirths >= 1) earned.add(BORN_AGAIN);
        if (s.rebirths >= 10) earned.add(DEJA_VU);
        if (s.rebirths >= 25) earned.add(GROUNDHOG);
        if (s.rebirths >= 50) earned.add(ETERNAL);
        if (s.riftEverUsed) earned.add(THROUGH_THE_RIFT);
        if (s.lifetimeEarned >= 1e6) earned.add(MILLIONAIRE);
        if (s.lifetimeEarned >= 1e9) earned.add(BILLIONAIRE);
        if (s.lifetimeEarned >= 1e12) earned.add(TRILLIONAIRE);
        if (s.lifetimeTaps >= 1_000) earned.add(WARMED_UP);
        if (s.lifetimeTaps >= 100_000) earned.add(STEEL_FINGER);
        if (s.door >= 100) earned.add(CHECKOUT);
        if (s.door >= 500) earned.add(ENDLESS);

        SharedPreferences.Editor e = null;
        for (Achievement a : earned) {
            if (a.isUnlocked(ctx) || fresh.contains(a)) continue;
            if (e == null) e = Settings.prefs(ctx).edit();
            e.putBoolean("ach_" + a.name(), true);
            fresh.add(a);
        }
        if (e != null) e.apply();
        return fresh;
    }
}
