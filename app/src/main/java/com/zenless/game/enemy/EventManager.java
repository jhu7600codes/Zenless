package com.zenless.game.enemy;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.zenless.game.Economy;
import com.zenless.game.GameState;
import com.zenless.game.Sfx;

import java.util.Random;

/**
 * Runs the doors. Every DOOR_MS a door opens: 51% nothing, 49% an entity rolls in
 * (only one at a time, with a cooldown after each encounter). Admin triggers bypass the roll.
 */
public class EventManager implements EnemyHost {

    public interface Listener {
        void onEnemySpawned(Enemy e);

        void onEnemyFinished(Enemy e, boolean survived, double holosDelta);

        void onDoor(int door, EnemyType spawned);

        void tapCircle();

        boolean isHolding();
    }

    public static final long DOOR_MS = 30_000;
    public static final long COOLDOWN_MS = 20_000;

    private final Context ctx;
    private final FrameLayout layer;
    private final Sfx sfx;
    private final JumpscareController jumpscares;
    private final GameState state;
    private final Economy eco;
    private final Listener listener;
    private final Random rng = new Random();

    private Enemy current;
    private long doorTimer = DOOR_MS;
    private long cooldown;
    private double lastDelta;

    public EventManager(Context ctx, FrameLayout layer, Sfx sfx, GameState state, Economy eco, Listener listener) {
        this.ctx = ctx;
        this.layer = layer;
        this.sfx = sfx;
        this.state = state;
        this.eco = eco;
        this.listener = listener;
        this.jumpscares = new JumpscareController(ctx, layer, sfx);
    }

    /** The spawn roll from the design doc. Returns null for "no spawn". */
    public EnemyType rollDoor() {
        return pick(rng.nextInt(100), rng.nextInt(101));
    }

    /**
     * @param spawnRoll  0..99, below 51 means the door is empty (51/49)
     * @param entityRoll 0..100, picks who shows up
     */
    public static EnemyType pick(int spawnRoll, int entityRoll) {
        if (spawnRoll < 51) return null;
        int r = entityRoll;
        if (r == 25) return EnemyType.RUSH;
        if (r > 25 && r < 50) return EnemyType.A90B;
        if (r < 25) return EnemyType.FIGURE;
        return EnemyType.A90; // 50..100
    }

    public void tick(long dt) {
        if (current != null) {
            current.dispatchTick(dt);
            return; // doors stay shut while something is here
        }
        if (cooldown > 0) cooldown -= dt;
        doorTimer -= dt;
        if (doorTimer <= 0) {
            doorTimer = DOOR_MS;
            state.door++;
            EnemyType t = cooldown > 0 ? null : rollDoor();
            listener.onDoor(state.door, t);
            if (t != null) spawn(t);
        }
    }

    /** Admin panel trigger. Ignores the roll and the cooldown, but still one at a time. */
    public boolean trigger(EnemyType t) {
        if (current != null || layer.getWidth() == 0) return false;
        spawn(t);
        return true;
    }

    private void spawn(EnemyType t) {
        current = t.create();
        listener.onEnemySpawned(current);
        current.spawn(this);
    }

    public boolean isActive() {
        return current != null;
    }

    public Enemy current() {
        return current;
    }

    public float doorProgress() {
        return 1f - doorTimer / (float) DOOR_MS;
    }

    public void onTouch(MotionEvent e) {
        if (current != null) current.dispatchTouch(e);
    }

    /** Pausing mid encounter just makes it go away, no penalty and no reward. */
    public void pause() {
        jumpscares.cancel();
        if (current != null) {
            current.abort();
            current = null;
            cooldown = COOLDOWN_MS;
        }
        doorTimer = DOOR_MS;
    }

    // ---- EnemyHost ----

    @Override
    public Context context() {
        return ctx;
    }

    @Override
    public FrameLayout layer() {
        return layer;
    }

    @Override
    public Sfx sfx() {
        return sfx;
    }

    @Override
    public JumpscareController jumpscares() {
        return jumpscares;
    }

    @Override
    public void tapCircle() {
        listener.tapCircle();
    }

    @Override
    public boolean isHolding() {
        return listener.isHolding();
    }

    @Override
    public double reactionScale() {
        return eco.reactionScale();
    }

    @Override
    public double penalize(double fraction) {
        double lost = Math.floor(state.holos * fraction * eco.penaltyScale());
        state.holos -= lost;
        lastDelta = -lost;
        return lost;
    }

    @Override
    public double reward(double seconds) {
        double gain = Math.max(eco.hps() * seconds, eco.clickPower() * seconds * 0.5);
        state.earn(gain);
        lastDelta = gain;
        return gain;
    }

    @Override
    public void finished(Enemy e, boolean survived) {
        if (e != current) return;
        current = null;
        cooldown = COOLDOWN_MS;
        doorTimer = DOOR_MS;
        if (survived) state.entitiesSurvived++;
        else state.entitiesFailed++;
        double d = lastDelta;
        lastDelta = 0;
        listener.onEnemyFinished(e, survived, d);
    }
}
