package com.zenless.game;

import com.zenless.game.enemy.EnemyType;

/** Picked at the start of every run. Changes shop prices, upgrade strength and entity doors. */
public enum Difficulty {
    EASY("Easy", "no entities, cheaper shop", 0.8, 1.0, 0.0, 1.0),
    NORMAL("Normal", "only Figure, cheaper shop", 0.8, 1.0, 0.49, 1.0),
    HARD("Hard", "every entity, normal spawns and prices", 1.0, 1.0, 0.49, 1.0),
    EXTREME("Extreme", "double spawns, more A-90B in his original look", 1.0, 1.0, 0.98, 1.0),
    SUPER_HARD("SUPER HARD MODE", "+50% prices, -50% power, +547% spawns, 3x faster doors", 1.5, 0.5, 1.0, 3.0);

    public final String label;
    public final String desc;
    /** multiplier on shop and upgrade prices */
    public final double costMult;
    /** multiplier on building output and per tier upgrade effects */
    public final double power;
    /** chance a door has an entity behind it */
    public final double spawnChance;
    /** doors open this many times faster */
    public final double doorSpeed;

    Difficulty(String label, String desc, double costMult, double power, double spawnChance, double doorSpeed) {
        this.label = label;
        this.desc = desc;
        this.costMult = costMult;
        this.power = power;
        this.spawnChance = spawnChance;
        this.doorSpeed = doorSpeed;
    }

    /** Saved as ordinal, -1 means the player hasn't picked yet (falls back to hard rules). */
    public static Difficulty of(int saved) {
        Difficulty[] all = values();
        return saved >= 0 && saved < all.length ? all[saved] : HARD;
    }

    /** Who comes through the door, from a 0..100 roll. null means nobody. */
    public EnemyType pickEntity(int r) {
        switch (this) {
            case EASY:
                return null;
            case NORMAL:
                return EnemyType.FIGURE;
            case EXTREME:
                // a-90b gets 26..69 instead of 26..49
                if (r == 25) return EnemyType.RUSH;
                if (r > 25 && r < 70) return EnemyType.A90B;
                if (r < 25) return EnemyType.FIGURE;
                return EnemyType.A90;
            default:
                if (r == 25) return EnemyType.RUSH;
                if (r > 25 && r < 50) return EnemyType.A90B;
                if (r < 25) return EnemyType.FIGURE;
                return EnemyType.A90; // 50..100
        }
    }

    /** Extreme brings back A-90B's own sprites. */
    public boolean a90bOriginalSprites() {
        return this == EXTREME;
    }
}
