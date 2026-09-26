package com.zenless.game;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

/** Logo, PLAY / SETTINGS / TUTORIAL, save slots and the achievement badges up top. */
public class TitleActivity extends Activity {

    private View menuPanel, savesPanel;
    private LinearLayout slots, badges;
    private TextView badgeCount;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_title);
        Immersive.setup(this, findViewById(R.id.root));
        Settings.load(this);
        SaveSlots.migrate(this);
        Textures.preload(this);

        menuPanel = findViewById(R.id.menuPanel);
        savesPanel = findViewById(R.id.savesPanel);
        slots = (LinearLayout) findViewById(R.id.slots);
        badges = (LinearLayout) findViewById(R.id.badges);
        badgeCount = (TextView) findViewById(R.id.badgeCount);

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaves(true);
            }
        });
        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });
        findViewById(R.id.tutorial).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TitleActivity.this, TutorialActivity.class));
            }
        });
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaves(false);
            }
        });

        // slow glow breathing on the logo
        ObjectAnimator glow = ObjectAnimator.ofFloat(findViewById(R.id.logo), "alpha", 1f, 0.8f);
        glow.setDuration(2200);
        glow.setRepeatMode(ValueAnimator.REVERSE);
        glow.setRepeatCount(ValueAnimator.INFINITE);
        glow.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildBadges();
        buildSlots();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Immersive.hideBars(this);
    }

    @Override
    public void onBackPressed() {
        if (savesPanel.getVisibility() == View.VISIBLE) showSaves(false);
        else super.onBackPressed();
    }

    private void showSaves(boolean saves) {
        menuPanel.setVisibility(saves ? View.GONE : View.VISIBLE);
        savesPanel.setVisibility(saves ? View.VISIBLE : View.GONE);
        if (saves) buildSlots();
    }

    // ---- achievements ----

    private void buildBadges() {
        badges.removeAllViews();
        int size = dp(36);
        for (final Achievement a : Achievement.values()) {
            boolean got = a.isUnlocked(this);
            TextView b = new TextView(this);
            b.setGravity(Gravity.CENTER);
            b.setTextSize(14);
            b.setText(got ? a.title.substring(0, 1).toUpperCase(Locale.US) : "?");
            b.setTextColor(got ? 0xFFFFFFFF : 0xFF5A5A5A);
            b.setBackgroundResource(got ? R.drawable.badge_unlocked : R.drawable.badge_locked);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAchievement(a);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.rightMargin = dp(8);
            badges.addView(b, lp);
        }
        badgeCount.setText(String.format(Locale.US, "achievements %d/%d",
                Achievement.unlockedCount(this), Achievement.values().length));
    }

    private void showAchievement(Achievement a) {
        AlertDialog.Builder d = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        if (a.isUnlocked(this)) {
            d.setTitle(a.title).setMessage(a.desc + "\n\nunlocked: " + a.how);
        } else {
            // locked ones only give away the flavor line
            d.setTitle("???").setMessage(a.desc);
        }
        d.setPositiveButton("OK", null).show();
    }

    // ---- saves ----

    private void buildSlots() {
        slots.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (int slot = 1; slot <= SaveSlots.COUNT; slot++) {
            final int s = slot;
            View row = inf.inflate(R.layout.row_slot, slots, false);
            TextView title = (TextView) row.findViewById(R.id.slotTitle);
            TextView summary = (TextView) row.findViewById(R.id.slotSummary);
            View warning = row.findViewById(R.id.slotWarning);
            title.setText("SAVE " + slot);
            if (SaveSlots.exists(this, slot)) {
                GameState st = SaveSlots.load(this, slot);
                summary.setText(String.format(Locale.US, "%s holos · %s · door %d · %d rebirths",
                        Fmt.holos(st.holos),
                        st.difficulty < 0 ? "no difficulty yet" : st.diff().label,
                        st.door, st.rebirths));
                warning.setVisibility(st.progressionDisabled() ? View.VISIBLE : View.GONE);
                row.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        confirmDelete(s);
                        return true;
                    }
                });
            } else {
                summary.setText("empty — tap to start");
            }
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(TitleActivity.this, MainActivity.class);
                    i.putExtra(MainActivity.EXTRA_SLOT, s);
                    startActivity(i);
                }
            });
            slots.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void confirmDelete(final int slot) {
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Delete save " + slot + "?")
                .setMessage("This can't be undone. Achievements you already have stay.")
                .setNegativeButton("Keep it", null)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        SaveSlots.delete(TitleActivity.this, slot);
                        buildSlots();
                    }
                })
                .show();
    }

    // ---- settings ----

    private void showSettings() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        bindSetting(v, R.id.setSound, Settings.sound, 0);
        bindSetting(v, R.id.setHaptics, Settings.haptics, 1);
        bindSetting(v, R.id.setFlashing, Settings.reduceFlashing, 2);
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Settings")
                .setView(v)
                .setPositiveButton("Done", null)
                .show();
    }

    private void bindSetting(View root, int id, boolean value, final int which) {
        Switch sw = (Switch) root.findViewById(id);
        sw.setChecked(value);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean on) {
                if (which == 0) Settings.sound = on;
                else if (which == 1) Settings.haptics = on;
                else Settings.reduceFlashing = on;
                Settings.save(TitleActivity.this);
            }
        });
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
