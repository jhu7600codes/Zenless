package com.zenless.game.enemy;

import android.graphics.drawable.AnimationDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.zenless.game.Sfx;
import com.zenless.game.Textures;

import java.util.Locale;
import java.util.Random;

/**
 * Stays for 50s cycling approach / retreat.
 * Approach (running): halt, hold the screen. Retreat (walking): proceed, let go (taps allowed).
 * One mismatch after the grace period and it gets you. Survive the full 50s and it leaves.
 */
public class FigureEnemy extends Enemy {
    private static final long TOTAL_MS = 50_000;
    private static final long INTRO_MS = 2500;
    private static final long LEAVE_MS = 1500;
    private static final long GRACE_MS = 1100;
    private static final long PROCEED_MAX_PRESS_MS = 600;
    private static final long STEP_MS = 900;

    private static final int IDLE = 0, APPROACH = 1, RETREAT = 2, LEAVING = 3;

    private ImageView sprite;
    private TextView hint, timer;
    private final Random rng = new Random();

    private int phase = IDLE;
    private long phaseStart, phaseLen, grace, lastStep;
    private float scale = 0.55f;

    @Override
    public EnemyType type() {
        return EnemyType.FIGURE;
    }

    @Override
    protected double penaltyFraction() {
        return 1.0 / 5;
    }

    @Override
    protected double rewardSeconds() {
        return 120;
    }

    @Override
    public void spawn(EnemyHost host) {
        super.spawn(host);
        grace = scaled(GRACE_MS);
        addDim(0xAA000000);
        int h = (int) (layerH() * 0.6f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) (h * 0.8f), h, Gravity.CENTER);
        sprite = add(new ImageView(ctx()), lp);
        sprite.setScaleType(ImageView.ScaleType.FIT_CENTER);
        sprite.setPivotX(h * 0.4f);
        sprite.setPivotY(h);
        applyScale();
        timer = addHint("", Gravity.TOP | Gravity.CENTER_HORIZONTAL, 56);
        hint = addHint("it's listening...", Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 48);
        setAnim(Textures.FIGURE_IDLE, 250);
        host.sfx().play(Sfx.FIGURE_SPAWN);
    }

    private void setAnim(String folder, int frameMs) {
        AnimationDrawable a = Textures.animation(ctx(), folder, frameMs);
        sprite.setImageDrawable(a);
        a.start();
    }

    private void applyScale() {
        sprite.setScaleX(scale);
        sprite.setScaleY(scale);
    }

    private void startPhase(int p) {
        phase = p;
        phaseStart = elapsed;
        phaseLen = 4000 + rng.nextInt(3500);
        if (p == APPROACH) {
            setAnim(Textures.FIGURE_RUNNING, 63);
            hint.setText("it's coming — HALT, hold still");
        } else if (p == RETREAT) {
            setAnim(Textures.FIGURE_WALKING, 69);
            hint.setText("it's wandering off — PROCEED");
        } else if (p == LEAVING) {
            setAnim(Textures.FIGURE_WALKING, 69);
            hint.setText("it lost interest");
            timer.setText("");
            sprite.animate().alpha(0f).setDuration(LEAVE_MS).start();
        }
    }

    @Override
    public void tick(long dt) {
        long inPhase = elapsed - phaseStart;
        if (phase == IDLE) {
            if (elapsed >= INTRO_MS) startPhase(APPROACH);
            return;
        }
        if (phase == LEAVING) {
            scale = Math.max(0.2f, scale - dt / 3000f);
            applyScale();
            if (inPhase >= LEAVE_MS) succeed();
            return;
        }

        long left = Math.max(0, TOTAL_MS - elapsed);
        timer.setText(String.format(Locale.US, "survive %ds", (left + 999) / 1000));

        if (phase == APPROACH) {
            scale = Math.min(1.35f, scale + dt / 6000f);
            if (elapsed - lastStep >= STEP_MS) {
                lastStep = elapsed;
                host.sfx().play(Sfx.FIGURE_STEP);
            }
        } else {
            scale = Math.max(0.45f, scale - dt / 7000f);
        }
        applyScale();

        if (inPhase >= grace) {
            if (phase == APPROACH && !holding()) {
                fail();
                return;
            }
            if (phase == RETREAT && heldMs() > PROCEED_MAX_PRESS_MS) {
                fail();
                return;
            }
        }

        if (elapsed >= TOTAL_MS) {
            startPhase(LEAVING);
        } else if (inPhase >= phaseLen) {
            startPhase(phase == APPROACH ? RETREAT : APPROACH);
        }
    }

    @Override
    public boolean resolvePlayerAction(MotionEvent e) {
        int a = e.getActionMasked();
        if (phase == RETREAT && (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN)) {
            host.tapCircle();
        }
        return true;
    }

    @Override
    public void onFail() {
        host.sfx().stop(Sfx.FIGURE_STEP);
        host.sfx().stop(Sfx.FIGURE_SPAWN);
        super.onFail();
    }

    @Override
    public void onSuccess() {
        host.sfx().play(Sfx.SAFE);
        super.onSuccess();
    }
}
