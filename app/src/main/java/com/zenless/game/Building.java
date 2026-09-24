package com.zenless.game;

/** Static definition of a shop building / autoclicker. */
public final class Building {
    public final String name;
    public final String desc;
    public final double baseCost;
    public final double baseHps;

    public static final double COST_GROWTH = 1.15;

    public Building(String name, String desc, double baseCost, double baseHps) {
        this.name = name;
        this.desc = desc;
        this.baseCost = baseCost;
        this.baseHps = baseHps;
    }

    public static final Building[] ALL = {
            new Building("Holo Cursor", "taps the circle for you", 15, 0.1),
            new Building("Tap Drone", "a tiny hovering finger", 100, 1),
            new Building("Holo Farm", "grows holos in neat rows", 1_100, 8),
            new Building("Prism Mine", "digs light out of rock", 12_000, 47),
            new Building("Light Forge", "hammers photons into holos", 130_000, 260),
            new Building("Neon Factory", "mass produced glow", 1_400_000, 1_400),
            new Building("Zenith Portal", "holos from the other side", 20_000_000, 7_800),
            new Building("Hotel Annex", "rooms nobody checks out of", 330_000_000, 44_000),
            new Building("Superterrestrial Array", "listens to the sky", 5_100_000_000.0, 260_000),
    };
}
