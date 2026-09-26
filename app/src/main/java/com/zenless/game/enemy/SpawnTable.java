package com.zenless.game.enemy;

import com.zenless.game.Difficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Doors / Rooms style spawning: every entity has a scripted first meeting at a fixed door,
 * after that it only has a small chance per door.
 */
public final class SpawnTable {
    /** doors after an encounter where nothing random spawns (scripted meetings still do) */
    public static final int QUIET_DOORS = 2;

    /** 0.001%: a meet-and-greet door brings the SUPER version, or door 1 brings ??? */
    public static final double RARE_CHANCE = 0.00001;

    private SpawnTable() {}

    /** Door of the first guaranteed meeting in a run. */
    public static int firstDoor(EnemyType t) {
        switch (t) {
            case RUSH: return 10;
            case A90: return 30;
            case FIGURE: return 50;
            case A90B: return 70;
            default: return Integer.MAX_VALUE;
        }
    }

    /** normal mode: figure is the only entity and shows up on a fixed rhythm instead */
    public static final int NORMAL_FIGURE_EVERY = 30;

    /** Figure is always waiting at the library (50) and the courtyard (100), like in Doors. */
    public static boolean scripted(EnemyType t, int door, Difficulty d) {
        if (d == Difficulty.NORMAL) return t == EnemyType.FIGURE && door % NORMAL_FIGURE_EVERY == 0;
        if (door == firstDoor(t)) return true;
        return t == EnemyType.FIGURE && door == 100;
    }

    /** Chance per door once the entity has been met, before difficulty. */
    public static double baseChance(EnemyType t) {
        switch (t) {
            case RUSH: return 0.08;
            case A90: return 0.06;
            case A90B: return 0.04;
            case FIGURE: return 0.02;
            default: return 0;
        }
    }

    public static double chance(EnemyType t, Difficulty d, int door) {
        if (!d.allows(t) || d == Difficulty.NORMAL) return 0;
        int after = t == EnemyType.FIGURE ? 100 : firstDoor(t);
        if (door <= after) return 0;
        return Math.min(1, baseChance(t) * d.chanceMult(t));
    }

    /**
     * Who comes through this door, or null.
     * @param quiet true right after an encounter, only scripted meetings can happen then
     */
    public static EnemyType roll(int door, Difficulty d, boolean quiet, Random rng) {
        if (door == 1 && rng.nextDouble() < RARE_CHANCE) return EnemyType.SECRET;
        for (EnemyType t : EnemyType.values()) {
            if (d.allows(t) && scripted(t, door, d)) return t;
        }
        if (quiet) return null;
        // random order so nobody is favored when several would roll at once
        List<EnemyType> order = new ArrayList<>();
        Collections.addAll(order, EnemyType.values());
        Collections.shuffle(order, rng);
        for (EnemyType t : order) {
            if (rng.nextDouble() < chance(t, d, door)) return t;
        }
        return null;
    }
}
