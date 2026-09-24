package com.zenless.game;

/** Tiered upgrade definition. Effects are computed in {@link Economy}. */
public final class Upgrade {
    public static final int CLICK_POWER = 0;
    public static final int PASSIVE_BOOST = 1;
    public static final int COST_REDUCTION = 2;
    public static final int CRIT_TAPS = 3;
    public static final int SYNERGY = 4;
    public static final int GUARD = 5;

    public final int id;
    public final String name;
    public final String effectPerTier;
    public final double baseCost;
    public final double costMult;
    public final int maxTier;

    public Upgrade(int id, String name, String effectPerTier, double baseCost, double costMult, int maxTier) {
        this.id = id;
        this.name = name;
        this.effectPerTier = effectPerTier;
        this.baseCost = baseCost;
        this.costMult = costMult;
        this.maxTier = maxTier;
    }

    public static final Upgrade[] ALL = {
            new Upgrade(CLICK_POWER, "Click Power", "x2 holos per tap", 50, 6, 15),
            new Upgrade(PASSIVE_BOOST, "Passive Boost", "x1.5 holos/sec", 500, 7, 15),
            new Upgrade(COST_REDUCTION, "Cost Reduction", "-4% shop prices", 1_000, 9, 10),
            new Upgrade(CRIT_TAPS, "Critical Taps", "+4% chance of a x10 tap", 2_500, 8, 10),
            new Upgrade(SYNERGY, "Holo Synergy", "taps gain +1% of holos/sec", 10_000, 10, 10),
            new Upgrade(GUARD, "Night Light", "-6% holos lost to entities", 25_000, 12, 8),
    };
}
