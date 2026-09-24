package com.zenless.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The big holo blue circle. Draws glow, ripples and floating "+N" popups. */
public class HoloCircleView extends View {

    public interface OnCircleTap {
        /** @return text for the popup, e.g. "+1.2K" */
        String onCircleTap();
    }

    private static final int BLUE = 0xFF33B5E5;
    private static final int BLUE_DARK = 0xFF0099CC;
    private static final long RIPPLE_MS = 550;
    private static final long POPUP_MS = 900;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripple = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<long[]> ripples = new ArrayList<>();   // {startTime}
    private final List<Popup> popups = new ArrayList<>();

    private float cx, cy, radius;
    private long pulseStart = -10_000;
    private OnCircleTap listener;
    private String centerLabel = "TAP";

    private static final class Popup {
        final String text;
        final float x, y;
        final long start;
        final boolean crit;

        Popup(String text, float x, float y, long start, boolean crit) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.start = start;
            this.crit = crit;
        }
    }

    public HoloCircleView(Context c) {
        this(c, null);
    }

    public HoloCircleView(Context c, AttributeSet a) {
        super(c, a);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(BLUE);
        ripple.setStyle(Paint.Style.STROKE);
        ripple.setColor(BLUE);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        label.setColor(0xCCFFFFFF);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setOnCircleTap(OnCircleTap l) {
        listener = l;
    }

    public void setCenterLabel(String s) {
        centerLabel = s;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx = w / 2f;
        cy = h / 2f;
        radius = Math.min(w, h) * 0.34f;
        float d = getResources().getDisplayMetrics().density;
        stroke.setStrokeWidth(3 * d);
        text.setTextSize(22 * d);
        label.setTextSize(Math.max(14 * d, radius * 0.22f));
        fill.setShader(new RadialGradient(cx, cy - radius * 0.3f, radius * 1.3f,
                new int[]{0xFF4FC6F0, BLUE_DARK, 0xFF06384C}, new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        glow.setShader(new RadialGradient(cx, cy, radius * 1.45f,
                new int[]{0x6633B5E5, 0x2233B5E5, 0x0033B5E5}, new float[]{0.6f, 0.8f, 1f},
                Shader.TileMode.CLAMP));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int idx = e.getActionIndex();
            float x = e.getX(idx), y = e.getY(idx);
            float dx = x - cx, dy = y - cy;
            // a bit of slack around the edge so fast tapping doesn't miss
            if (dx * dx + dy * dy <= radius * radius * 1.15f) {
                tap(x, y);
            }
            return true;
        }
        return action != MotionEvent.ACTION_CANCEL;
    }

    /** Also used by enemies that forward "proceed" taps. */
    public void tap(float x, float y) {
        if (listener == null) return;
        String s = listener.onCircleTap();
        if (s == null) return;
        long now = SystemClock.uptimeMillis();
        pulseStart = now;
        ripples.add(new long[]{now});
        boolean crit = s.endsWith("!");
        float jitter = (float) (Math.random() - 0.5) * radius * 0.4f;
        popups.add(new Popup(s, x + jitter, y, now, crit));
        if (popups.size() > 40) popups.remove(0);
        if (ripples.size() > 12) ripples.remove(0);
        postInvalidateOnAnimation();
    }

    public void tapCenter() {
        tap(cx, cy);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        boolean animating = false;

        // pulse: quick squash then spring back
        float t = (now - pulseStart) / 220f;
        float scale = 1f;
        if (t < 1f) {
            scale = 1f - 0.07f * (float) Math.sin(t * Math.PI);
            animating = true;
        }
        // idle breathing glow
        float breathe = 1f + 0.03f * (float) Math.sin(now / 700.0);

        c.save();
        c.scale(breathe * (t < 1f ? 1f + 0.08f * (1 - t) : 1f), breathe * (t < 1f ? 1f + 0.08f * (1 - t) : 1f), cx, cy);
        c.drawCircle(cx, cy, radius * 1.45f, glow);
        c.restore();

        Iterator<long[]> ri = ripples.iterator();
        while (ri.hasNext()) {
            float p = (now - ri.next()[0]) / (float) RIPPLE_MS;
            if (p >= 1f) {
                ri.remove();
                continue;
            }
            ripple.setAlpha((int) (200 * (1 - p)));
            ripple.setStrokeWidth(stroke.getStrokeWidth() * (3 - 2 * p));
            c.drawCircle(cx, cy, radius * (1f + 0.45f * p), ripple);
            animating = true;
        }

        c.save();
        c.scale(scale, scale, cx, cy);
        c.drawCircle(cx, cy, radius, fill);
        c.drawCircle(cx, cy, radius, stroke);
        c.drawCircle(cx, cy, radius * 0.82f, stroke);
        c.drawText(centerLabel, cx, cy - (label.descent() + label.ascent()) / 2, label);
        c.restore();

        Iterator<Popup> pi = popups.iterator();
        while (pi.hasNext()) {
            Popup pop = pi.next();
            float p = (now - pop.start) / (float) POPUP_MS;
            if (p >= 1f) {
                pi.remove();
                continue;
            }
            text.setColor(pop.crit ? 0xFFFFBB33 : Color.WHITE);
            text.setAlpha((int) (255 * (1 - p * p)));
            float size = text.getTextSize();
            if (pop.crit) text.setTextSize(size * 1.4f);
            c.drawText(pop.text, pop.x, pop.y - radius * 0.7f * p, text);
            if (pop.crit) text.setTextSize(size);
            animating = true;
        }

        // keep the breathing glow alive at a relaxed pace
        if (animating) postInvalidateOnAnimation();
        else postInvalidateDelayed(50);
    }
}
