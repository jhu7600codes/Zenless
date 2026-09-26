package com.zenless.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zenless.game.enemy.EnemyType;
import com.zenless.game.enemy.EventManager;

import java.util.Locale;

/** The cheat client. Opened from the ADMIN button next to the holo counter. */
public class AdminPanel {

    public interface Host {
        void refreshAll();

        /** close the panel first, then spawn */
        boolean spawn(EnemyType t, boolean buffed);

        void askDifficulty();
    }

    private final Activity act;
    private final GameState s;
    private final Economy eco;
    private final EventManager events;
    private final Host host;
    private AlertDialog dialog;
    private LinearLayout root;

    public AdminPanel(Activity act, GameState s, Economy eco, EventManager events, Host host) {
        this.act = act;
        this.s = s;
        this.eco = eco;
        this.events = events;
        this.host = host;
    }

    public void show() {
        ScrollView scroll = new ScrollView(act);
        root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(8), dp(16), dp(16));
        scroll.addView(root);
        build();
        dialog = new AlertDialog.Builder(act, AlertDialog.THEME_HOLO_DARK)
                .setTitle("ADMIN PANEL")
                .setView(scroll)
                .setPositiveButton("Close", null)
                .create();
        dialog.show();
    }

    private void rebuild() {
        root.removeAllViews();
        build();
        host.refreshAll();
    }

    private void build() {
        section("HOLOS", "you have " + Fmt.holos(s.holos));
        row(btn("+1K", add(1e3)), btn("+1M", add(1e6)), btn("+1B", add(1e9)));
        row(btn("+1T", add(1e12)), btn("x10", new Runnable() {
            @Override
            public void run() {
                s.earn(Math.max(1, s.holos) * 9);
            }
        }), btn("SET 0", new Runnable() {
            @Override
            public void run() {
                s.holos = 0;
            }
        }));

        section("SUPERS", "you have " + s.stItems + " (income x" + String.format(Locale.US, "%.1f", eco.globalMult()) + ")");
        row(btn("+1", supers(1)), btn("+10", supers(10)), btn("+100", supers(100)));

        section("DOORS", "door " + s.door);
        row(btn("OPEN NEXT", new Runnable() {
            @Override
            public void run() {
                events.openDoorNow();
                toast("the next door is opening");
            }
        }), btn("SKIP 1", skip(1)), btn("SKIP 10", skip(10)));
        row(btn("SKIP 100", skip(100)), btn("SKIP 500", skip(500)));

        section("ENTITIES", "tap to spawn, hold for the SUPER version");
        row(spawn("A-90", EnemyType.A90), spawn("A-90B", EnemyType.A90B), spawn("RUSH", EnemyType.RUSH));
        row(spawn("FIGURE", EnemyType.FIGURE), spawn("???", EnemyType.SECRET));
        row(toggle("NO ENTITIES", s.cheatNoEntities, new Runnable() {
            @Override
            public void run() {
                s.cheatNoEntities = !s.cheatNoEntities;
            }
        }), toggle("INVINCIBLE", s.cheatInvincible, new Runnable() {
            @Override
            public void run() {
                s.cheatInvincible = !s.cheatInvincible;
            }
        }));

        section("RUN", "difficulty: " + (s.difficulty < 0 ? "not picked" : s.diff().label) + ", rebirths: " + s.rebirths);
        row(btn("DIFFICULTY", new Runnable() {
            @Override
            public void run() {
                pickDifficulty();
            }
        }), btn("FREE REBIRTH", new Runnable() {
            @Override
            public void run() {
                long got = eco.forceRebirth();
                toast("reborn. +" + got + " Supers");
                dialog.dismiss();
                host.askDifficulty();
            }
        }));
        row(btn("+1 REBIRTH", rebirths(1)), btn("+10 REBIRTHS", rebirths(10)));

        section("SHOP", null);
        row(btn("+10 ALL BUILDINGS", new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < s.buildings.length; i++) s.buildings[i] += 10;
            }
        }), btn("MAX UPGRADES", new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < Upgrade.ALL.length; i++) s.upgrades[i] = Upgrade.ALL[i].maxTier;
            }
        }));
        row(btn("MAX SUPER UNLOCKS", new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < MetaUpgrade.ALL.length; i++) s.meta[i] = MetaUpgrade.ALL[i].maxTier;
            }
        }), btn("RESET UPGRADES", new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < s.upgrades.length; i++) s.upgrades[i] = 0;
            }
        }));

        section("CHEATS", null);
        row(btn("INCOME x" + s.cheatIncomeMult, new Runnable() {
            @Override
            public void run() {
                // 1 -> 2 -> 10 -> 100 -> 1
                s.cheatIncomeMult = s.cheatIncomeMult == 1 ? 2 : s.cheatIncomeMult == 2 ? 10
                        : s.cheatIncomeMult == 10 ? 100 : 1;
            }
        }), toggle("AUTOCLICKER", s.cheatAutoClick, new Runnable() {
            @Override
            public void run() {
                s.cheatAutoClick = !s.cheatAutoClick;
            }
        }));
    }

    // ---- actions ----

    private Runnable add(final double amount) {
        return new Runnable() {
            @Override
            public void run() {
                s.earn(amount);
            }
        };
    }

    private Runnable supers(final long n) {
        return new Runnable() {
            @Override
            public void run() {
                s.stItems += n;
                s.stLifetime += n;
            }
        };
    }

    private Runnable skip(final int n) {
        return new Runnable() {
            @Override
            public void run() {
                events.skipDoors(n);
            }
        };
    }

    private Runnable rebirths(final int n) {
        return new Runnable() {
            @Override
            public void run() {
                s.rebirths += n;
            }
        };
    }

    private void pickDifficulty() {
        final Difficulty[] all = Difficulty.values();
        CharSequence[] names = new CharSequence[all.length];
        for (int i = 0; i < all.length; i++) names[i] = all[i].label;
        new AlertDialog.Builder(act, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Switch difficulty (keeps your run)")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        s.difficulty = all[which].ordinal();
                        events.resetDoors();
                        s.save(act);
                        rebuild();
                    }
                })
                .show();
    }

    // ---- view helpers ----

    private void section(String title, String sub) {
        TextView t = new TextView(act);
        t.setText(title);
        t.setTextColor(0xFFFF4444);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setPadding(0, dp(14), 0, dp(2));
        root.addView(t);
        if (sub != null) {
            TextView st = new TextView(act);
            st.setText(sub);
            st.setTextColor(0xFF9A9A9A);
            st.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            root.addView(st);
        }
    }

    private void row(View... views) {
        LinearLayout r = new LinearLayout(act);
        r.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : views) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            r.addView(v, lp);
        }
        root.addView(r);
    }

    private Button btn(String label, final Runnable action) {
        Button b = new Button(act);
        b.setText(label);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setTextColor(0xFFF3F3F3);
        b.setBackgroundResource(R.drawable.holo_button_bg);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
                s.save(act);
                if (dialog.isShowing()) rebuild();
            }
        });
        return b;
    }

    private Button toggle(String label, boolean on, Runnable flip) {
        Button b = btn(label + (on ? ": ON" : ": OFF"), flip);
        b.setTextColor(on ? 0xFF99CC00 : 0xFF9A9A9A);
        return b;
    }

    private Button spawn(String label, final EnemyType t) {
        Button b = new Button(act);
        b.setText(label);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setTextColor(0xFFFFBB33);
        b.setBackgroundResource(R.drawable.holo_button_bg);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (!host.spawn(t, false)) toast("something is already here");
            }
        });
        b.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialog.dismiss();
                if (!host.spawn(t, t != EnemyType.SECRET)) toast("something is already here");
                return true;
            }
        });
        return b;
    }

    private void toast(String msg) {
        Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(float v) {
        return (int) (v * act.getResources().getDisplayMetrics().density + 0.5f);
    }
}
