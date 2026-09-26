package com.zenless.game;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * Entity sounds come from assets/sounds/<enemy>/ (Doors wiki, A-90B from the Rooms Revisited wiki).
 * Each one gets a prepared MediaPlayer so replaying is just seek + start.
 * The "you survived" chime has no original, so it's synthesized.
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
    public static final int A90B_SPAWN = 9;
    public static final int FIGURE_SPAWN = 10;
    private static final int COUNT = 11;

    private static final String[] FILES = new String[COUNT];

    static {
        FILES[SCREAM_A90] = "sounds/a-90/A-90 jumpscare.mp3";
        FILES[A90_STING] = "sounds/a-90/A90SpawnSound.ogg";
        FILES[A90B_SPAWN] = "sounds/a-90b/Sensation spawn.ogg";
        FILES[BEEP_HALT] = "sounds/a-90b/Halt.ogg";
        FILES[BEEP_PROCEED] = "sounds/a-90b/Proceed.ogg";
        // trimmed to the 5s warning + pass, fading in as it gets closer
        FILES[RUSH_CUE] = "sounds/rush/PlaySound (Rush).ogg";
        FILES[SCREAM_RUSH] = "sounds/rush/RushScare.mp3";
        FILES[FIGURE_SPAWN] = "sounds/figure/FigureGrowl1.mp3";
        FILES[FIGURE_STEP] = "sounds/figure/FigureFootstep.mp3";
        FILES[SCREAM_FIGURE] = "sounds/figure/FigureKillNew.mp3";
    }

    private static final int RATE = 22050;

    private final MediaPlayer[] players = new MediaPlayer[COUNT];
    private AudioTrack chime;
    private volatile boolean ready;

    public Sfx(Context ctx) {
        final Context app = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < COUNT; i++) {
                    if (FILES[i] != null) players[i] = load(app, FILES[i]);
                }
                chime = buildChime();
                ready = true;
            }
        }, "sfx-load").start();
    }

    public void play(int id) {
        if (!ready || !Settings.sound) return;
        if (id == SAFE) {
            playChime();
            return;
        }
        MediaPlayer mp = players[id];
        if (mp == null) return;
        try {
            mp.seekTo(0);
            mp.start();
        } catch (IllegalStateException ignored) {
        }
    }

    public void stop(int id) {
        if (!ready) return;
        MediaPlayer mp = players[id];
        if (mp == null) return;
        try {
            if (mp.isPlaying()) mp.pause();
            mp.seekTo(0);
        } catch (IllegalStateException ignored) {
        }
    }

    public void stopAll() {
        for (int i = 0; i < COUNT; i++) stop(i);
    }

    public void release() {
        ready = false;
        for (int i = 0; i < COUNT; i++) {
            if (players[i] != null) players[i].release();
            players[i] = null;
        }
        if (chime != null) chime.release();
        chime = null;
    }

    private static MediaPlayer load(Context ctx, String path) {
        AssetFileDescriptor fd = null;
        try {
            fd = ctx.getAssets().openFd(path);
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mp.prepare();
            return mp;
        } catch (Exception e) {
            Log.e("Zenless", "can't load sound " + path, e);
            return null;
        } finally {
            if (fd != null) try {
                fd.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void playChime() {
        if (chime == null) return;
        try {
            if (chime.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) chime.stop();
            chime.reloadStaticData();
            chime.play();
        } catch (IllegalStateException ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private static AudioTrack buildChime() {
        int n = (int) (0.35f * RATE);
        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            double f = t < 0.4 ? 660 : 990;
            pcm[i] = (short) (Math.sin(i * 2 * Math.PI * f / RATE) * Math.pow(1 - t, 2) * 12000);
        }
        AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, n * 2, AudioTrack.MODE_STATIC);
        t.write(pcm, 0, n);
        return t;
    }
}
