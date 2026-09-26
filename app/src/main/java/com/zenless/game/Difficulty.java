package com.zenless.game;

import com.zenless.game.enemy.EnemyType;

/** Picked at the start of every run. Changes shop prices, upgrade strength and entity doors. */
public enum Difficulty {
    EASY("Easy", "no entities, cheaper shop", 0.8, 1.0, 0.0, 1.0),
    NORMAL("Normal", "only Figure, every 30 doors, cheaper shop", 0.8, 1.0, 1.0, 1.0),
    HARD("Hard", "every entity, normal spawns and prices", 1.0, 1.0, 1.0, 1.0),
    EXTREME("Extreme", "double spawns, more A-90B in his original look", 1.0, 1.0, 2.0, 1.0),
    SUPER_HARD("SUPER HARD MODE", "+50% prices, -50% power, +547% spawns, 3x faster doors", 1.5, 0.5, 6.47, 3.0);

    public final String label;
    public final String desc;
    /** multiplier on shop and upgrade prices */
    public final double costMult;
    /** multiplier on building output and per tier upgrade effects */
    public final double power;
    /** multiplier on every entity's per door chance */
    public final double spawnMult;
    /** doors open this many times faster */
    public final double doorSpeed;

    Difficulty(String label, String desc, double costMult, double power, double spawnMult, double doorSpeed) {
        this.label = label;
        this.desc = desc;
        this.costMult = costMult;
        this.power = power;
        this.spawnMult = spawnMult;
        this.doorSpeed = doorSpeed;
    }

    /** Saved as ordinal, -1 means the player hasn't picked yet (falls back to hard rules). */
    public static Difficulty of(int saved) {
        Difficulty[] all = values();
        return saved >= 0 && saved < all.length ? all[saved] : HARD;
    }

    public boolean allows(EnemyType t) {
        switch (this) {
            case EASY:
                return false;
            case NORMAL:
                return t == EnemyType.FIGURE;
            default:
                return true;
        }
    }

    public double chanceMult(EnemyType t) {
        // extreme: a-90b shows up twice as often again on top of the doubled spawns
        if (this == EXTREME && t == EnemyType.A90B) return spawnMult * 2;
        return spawnMult;
    }

    /** Extreme brings back A-90B's own sprites. */
    public boolean a90bOriginalSprites() {
        return this == EXTREME;
    }
}
