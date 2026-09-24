package com.zenless.game;

import java.util.Random;

/** All the math: costs, income, multipliers, rebirth rewards. */
public final class Economy {
    public static final double REBIRTH_MIN = 1_000_000;

    private final GameState s;
    private final Random rng = new Random();

    public Economy(GameState s) {
        this.s = s;
    }

    /** Permanent income multiplier from superterrestrial items and meta upgrades. */
    public double globalMult() {
        return (1 + 0.10 * s.stLifetime) * (1 + 0.25 * s.meta[MetaUpgrade.ETERNAL_GLOW]);
    }

    /** Per tier upgrade effects get scaled by this (super hard halves them). */
    private double power() {
        return s.diff().power;
    }

    public double costMult() {
        return Math.pow(1 - 0.04 * power(), s.upgrades[Upgrade.COST_REDUCTION]) * s.diff().costMult;
    }

    public double buildingCost(int i) {
        Building b = Building.ALL[i];
        return Math.ceil(b.baseCost * Math.pow(Building.COST_GROWTH, s.buildings[i]) * costMult());
    }

    public double buildingHps(int i) {
        return Building.ALL[i].baseHps * power() * passiveMult();
    }

    public double passiveMult() {
        return Math.pow(1 + 0.5 * power(), s.upgrades[Upgrade.PASSIVE_BOOST]) * globalMult();
    }

    public double hps() {
        double base = 0;
        for (int i = 0; i < Building.ALL.length; i++) base += Building.ALL[i].baseHps * s.buildings[i];
        return base * power() * passiveMult();
    }

    public double clickPower() {
        double p = Math.pow(1 + power(), s.upgrades[Upgrade.CLICK_POWER]) * globalMult();
        p += hps() * 0.01 * power() * s.upgrades[Upgrade.SYNERGY];
        return p;
    }

    public double critChance() {
        return 0.04 * power() * s.upgrades[Upgrade.CRIT_TAPS];
    }

    public boolean rollCrit() {
        return rng.nextDouble() < critChance();
    }

    public double upgradeCost(int i) {
        Upgrade u = Upgrade.ALL[i];
        return Math.ceil(u.baseCost * Math.pow(u.costMult, s.upgrades[i]) * s.diff().costMult);
    }

    public boolean upgradeMaxed(int i) {
        return s.upgrades[i] >= Upgrade.ALL[i].maxTier;
    }

    /** Fraction of the penalty that actually applies after Night Light. */
    public double penaltyScale() {
        return Math.max(0.2, 1 - 0.06 * power() * s.upgrades[Upgrade.GUARD]);
    }

    /** Multiplier on entity reaction windows from Composure. */
    public double reactionScale() {
        return 1 + 0.10 * s.meta[MetaUpgrade.COMPOSURE];
    }

    public long rebirthReward() {
        if (s.runEarned < REBIRTH_MIN) return 0;
        return (long) Math.floor(Math.sqrt(s.runEarned / REBIRTH_MIN));
    }

    public double offlineRate() {
        return 0.25 + 0.10 * s.meta[MetaUpgrade.OFFLINE];
    }

    public boolean buyBuilding(int i) {
        double c = buildingCost(i);
        if (s.holos < c) return false;
        s.holos -= c;
        s.buildings[i]++;
        return true;
    }

    public boolean buyUpgrade(int i) {
        if (upgradeMaxed(i)) return false;
        double c = upgradeCost(i);
        if (s.holos < c) return false;
        s.holos -= c;
        s.upgrades[i]++;
        return true;
    }

    public boolean buyMeta(int i) {
        MetaUpgrade m = MetaUpgrade.ALL[i];
        if (s.meta[i] >= m.maxTier) return false;
        long c = m.costFor(s.meta[i]);
        if (s.stItems < c) return false;
        s.stItems -= c;
        s.meta[i]++;
        return true;
    }

    public long rebirth() {
        long reward = rebirthReward();
        if (reward <= 0) return 0;
        s.stItems += reward;
        s.stLifetime += reward;
        s.rebirths++;
        s.adminUnlocked = true;
        s.resetRun();
        return reward;
    }
}
