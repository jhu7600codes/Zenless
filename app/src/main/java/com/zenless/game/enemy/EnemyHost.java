package com.zenless.game.enemy;

import android.content.Context;
import android.widget.FrameLayout;

import com.zenless.game.Sfx;

/** What an enemy is allowed to touch in the game. Implemented by EventManager. */
public interface EnemyHost {
    Context context();

    /** Full screen layer above the game for enemy views. */
    FrameLayout layer();

    Sfx sfx();

    JumpscareController jumpscares();

    /** Forward a "proceed" tap to the circle so it still earns holos. */
    void tapCircle();

    /** Current pointer state as seen by the activity (true while any finger is down). */
    boolean isHolding();

    /** 1.0 normally, bigger with the Composure meta upgrade. */
    double reactionScale();

    /** Takes {@code fraction} of current holos (after Night Light), returns amount lost. */
    double penalize(double fraction);

    /** Gives {@code seconds} worth of income, returns amount gained. */
    double reward(double seconds);

    /** Enemy is gone (after its jumpscare if it failed). */
    void finished(Enemy e, boolean survived);
}
