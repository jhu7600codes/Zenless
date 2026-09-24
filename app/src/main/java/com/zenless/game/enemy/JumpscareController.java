package com.zenless.game.enemy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.zenless.game.Sfx;
import com.zenless.game.Textures;

import java.util.Random;

/**
 * Full screen fail animations, always under a second.
 * A-90 / A-90B: distorted texture over its red static background, violent shake.
 * Rush / Figure: the sprite lunges from small to filling the screen.
 */
public class JumpscareController {
    private static final long GLITCH_MS = 750;
    private static final long LUNGE_MS = 850;

    private final Context ctx;
    private final FrameLayout layer;
    private final Sfx sfx;
    private final Random rng = new Random();
    private ValueAnimator running;
    private View[] runningViews;

    public JumpscareController(Context ctx, FrameLayout layer, Sfx sfx) {
        this.ctx = ctx;
        this.layer = layer;
        this.sfx = sfx;
    }

    public void play(EnemyType type, Runnable done) {
        switch (type) {
            case A90:
                glitch(Textures.A90_DISTORT_BG, Textures.A90_DISTORT, Sfx.SCREAM_A90, done);
                break;
            case A90B:
                glitch(Textures.A90_DISTORT_BG, Textures.A90_DISTORT, Sfx.SCREAM_A90, done);
                break;
            case RUSH:
                lunge(Textures.get(ctx, Textures.RUSH_JUMPSCARE), 0xFF000000, ImageView.ScaleType.CENTER_CROP, Sfx.SCREAM_RUSH, done);
                break;
            case FIGURE: {
                Bitmap[] f = Textures.frames(ctx, Textures.FIGURE_RUNNING);
                lunge(f.length > 0 ? f[0] : null, 0xFF200000, ImageView.ScaleType.FIT_CENTER, Sfx.SCREAM_FIGURE, done);
                break;
            }
            default:
                done.run();
        }
    }

    /** Stops a jumpscare in progress (app paused), skipping its callback. */
    public void cancel() {
        if (running != null) {
            running.removeAllListeners();
            running.cancel();
            running = null;
        }
        if (runningViews != null) {
            for (View v : runningViews) layer.removeView(v);
            runningViews = null;
        }
    }

    private void glitch(String bg, String fg, int scream, final Runnable done) {
        final ImageView back = new ImageView(ctx);
        back.setScaleType(ImageView.ScaleType.FIT_XY);
        back.setImageBitmap(Textures.get(ctx, bg));
        final ImageView face = new ImageView(ctx);
        face.setScaleType(ImageView.ScaleType.FIT_CENTER);
        face.setImageBitmap(Textures.get(ctx, fg));
        layer.addView(back, new FrameLayout.LayoutParams(-1, -1));
        layer.addView(face, new FrameLayout.LayoutParams(-1, -1));
        sfx.play(scream);

        final float amp = Math.min(layer.getWidth(), layer.getHeight()) * 0.06f;
        run(GLITCH_MS, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float s = 1.1f + rng.nextFloat() * 0.35f;
                // occasional mirror flip for extra wrongness
                face.setScaleX(rng.nextFloat() < 0.15f ? -s : s);
                face.setScaleY(s);
                face.setTranslationX((rng.nextFloat() * 2 - 1) * amp);
                face.setTranslationY((rng.nextFloat() * 2 - 1) * amp);
                back.setTranslationX((rng.nextFloat() * 2 - 1) * amp * 0.5f);
                face.setAlpha(rng.nextFloat() < 0.12f ? 0.3f : 1f);
            }
        }, done, back, face);
    }

    private void lunge(Bitmap bmp, int bgColor, ImageView.ScaleType st, int scream, final Runnable done) {
        final View bg = new View(ctx);
        bg.setBackgroundColor(bgColor);
        final ImageView img = new ImageView(ctx);
        img.setScaleType(st);
        img.setImageBitmap(bmp);
        layer.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        layer.addView(img, new FrameLayout.LayoutParams(-1, -1));
        sfx.play(scream);

        final float amp = Math.min(layer.getWidth(), layer.getHeight()) * 0.03f;
        run(LUNGE_MS, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float t = a.getAnimatedFraction();
                // rushes in over the first 40%, then overshoots and shakes
                float in = Math.min(1f, t / 0.4f);
                float s = 0.15f + 1.45f * in * in + (t > 0.4f ? 0.1f * (float) Math.sin(t * 60) : 0);
                img.setScaleX(s);
                img.setScaleY(s);
                float shake = t > 0.35f ? amp : amp * 0.2f;
                img.setTranslationX((rng.nextFloat() * 2 - 1) * shake);
                img.setTranslationY((rng.nextFloat() * 2 - 1) * shake);
                bg.setAlpha(Math.min(1f, t * 4));
            }
        }, done, bg, img);
    }

    private void run(long ms, ValueAnimator.AnimatorUpdateListener l, final Runnable done, final View... views) {
        cancel();
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(ms);
        a.setInterpolator(new LinearInterpolator());
        a.addUpdateListener(l);
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                running = null;
                runningViews = null;
                for (View v : views) layer.removeView(v);
                done.run();
            }
        });
        running = a;
        runningViews = views;
        a.start();
    }
}
