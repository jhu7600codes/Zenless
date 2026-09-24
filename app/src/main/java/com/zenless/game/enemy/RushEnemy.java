package com.zenless.game.enemy;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.zenless.game.Sfx;
import com.zenless.game.Textures;

import java.util.Random;

/**
 * Audio cue first, lights flicker, then it tears across the screen.
 * The player has to be holding (hiding) when it arrives and keep holding until it's gone.
 * Failing costs 1/14 of current holos.
 */
public class RushEnemy extends Enemy {
    public static final long CUE_MS = 5000;
    private static final long PASS_MS = 1400;

    private View dark;
    private TextView hint;
    private ImageView sprite;
    private boolean passing;
    private int spriteSize;
    private final Random rng = new Random();

    @Override
    public EnemyType type() {
        return EnemyType.RUSH;
    }

    @Override
    protected double penaltyFraction() {
        return 1.0 / 14;
    }

    @Override
    protected double rewardSeconds() {
        return 30;
    }

    @Override
    public void spawn(EnemyHost host) {
        super.spawn(host);
        dark = addDim(0x00000000);
        hint = addHint("the lights flicker...", Gravity.CENTER, 24);
        hint.setAlpha(0f);
        host.sfx().play(Sfx.RUSH_CUE);
    }

    @Override
    public void tick(long dt) {
        if (!passing) {
            float t = elapsed / (float) CUE_MS;
            // flicker gets more violent as it gets close
            boolean off = rng.nextFloat() < 0.05f + 0.35f * t;
            int alpha = off ? (int) (120 + 120 * t) : (int) (60 * t);
            dark.setBackgroundColor(alpha << 24);
            hint.setAlpha(Math.min(1f, t * 3));
            if (t > 0.5f) hint.setText("hold to hide");
            if (elapsed >= CUE_MS) arrive();
        } else {
            if (!holding()) {
                fail();
                return;
            }
            float p = (elapsed - CUE_MS) / (float) PASS_MS;
            int w = spriteSize;
            sprite.setTranslationX(-w + (layerW() + w) * p);
            sprite.setTranslationY(rng.nextInt(dp(16) + 1) - dp(8));
            if (p >= 1f) succeed();
        }
    }

    private void arrive() {
        if (!holding()) {
            fail();
            return;
        }
        passing = true;
        hint.setText("");
        dark.setBackgroundColor(0xDD000000);
        int size = (int) (layerH() * 0.55f);
        spriteSize = size;
        sprite = add(new ImageView(ctx()), new FrameLayout.LayoutParams(size, size, Gravity.CENTER_VERTICAL | Gravity.LEFT));
        sprite.setScaleType(ImageView.ScaleType.FIT_CENTER);
        sprite.setImageBitmap(Textures.get(ctx(), Textures.RUSH_NORMAL));
        sprite.setTranslationX(-size);
    }

    @Override
    public boolean resolvePlayerAction(MotionEvent e) {
        // still free to tap the circle while it's only a sound
        int a = e.getActionMasked();
        if (!passing && (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN)) {
            host.tapCircle();
        }
        return true;
    }

    @Override
    public void onFail() {
        host.sfx().stop(Sfx.RUSH_CUE);
        super.onFail();
    }

    @Override
    public void onSuccess() {
        host.sfx().play(Sfx.SAFE);
        super.onSuccess();
    }

    @Override
    public void abort() {
        host.sfx().stop(Sfx.RUSH_CUE);
        super.abort();
    }
}
