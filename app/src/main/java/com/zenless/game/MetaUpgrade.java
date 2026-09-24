package com.zenless.game;

/** Permanent unlocks bought with superterrestrial items. Survive rebirths. */
public final class MetaUpgrade {
    public static final int HEAD_START = 0;
    public static final int STARTER_DRONES = 1;
    public static final int ETERNAL_GLOW = 2;
    public static final int COMPOSURE = 3;
    public static final int OFFLINE = 4;

    public final int id;
    public final String name;
    public final String effect;
    public final long baseCost;
    public final int maxTier;

    public MetaUpgrade(int id, String name, String effect, long baseCost, int maxTier) {
        this.id = id;
        this.name = name;
        this.effect = effect;
        this.baseCost = baseCost;
        this.maxTier = maxTier;
    }

    public long costFor(int tier) {
        return baseCost * (tier + 1);
    }

    public static final MetaUpgrade[] ALL = {
            new MetaUpgrade(HEAD_START, "Head Start", "start each run with 1K x tier holos", 1, 10),
            new MetaUpgrade(STARTER_DRONES, "Starter Drones", "start with 5 tap drones per tier", 2, 5),
            new MetaUpgrade(ETERNAL_GLOW, "Eternal Glow", "+25% all income per tier", 3, 20),
            new MetaUpgrade(COMPOSURE, "Composure", "entity reaction windows +10% per tier", 4, 5),
            new MetaUpgrade(OFFLINE, "Dreaming", "+10% offline earnings per tier", 2, 5),
    };
}
