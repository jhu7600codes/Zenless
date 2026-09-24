package com.zenless.game;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Enemy textures live in assets/textures/<enemy>/ with the exact names from textures.zip
 * (android resource names can't contain '-', so they stay assets instead of drawables).
 * Animated ones were split into frame folders so they play on every api level.
 */
public final class Textures {
    public static final String A90_NORMAL = "a-90/a-90normal.webp";
    public static final String A90_STOP_SIGN = "a-90/a-90stop-sign.webp";
    public static final String A90_DISTORT = "a-90/a-90distort.webp";
    public static final String A90_DISTORT_BG = "a-90/a-90distort_bg.png";

    public static final String A90B_NORMAL = "a-90b/a-90bnormal.webp";
    public static final String A90B_HALT_SIGN = "a-90b/a-90bhalt-sign.webp";
    public static final String A90B_PROCEED_SIGN = "a-90b/a-90bproceed-sign.webp";
    public static final String A90B_DISTORT = "a-90b/a-90bdistort.webp";
    public static final String A90B_DISTORT_BG = "a-90b/a-90bdistort_bg.png";

    public static final String RUSH_NORMAL = "rush/rush-normal.webp";
    public static final String RUSH_JUMPSCARE = "rush/rush-jumpscare.jpg";

    /** frame folders, see {@link #animation} */
    public static final String FIGURE_IDLE = "figure/figure-idle";
    public static final String FIGURE_RUNNING = "figure/figure-running";
    public static final String FIGURE_WALKING = "figure/figure-walking";

    private static final String ROOT = "textures/";
    private static final Map<String, Bitmap> cache = new HashMap<>();
    private static final Map<String, Bitmap[]> frameCache = new HashMap<>();

    private Textures() {}

    public static synchronized Bitmap get(Context ctx, String path) {
        Bitmap b = cache.get(path);
        if (b != null) return b;
        b = decode(ctx.getAssets(), ROOT + path);
        if (b != null) cache.put(path, b);
        return b;
    }

    public static BitmapDrawable drawable(Context ctx, String path) {
        return new BitmapDrawable(ctx.getResources(), get(ctx, path));
    }

    public static synchronized Bitmap[] frames(Context ctx, String folder) {
        Bitmap[] f = frameCache.get(folder);
        if (f != null) return f;
        AssetManager am = ctx.getAssets();
        try {
            String[] names = am.list(ROOT + folder);
            if (names == null) names = new String[0];
            Arrays.sort(names);
            f = new Bitmap[names.length];
            for (int i = 0; i < names.length; i++) f[i] = decode(am, ROOT + folder + "/" + names[i]);
        } catch (IOException e) {
            Log.e("Zenless", "missing frames " + folder, e);
            f = new Bitmap[0];
        }
        frameCache.put(folder, f);
        return f;
    }

    /** A fresh looping AnimationDrawable for a frame folder. */
    public static AnimationDrawable animation(Context ctx, String folder, int frameMs) {
        AnimationDrawable a = new AnimationDrawable();
        for (Bitmap b : frames(ctx, folder)) {
            if (b != null) a.addFrame(new BitmapDrawable(ctx.getResources(), b), frameMs);
        }
        a.setOneShot(false);
        return a;
    }

    /** Warm the cache off the main thread so the first spawn doesn't stutter. */
    public static void preload(final Context ctx) {
        final Context app = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] all = {A90_NORMAL, A90_STOP_SIGN, A90_DISTORT, A90_DISTORT_BG, A90B_NORMAL,
                        A90B_HALT_SIGN, A90B_PROCEED_SIGN, A90B_DISTORT, A90B_DISTORT_BG,
                        RUSH_NORMAL, RUSH_JUMPSCARE};
                for (String s : all) get(app, s);
                frames(app, FIGURE_IDLE);
                frames(app, FIGURE_RUNNING);
                frames(app, FIGURE_WALKING);
            }
        }, "tex-preload").start();
    }

    private static Bitmap decode(AssetManager am, String path) {
        InputStream in = null;
        try {
            in = am.open(path);
            return BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            Log.e("Zenless", "missing texture " + path, e);
            return null;
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }
}
