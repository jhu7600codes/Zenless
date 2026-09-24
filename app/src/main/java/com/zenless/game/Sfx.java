package com.zenless.game;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.zenless.game.enemy.RushEnemy;

import java.util.Random;

/**
 * No audio shipped in textures.zip, so every sound is synthesized once into PCM
 * and played through static AudioTracks.
 */
public final class Sfx {
    public static final int SCREAM_A90 = 0;
    public static final int SCREAM_RUSH = 1;
    public static final int SCREAM_FIGURE = 2;
    public static final int RUSH_CUE = 3;
    public static final int A90_STING = 4;
    public static final int BEEP_HALT = 5;
    public static final int BEEP_PROCEED = 6;
    public static final int SAFE = 7;
    public static final int FIGURE_STEP = 8;
    private static final int COUNT = 9;

    private static final int RATE = 22050;

    private final AudioTrack[] tracks = new AudioTrack[COUNT];
    private final Random rng = new Random(90);
    private volatile boolean ready;

    public Sfx() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                build(SCREAM_A90, scream(0.85f, 1400, 500, 0.55f));
                build(SCREAM_RUSH, scream(0.9f, 700, 220, 0.75f));
                build(SCREAM_FIGURE, growl(1.0f));
                build(RUSH_CUE, rumble(RushEnemy.CUE_MS / 1000f));
                build(A90_STING, sting(0.28f));
                build(BEEP_HALT, beep(0.12f, 880));
                build(BEEP_PROCEED, beep(0.12f, 440));
                build(SAFE, chime(0.35f));
                build(FIGURE_STEP, thud(0.18f));
                ready = true;
            }
        }, "sfx-synth").start();
    }

    public void play(int id) {
        if (!ready) return;
        AudioTrack t = tracks[id];
        if (t == null) return;
        try {
            if (t.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) t.stop();
            t.reloadStaticData();
            t.play();
        } catch (IllegalStateException ignored) {
        }
    }

    public void stop(int id) {
        AudioTrack t = tracks[id];
        if (t == null) return;
        try {
            t.stop();
        } catch (IllegalStateException ignored) {
        }
    }

    public void release() {
        ready = false;
        for (int i = 0; i < COUNT; i++) {
            if (tracks[i] != null) tracks[i].release();
            tracks[i] = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void build(int id, short[] pcm) {
        AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, pcm.length * 2, AudioTrack.MODE_STATIC);
        t.write(pcm, 0, pcm.length);
        tracks[id] = t;
    }

    // ---- synthesis ----

    private short[] scream(float sec, float f0, float f1, float noiseAmt) {
        int n = (int) (sec * RATE);
        short[] out = new short[n];
        double phase = 0, phase2 = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            double f = f0 + (f1 - f0) * t + 60 * Math.sin(i * 2 * Math.PI * 28 / RATE);
            phase += f / RATE;
            phase2 += f * 1.49 / RATE;
            double saw = 2 * (phase - Math.floor(phase)) - 1;
            double saw2 = 2 * (phase2 - Math.floor(phase2)) - 1;
            double v = 0.5 * saw + 0.3 * saw2 + noiseAmt * (rng.nextDouble() * 2 - 1);
            v = Math.tanh(v * 3.5); // distortion
            double env = Math.min(1, t * 40) * Math.pow(1 - t, 0.6);
            out[i] = (short) (v * env * 30000);
        }
        return out;
    }

    private short[] growl(float sec) {
        int n = (int) (sec * RATE);
        short[] out = new short[n];
        double phase = 0;
        double lp = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            double f = 140 + 90 * Math.sin(t * 9) + 400 * t;
            phase += f / RATE;
            double sq = (phase - Math.floor(phase)) < 0.5 ? 1 : -1;
            lp += 0.25 * ((rng.nextDouble() * 2 - 1) - lp);
            double v = Math.tanh((0.6 * sq + 0.9 * lp) * 4 * (0.7 + 0.3 * Math.sin(i * 2 * Math.PI * 35 / RATE)));
            double env = Math.min(1, t * 30) * Math.pow(1 - t, 0.5);
            out[i] = (short) (v * env * 30000);
        }
        return out;
    }

    private short[] rumble(float sec) {
        int n = (int) (sec * RATE);
        short[] out = new short[n];
        double brown = 0, phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            brown = (brown + 0.02 * (rng.nextDouble() * 2 - 1)) * 0.998;
            phase += (38 + 30 * t) / RATE;
            double hum = Math.sin(phase * 2 * Math.PI);
            // occasional crackle like flickering lights
            double crackle = rng.nextDouble() < 0.0008 + 0.004 * t ? (rng.nextDouble() * 2 - 1) : 0;
            double v = Math.tanh((brown * 12 + 0.5 * hum) * (0.3 + 1.7 * t * t)) + crackle * 0.8;
            out[i] = (short) (Math.max(-1, Math.min(1, v)) * 26000);
        }
        return out;
    }

    private short[] sting(float sec) {
        int n = (int) (sec * RATE);
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            // bit-crushed noise with a stepped pitch, glitchy
            double step = Math.floor(t * 12) % 2 == 0 ? 1 : -1;
            double v = (rng.nextDouble() * 2 - 1) * 0.6 + 0.5 * step * Math.signum(Math.sin(i * 2 * Math.PI * 180 / RATE));
            v = Math.round(v * 4) / 4.0;
            out[i] = (short) (Math.max(-1, Math.min(1, v)) * (1 - t) * 24000);
        }
        return out;
    }

    private short[] beep(float sec, float f) {
        int n = (int) (sec * RATE);
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            double env = Math.min(1, t * 30) * (1 - t);
            out[i] = (short) (Math.sin(i * 2 * Math.PI * f / RATE) * env * 14000);
        }
        return out;
    }

    private short[] chime(float sec) {
        int n = (int) (sec * RATE);
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            double f = t < 0.4 ? 660 : 990;
            out[i] = (short) (Math.sin(i * 2 * Math.PI * f / RATE) * Math.pow(1 - t, 2) * 12000);
        }
        return out;
    }

    private short[] thud(float sec) {
        int n = (int) (sec * RATE);
        short[] out = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            phase += (90 - 50 * t) / RATE;
            out[i] = (short) (Math.sin(phase * 2 * Math.PI) * Math.pow(1 - t, 3) * 22000);
        }
        return out;
    }
}
