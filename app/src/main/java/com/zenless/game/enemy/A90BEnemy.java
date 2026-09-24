package com.zenless.game.enemy;

import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.zenless.game.Sfx;
import com.zenless.game.Textures;

import java.util.Random;

/**
 * Alternates HALT / PROCEED for ~15s.
 * HALT: tap and hold. PROCEED: let go and tap (taps still earn holos).
 * Each phase starts with a short grace period to react.
 */
public class A90BEnemy extends Enemy {
    private static final long TOTAL_MS = 15_000;
    private static final long INTRO_MS = 800;
    private static final long GRACE_MS = 900;
    private static final long PROCEED_MAX_PRESS_MS = 600;

    private ImageView sign;
    private TextView hint;
    private final Random rng = new Random();

    private boolean intro = true;
    private boolean halt;
    private long phaseStart, phaseLen, grace;
    private int proceedTaps;

    @Override
    public EnemyType type() {
        return EnemyType.A90B;
    }

    @Override
    protected double penaltyFraction() {
        return 1.0 / 6;
    }

    @Override
    protected double rewardSeconds() {
        return 60;
    }

    @Override
    public void spawn(EnemyHost host) {
        super.spawn(host);
        grace = scaled(GRACE_MS);
        addDim(0x66000000);
        int size = (int) (Math.min(layerW(), layerH()) * 0.6f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        lp.topMargin = dp(70);
        sign = add(new ImageView(ctx()), lp);
        sign.setScaleType(ImageView.ScaleType.FIT_CENTER);
        sign.setImageBitmap(Textures.get(ctx(), host.a90bOriginalSprites() ? Textures.A90B_NORMAL : Textures.A90_NORMAL));
        hint = addHint("", Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 48);
        host.sfx().play(Sfx.A90_STING);
    }

    private void startPhase(boolean newHalt) {
        halt = newHalt;
        phaseStart = elapsed;
        phaseLen = 2000 + rng.nextInt(1800);
        proceedTaps = 0;
        sign.setImageBitmap(Textures.get(ctx(), halt ? Textures.A90B_HALT_SIGN : Textures.A90B_PROCEED_SIGN));
        sign.setScaleX(1.25f);
        sign.setScaleY(1.25f);
        sign.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
        hint.setText(halt ? "HALT — hold" : "PROCEED — let go and tap");
        host.sfx().play(halt ? Sfx.BEEP_HALT : Sfx.BEEP_PROCEED);
    }

    @Override
    public void tick(long dt) {
        if (intro) {
            if (elapsed >= INTRO_MS) {
                intro = false;
                startPhase(true);
            }
            return;
        }
        long inPhase = elapsed - phaseStart;
        if (inPhase >= grace) {
            if (halt && !holding()) {
                fail();
                return;
            }
            if (!halt && heldMs() > PROCEED_MAX_PRESS_MS) {
                fail();
                return;
            }
        }
        if (inPhase >= phaseLen) {
            // proceed means act, sitting still through it is a mismatch too
            if (!halt && proceedTaps == 0) {
                fail();
                return;
            }
            if (elapsed >= TOTAL_MS) {
                succeed();
                return;
            }
            startPhase(!halt);
        }
    }

    @Override
    public boolean resolvePlayerAction(MotionEvent e) {
        int a = e.getActionMasked();
        if (!intro && !halt && (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN)) {
            proceedTaps++;
            host.tapCircle();
        }
        return true;
    }

    @Override
    public void onSuccess() {
        host.sfx().play(Sfx.SAFE);
        super.onSuccess();
    }
}
