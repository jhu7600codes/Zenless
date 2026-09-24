package com.zenless.game.enemy;

/** Registry of entities. To add a new one: write an Enemy subclass and add an entry here. */
public enum EnemyType {
    A90("A-90") {
        @Override public Enemy create() { return new A90Enemy(); }
    },
    A90B("A-90B") {
        @Override public Enemy create() { return new A90BEnemy(); }
    },
    RUSH("Rush") {
        @Override public Enemy create() { return new RushEnemy(); }
    },
    FIGURE("Figure") {
        @Override public Enemy create() { return new FigureEnemy(); }
    };

    public final String label;

    EnemyType(String label) {
        this.label = label;
    }

    public abstract Enemy create();
}
