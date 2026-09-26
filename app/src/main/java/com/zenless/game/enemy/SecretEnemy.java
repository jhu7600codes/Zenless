package com.zenless.game.enemy;

import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.zenless.game.Sfx;
import com.zenless.game.Textures;

import java.util.Random;

/**
 * "???" - 0.001% on door 1. The app icon bounces around like a DVD logo.
 * Catch it (tap it) within 8 seconds for a huge payout, otherwise it lunges at you.
 */
public class SecretEnemy extends Enemy {
    private static final long TOTAL_MS = 8000;

    private ImageView icon;
    private float x, y, vx, vy;
    private int size;
    private final Random rng = new Random();

    @Override
    public EnemyType type() {
        return EnemyType.SECRET;
    }

    @Override
    protected double penaltyFraction() {
        return 1.0 / 14;
    }

    @Override
    protected double rewardSeconds() {
        return 3600; // an hour of income, you earned it
    }

    @Override
    public void spawn(EnemyHost host) {
        super.spawn(host);
        addDim(0xCC000000);
        addHint("catch it", Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 48);
        size = (int) (Math.min(layerW(), layerH()) * 0.28f);
        icon = add(new ImageView(ctx()), new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.LEFT));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageBitmap(Textures.get(ctx(), Textures.ICON));
        x = rng.nextInt(Math.max(1, layerW() - size));
        y = rng.nextInt(Math.max(1, layerH() - size));
        float speed = Math.min(layerW(), layerH()) * 0.9f; // px per second
        vx = rng.nextBoolean() ? speed : -speed;
        vy = rng.nextBoolean() ? speed * 0.8f : -speed * 0.8f;
        host.sfx().play(Sfx.A90B_SPAWN);
    }

    @Override
    public void tick(long dt) {
        float s = dt / 1000f;
        x += vx * s;
        y += vy * s;
        // bounce off the edges, dvd logo style
        if (x < 0) { x = 0; vx = -vx; }
        if (y < 0) { y = 0; vy = -vy; }
        if (x > layerW() - size) { x = layerW() - size; vx = -vx; }
        if (y > layerH() - size) { y = layerH() - size; vy = -vy; }
        icon.setTranslationX(x);
        icon.setTranslationY(y);
        icon.setRotation(elapsed * 0.2f);
        if (elapsed >= TOTAL_MS) fail();
    }

    @Override
    public boolean resolvePlayerAction(MotionEvent e) {
        int a = e.getActionMasked();
        if (a != MotionEvent.ACTION_DOWN && a != MotionEvent.ACTION_POINTER_DOWN) return true;
        int i = e.getActionIndex();
        int[] loc = new int[2];
        host.layer().getLocationOnScreen(loc);
        float tx = e.getRawX() - e.getX() + e.getX(i) - loc[0];
        float ty = e.getRawY() - e.getY() + e.getY(i) - loc[1];
        // a little generous, it's moving fast
        float pad = size * 0.2f;
        if (tx >= x - pad && tx <= x + size + pad && ty >= y - pad && ty <= y + size + pad) succeed();
        return true;
    }

    @Override
    public void onSuccess() {
        host.sfx().play(Sfx.SAFE);
        super.onSuccess();
    }
}
