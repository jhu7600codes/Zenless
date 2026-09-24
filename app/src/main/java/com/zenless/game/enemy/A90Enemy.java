package com.zenless.game.enemy;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.zenless.game.Sfx;
import com.zenless.game.Textures;

import java.util.Random;

/**
 * Pops up for a split second, then turns into a stop sign. While the sign is up the player
 * must not touch the screen, except to tap the sign's hand (which dismisses it early).
 */
public class A90Enemy extends Enemy {
    private static final long APPEAR_MS = 1000;
    private static final long WINDOW_MS = 2800;

    // hand position inside a-90stop-sign.webp, as fractions of the texture
    private static final float HAND_CX = 0.54f, HAND_CY = 0.50f, HAND_RX = 0.22f, HAND_RY = 0.31f;

    private ImageView sprite;
    private View flash;
    private boolean sign;
    private long appearMs, windowMs;
    private final Random rng = new Random();

    @Override
    public EnemyType type() {
        return EnemyType.A90;
    }

    @Override
    protected double penaltyFraction() {
        return 1.0 / 8;
    }

    @Override
    protected double rewardSeconds() {
        return 20;
    }

    @Override
    public void spawn(EnemyHost host) {
        super.spawn(host);
        appearMs = APPEAR_MS;
        windowMs = scaled(WINDOW_MS);

        flash = addDim(0x55FF0000);
        int size = (int) (Math.min(layerW(), layerH()) * 0.55f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.LEFT);
        // somewhere random but fully on screen
        lp.leftMargin = rng.nextInt(Math.max(1, layerW() - size));
        lp.topMargin = rng.nextInt(Math.max(1, layerH() - size));
        sprite = add(new ImageView(ctx()), lp);
        sprite.setScaleType(ImageView.ScaleType.FIT_CENTER);
        sprite.setImageBitmap(Textures.get(ctx(), Textures.A90_NORMAL));
        host.sfx().play(Sfx.A90_STING);
    }

    @Override
    public void tick(long dt) {
        if (!sign) {
            // glitchy jitter while it shows itself
            sprite.setTranslationX(rng.nextInt(dp(10) + 1) - dp(5));
            sprite.setTranslationY(rng.nextInt(dp(10) + 1) - dp(5));
            if (elapsed >= appearMs) {
                sign = true;
                sprite.setTranslationX(0);
                sprite.setTranslationY(0);
                sprite.setImageBitmap(Textures.get(ctx(), Textures.A90_STOP_SIGN));
                flash.setBackgroundColor(0x88000000);
            }
        } else if (elapsed >= appearMs + windowMs) {
            // stayed still the whole time
            succeed();
        }
    }

    @Override
    public boolean resolvePlayerAction(MotionEvent e) {
        int a = e.getActionMasked();
        if (a != MotionEvent.ACTION_DOWN && a != MotionEvent.ACTION_POINTER_DOWN) return true;
        // taps during the split second appearance are too fast to punish
        if (!sign) return true;
        int i = e.getActionIndex();
        if (onHand(e.getRawX() - e.getX() + e.getX(i), e.getRawY() - e.getY() + e.getY(i))) succeed();
        else fail();
        return true;
    }

    private boolean onHand(float rawX, float rawY) {
        int[] loc = new int[2];
        sprite.getLocationOnScreen(loc);
        float w = sprite.getWidth(), h = sprite.getHeight();
        float nx = (rawX - loc[0]) / w - HAND_CX;
        float ny = (rawY - loc[1]) / h - HAND_CY;
        return (nx * nx) / (HAND_RX * HAND_RX) + (ny * ny) / (HAND_RY * HAND_RY) <= 1f;
    }

    @Override
    public void onSuccess() {
        host.sfx().play(Sfx.SAFE);
        super.onSuccess();
    }
}
