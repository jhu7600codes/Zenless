package com.zenless.game.enemy;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for every entity. Lifecycle:
 * spawn() -> tick() every frame + resolvePlayerAction() per touch -> onSuccess() or onFail().
 * Subclasses call {@link #succeed()} / {@link #fail()} which guarantee exactly one resolution.
 */
public abstract class Enemy {
    protected EnemyHost host;
    protected long elapsed;
    private boolean resolved;
    /** the 0.001% "super" version: half the reaction time, red tint, double stakes */
    protected boolean buffed;
    private final List<View> views = new ArrayList<>();

    // press tracking so subclasses can tell a quick tap from a hold
    private long pressStart = -1;

    public abstract EnemyType type();

    /** Fraction of holos lost when this entity gets you. */
    protected abstract double penaltyFraction();

    /** Seconds of income granted for surviving. */
    protected abstract double rewardSeconds();

    /** Called by the event manager: spawn, then the buffed overlay on top of everything. */
    public final void start(EnemyHost host, boolean buffed) {
        this.buffed = buffed;
        spawn(host);
        if (buffed) {
            addDim(0x40FF0000);
            TextView t = addHint("SUPER " + type().label.toUpperCase(java.util.Locale.US), Gravity.TOP | Gravity.CENTER_HORIZONTAL, 16);
            t.setTextColor(0xFFFF4444);
        }
    }

    public final boolean isBuffed() {
        return buffed;
    }

    /** Name for toasts, e.g. "SUPER Rush". */
    public final String label() {
        return (buffed ? "SUPER " : "") + type().label;
    }

    /** Build views and start sounds. */
    public void spawn(EnemyHost host) {
        this.host = host;
        elapsed = 0;
        if (host.isHolding()) pressStart = SystemClock.uptimeMillis();
    }

    /** Called about 60 times a second while the entity is alive. */
    public abstract void tick(long dtMs);

    /** Every touch event while the entity is alive. Return true if handled. */
    public abstract boolean resolvePlayerAction(MotionEvent e);

    /** Player messed up. Default: remove views, run the type's jumpscare, then apply the penalty. */
    public void onFail() {
        clearViews();
        host.jumpscares().play(type(), new Runnable() {
            @Override
            public void run() {
                host.penalize(Math.min(0.5, penaltyFraction() * (buffed ? 2 : 1)));
                host.finished(Enemy.this, false);
            }
        });
    }

    /** Player survived. Default: remove views, reward, done. */
    public void onSuccess() {
        clearViews();
        host.reward(rewardSeconds() * (buffed ? 2 : 1));
        host.finished(this, true);
    }

    /** App paused mid encounter: vanish without penalty or reward. */
    public void abort() {
        resolved = true;
        clearViews();
    }

    public final boolean isResolved() {
        return resolved;
    }

    /** Entry point used by the event manager: updates hold tracking, then lets the enemy judge. */
    public final boolean dispatchTouch(MotionEvent e) {
        if (resolved) return true;
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) pressStart = SystemClock.uptimeMillis();
        else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) pressStart = -1;
        return resolvePlayerAction(e);
    }

    public final void dispatchTick(long dt) {
        if (resolved) return;
        elapsed += dt;
        tick(dt);
    }

    protected final void fail() {
        if (resolved) return;
        resolved = true;
        onFail();
    }

    protected final void succeed() {
        if (resolved) return;
        resolved = true;
        onSuccess();
    }

    /** How long the current press has lasted, 0 if not pressing. */
    protected final long heldMs() {
        return pressStart < 0 ? 0 : SystemClock.uptimeMillis() - pressStart;
    }

    protected final boolean holding() {
        return pressStart >= 0;
    }

    protected final long scaled(long ms) {
        return (long) (ms * host.reactionScale() * (buffed ? 0.5 : 1));
    }

    // ---- view helpers ----

    protected final Context ctx() {
        return host.context();
    }

    protected final <V extends View> V add(V v, FrameLayout.LayoutParams lp) {
        host.layer().addView(v, lp);
        views.add(v);
        return v;
    }

    protected final View addDim(int color) {
        View v = new View(ctx());
        v.setBackgroundColor(color);
        return add(v, new FrameLayout.LayoutParams(-1, -1));
    }

    protected final TextView addHint(String text, int gravity, int marginDp) {
        TextView t = new TextView(ctx());
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setGravity(Gravity.CENTER);
        t.setShadowLayer(8, 0, 0, Color.BLACK);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, gravity);
        int m = dp(marginDp);
        lp.setMargins(m, m, m, m);
        return add(t, lp);
    }

    protected final int dp(float v) {
        return (int) (v * ctx().getResources().getDisplayMetrics().density + 0.5f);
    }

    protected final int layerW() {
        return host.layer().getWidth();
    }

    protected final int layerH() {
        return host.layer().getHeight();
    }

    protected final void clearViews() {
        for (View v : views) {
            v.animate().cancel();
            host.layer().removeView(v);
        }
        views.clear();
    }
}
