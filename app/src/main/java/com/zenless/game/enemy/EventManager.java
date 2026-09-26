package com.zenless.game.enemy;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.zenless.game.Economy;
import com.zenless.game.GameState;
import com.zenless.game.Sfx;

import java.util.Random;

/**
 * Runs the doors. A door opens every DOOR_MS (sooner while tapping), and {@link SpawnTable}
 * decides who's behind it. One entity at a time. Admin triggers bypass all of it.
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
    /** each tap on the circle brings the next door this much closer */
    public static final long TAP_BOOST_MS = 250;

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
    private int quietDoors;
    private boolean doorsEnabled = true;
    private double lastDelta;

    public EventManager(Context ctx, FrameLayout layer, Sfx sfx, GameState state, Economy eco, Listener listener) {
        this.ctx = ctx;
        this.layer = layer;
        this.sfx = sfx;
        this.state = state;
        this.eco = eco;
        this.listener = listener;
        this.jumpscares = new JumpscareController(ctx, layer, sfx, state);
    }

    private EnemyType rollDoor() {
        if (state.cheatNoEntities) return null;
        EnemyType t = SpawnTable.roll(state.door, state.diff(), quietDoors > 0, rng);
        if (quietDoors > 0) quietDoors--;
        return t;
    }

    /** Tapping walks you through the rooms faster. */
    public void onPlayerTap() {
        if (current == null && state.difficulty >= 0) doorTimer -= TAP_BOOST_MS;
    }

    private long doorMs() {
        return (long) (DOOR_MS / state.diff().doorSpeed);
    }

    public void tick(long dt) {
        if (current != null) {
            current.dispatchTick(dt);
            return; // doors stay shut while something is here
        }
        if (state.difficulty < 0 || !doorsEnabled) return; // still picking a difficulty, or tutorial practice
        doorTimer -= dt;
        if (doorTimer <= 0) {
            doorTimer = doorMs();
            state.door++;
            EnemyType t = rollDoor();
            listener.onDoor(state.door, t);
            if (t != null) {
                boolean meet = t != EnemyType.SECRET && SpawnTable.scripted(t, state.door, state.diff());
                spawn(t, meet && rng.nextDouble() < SpawnTable.RARE_CHANCE);
            }
        }
    }

    /** Admin panel trigger. Ignores the doors, but still one at a time. */
    public boolean trigger(EnemyType t, boolean buffed) {
        if (current != null || layer.getWidth() == 0) return false;
        spawn(t, buffed);
        return true;
    }

    private void spawn(EnemyType t, boolean buffed) {
        current = t.create();
        listener.onEnemySpawned(current);
        current.start(this, buffed);
    }

    /** Admin: the next door opens on the next frame. */
    public void openDoorNow() {
        if (current == null) doorTimer = 0;
    }

    /** Admin: walk past doors without rolling anything. */
    public void skipDoors(int n) {
        state.door += n;
        doorTimer = doorMs();
    }

    public void setDoorsEnabled(boolean on) {
        doorsEnabled = on;
    }

    /** New run: fresh door timer, no leftover quiet doors. */
    public void resetDoors() {
        doorTimer = doorMs();
        quietDoors = 0;
    }

    public boolean a90bOriginalSprites() {
        return state.diff().a90bOriginalSprites();
    }

    public boolean isActive() {
        return current != null;
    }

    public Enemy current() {
        return current;
    }

    public float doorProgress() {
        return 1f - doorTimer / (float) doorMs();
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
            quietDoors = SpawnTable.QUIET_DOORS;
        }
        doorTimer = doorMs();
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
        if (state.cheatInvincible) {
            lastDelta = 0;
            return 0;
        }
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
        quietDoors = SpawnTable.QUIET_DOORS;
        doorTimer = doorMs();
        if (survived) state.entitiesSurvived++;
        else state.entitiesFailed++;
        double d = lastDelta;
        lastDelta = 0;
        listener.onEnemyFinished(e, survived, d);
    }
}
