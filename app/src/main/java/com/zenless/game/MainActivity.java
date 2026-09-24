package com.zenless.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.zenless.game.enemy.Enemy;
import com.zenless.game.enemy.EnemyType;
import com.zenless.game.enemy.EventManager;

import java.util.Locale;

public class MainActivity extends Activity implements EventManager.Listener {
    private static final long OFFLINE_CAP_MS = 8 * 60 * 60 * 1000L;
    private static final int AUTOSAVE_EVERY_S = 30;

    private final GameState state = new GameState();
    private final Economy eco = new Economy(state);
    private final Handler handler = new Handler();

    private Sfx sfx;
    private EventManager events;

    private HoloCircleView circle;
    private TextView holosText, ratesText, doorText, stText;
    private ProgressBar doorProgress;
    private TextView[] tabs;
    private ListView[] lists;
    private int currentTab;

    private TextView rebirthInfo, statsText;
    private Button rebirthButton;
    private View adminPanel, adminButtons;
    private Switch adminSwitch;

    private boolean holding;
    private long lastFrame;
    private int secondsSinceSave;
    private boolean running;
    private AlertDialog difficultyDialog;

    // ---- lifecycle ----

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        setupFullscreen();
        state.load(this);
        sfx = new Sfx();
        Textures.preload(this);

        holosText = (TextView) findViewById(R.id.holos);
        ratesText = (TextView) findViewById(R.id.rates);
        doorText = (TextView) findViewById(R.id.door);
        stText = (TextView) findViewById(R.id.stItems);
        doorProgress = (ProgressBar) findViewById(R.id.doorProgress);
        circle = (HoloCircleView) findViewById(R.id.circle);
        circle.setOnCircleTap(new HoloCircleView.OnCircleTap() {
            @Override
            public String onCircleTap() {
                return onTap();
            }
        });

        FrameLayout layer = (FrameLayout) findViewById(R.id.enemyLayer);
        events = new EventManager(this, layer, sfx, state, eco, this);

        setupTabs();
        setupShop();
        setupUpgrades();
        setupRebirth();
        selectTab(0);
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        grantOffline();
        running = true;
        lastFrame = SystemClock.uptimeMillis();
        handler.post(frameLoop);
        handler.postDelayed(secondLoop, 1000);
        if (state.difficulty < 0) askDifficulty();
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        handler.removeCallbacks(frameLoop);
        handler.removeCallbacks(secondLoop);
        events.pause();
        holding = false;
        state.save(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (difficultyDialog != null) difficultyDialog.dismiss();
        sfx.release();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // dialogs and the swipe-to-peek bring the bars back, hide them again
        if (hasFocus) hideSystemBars();
    }

    // ---- fullscreen ----

    private void setupFullscreen() {
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
            // draw under the notch but keep the ui itself clear of it
            final View root = findViewById(R.id.root);
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    android.view.DisplayCutout c = insets.getDisplayCutout();
                    if (c != null) {
                        v.setPadding(c.getSafeInsetLeft(), c.getSafeInsetTop(), c.getSafeInsetRight(), c.getSafeInsetBottom());
                    } else {
                        v.setPadding(0, 0, 0, 0);
                    }
                    return insets;
                }
            });
        }
        hideSystemBars();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onBackPressed() {
        // no running away from an entity
        if (!events.isActive()) super.onBackPressed();
    }

    private void grantOffline() {
        if (state.lastSaveTime <= 0) return;
        long away = Math.min(OFFLINE_CAP_MS, System.currentTimeMillis() - state.lastSaveTime);
        if (away < 60_000) return;
        double gain = eco.hps() * (away / 1000.0) * eco.offlineRate();
        if (gain < 1) return;
        state.earn(gain);
        state.lastSaveTime = System.currentTimeMillis();
        toast("while you were away: +" + Fmt.holos(gain) + " holos");
        refreshAll();
    }

    // ---- loops ----

    private final Runnable frameLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long now = SystemClock.uptimeMillis();
            long dt = Math.min(100, now - lastFrame);
            lastFrame = now;
            events.tick(dt);
            doorProgress.setProgress(events.isActive() ? 1000 : (int) (events.doorProgress() * 1000));
            handler.postDelayed(this, 16);
        }
    };

    private final Runnable secondLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            // passive income tick
            state.earn(eco.hps());
            if (++secondsSinceSave >= AUTOSAVE_EVERY_S) {
                secondsSinceSave = 0;
                state.save(MainActivity.this);
            }
            refreshAll();
            handler.postDelayed(this, 1000);
        }
    };

    // ---- touch routing ----

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) holding = true;
        else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) holding = false;

        if (events.isActive()) {
            events.onTouch(e);
            return true;
        }
        return super.dispatchTouchEvent(e);
    }

    @Override
    public boolean isHolding() {
        return holding;
    }

    // ---- tapping ----

    private String onTap() {
        double p = eco.clickPower();
        boolean crit = eco.rollCrit();
        if (crit) p *= 10;
        state.earn(p);
        state.runTaps++;
        refreshTop();
        return "+" + Fmt.holos(p) + (crit ? "!" : "");
    }

    @Override
    public void tapCircle() {
        circle.tapCenter();
    }

    // ---- tabs ----

    private void setupTabs() {
        tabs = new TextView[]{
                (TextView) findViewById(R.id.tabShop),
                (TextView) findViewById(R.id.tabUpgrades),
                (TextView) findViewById(R.id.tabRebirth)};
        lists = new ListView[]{
                (ListView) findViewById(R.id.listShop),
                (ListView) findViewById(R.id.listUpgrades),
                (ListView) findViewById(R.id.listRebirth)};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectTab(idx);
                }
            });
        }
    }

    private void selectTab(int idx) {
        currentTab = idx;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setSelected(i == idx);
            lists[i].setVisibility(i == idx ? View.VISIBLE : View.GONE);
        }
        refreshAll();
    }

    // ---- shop ----

    private void setupShop() {
        lists[0].setAdapter(new RowAdapter(Building.ALL.length) {
            @Override
            protected boolean bind(int i, Row r) {
                Building b = Building.ALL[i];
                double cost = eco.buildingCost(i);
                r.title.setText(b.name);
                r.subtitle.setText(b.desc + "  ·  +" + Fmt.holos(eco.buildingHps(i)) + "/sec each");
                r.cost.setText(Fmt.holos(cost) + " holos");
                r.count.setText(String.valueOf(state.buildings[i]));
                return state.holos >= cost;
            }
        });
        lists[0].setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (eco.buyBuilding(pos)) refreshAll();
            }
        });
        // long press buys up to 10
        lists[0].setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                int n = 0;
                while (n < 10 && eco.buyBuilding(pos)) n++;
                if (n > 0) toast("bought " + n + " x " + Building.ALL[pos].name);
                refreshAll();
                return true;
            }
        });
    }

    // ---- upgrades ----

    private void setupUpgrades() {
        lists[1].setAdapter(new RowAdapter(Upgrade.ALL.length) {
            @Override
            protected boolean bind(int i, Row r) {
                Upgrade u = Upgrade.ALL[i];
                int tier = state.upgrades[i];
                boolean maxed = eco.upgradeMaxed(i);
                r.title.setText(u.name);
                r.subtitle.setText(u.effectPerTier + " per tier" + (state.diff().power < 1 ? " (halved)" : ""));
                r.cost.setText(maxed ? "MAXED" : Fmt.holos(eco.upgradeCost(i)) + " holos");
                r.count.setText(String.format(Locale.US, "%d/%d", tier, u.maxTier));
                return !maxed && state.holos >= eco.upgradeCost(i);
            }
        });
        lists[1].setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (eco.buyUpgrade(pos)) refreshAll();
            }
        });
    }

    // ---- rebirth + admin ----

    private void setupRebirth() {
        View header = LayoutInflater.from(this).inflate(R.layout.rebirth_header, lists[2], false);
        rebirthInfo = (TextView) header.findViewById(R.id.rebirthInfo);
        statsText = (TextView) header.findViewById(R.id.stats);
        rebirthButton = (Button) header.findViewById(R.id.rebirthButton);
        adminPanel = header.findViewById(R.id.adminPanel);
        adminButtons = header.findViewById(R.id.adminButtons);
        adminSwitch = (Switch) header.findViewById(R.id.adminSwitch);

        rebirthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmRebirth();
            }
        });

        adminSwitch.setChecked(state.adminEnabled);
        adminSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean on) {
                state.adminEnabled = on && state.adminUnlocked;
                state.save(MainActivity.this);
                refreshRebirth();
            }
        });
        bindSpawn(header, R.id.spawnA90, EnemyType.A90);
        bindSpawn(header, R.id.spawnA90B, EnemyType.A90B);
        bindSpawn(header, R.id.spawnRush, EnemyType.RUSH);
        bindSpawn(header, R.id.spawnFigure, EnemyType.FIGURE);

        lists[2].addHeaderView(header, null, false);
        lists[2].setAdapter(new RowAdapter(MetaUpgrade.ALL.length) {
            @Override
            protected boolean bind(int i, Row r) {
                MetaUpgrade m = MetaUpgrade.ALL[i];
                int tier = state.meta[i];
                boolean maxed = tier >= m.maxTier;
                long cost = m.costFor(tier);
                r.title.setText(m.name);
                r.subtitle.setText(m.effect);
                r.cost.setText(maxed ? "MAXED" : cost + " superterrestrial item" + (cost == 1 ? "" : "s"));
                r.cost.setTextColor(getResources().getColor(R.color.holo_purple));
                r.count.setText(String.format(Locale.US, "%d/%d", tier, m.maxTier));
                return !maxed && state.stItems >= cost;
            }
        });
        lists[2].setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                int i = pos - lists[2].getHeaderViewsCount();
                if (i >= 0 && eco.buyMeta(i)) {
                    state.save(MainActivity.this);
                    refreshAll();
                }
            }
        });
    }

    private void bindSpawn(View root, int id, final EnemyType t) {
        root.findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!state.adminEnabled) return;
                if (!events.trigger(t)) toast("something is already here");
            }
        });
    }

    private void confirmRebirth() {
        final long reward = eco.rebirthReward();
        if (reward <= 0) {
            toast("earn " + Fmt.holos(Economy.REBIRTH_MIN) + " holos this run to rebirth");
            return;
        }
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Rebirth?")
                .setMessage("Your holos, shop and upgrades reset.\n\nYou get " + reward
                        + " superterrestrial item" + (reward == 1 ? "" : "s")
                        + " (+10% income each, forever)."
                        + (state.adminUnlocked ? "" : "\n\nRebirthing also unlocks the admin panel."))
                .setNegativeButton("Not yet", null)
                .setPositiveButton("Rebirth", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        long got = eco.rebirth();
                        if (got > 0) {
                            state.save(MainActivity.this);
                            toast("reborn. +" + got + " superterrestrial items");
                            refreshAll();
                            askDifficulty();
                        }
                    }
                })
                .show();
    }

    // ---- difficulty ----

    /** Asked at the start of every run. Can't be dismissed, the run waits until you pick. */
    private void askDifficulty() {
        if (difficultyDialog != null && difficultyDialog.isShowing()) return;
        final Difficulty[] all = Difficulty.values();
        CharSequence[] items = new CharSequence[all.length];
        for (int i = 0; i < all.length; i++) items[i] = all[i].label + "\n" + all[i].desc;
        difficultyDialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("What difficulty?")
                .setCancelable(false)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        state.difficulty = all[which].ordinal();
                        events.resetDoors();
                        state.save(MainActivity.this);
                        toast(all[which].label + " run started");
                        refreshAll();
                    }
                })
                .create();
        difficultyDialog.show();
    }

    // ---- EventManager.Listener ----

    @Override
    public void onEnemySpawned(Enemy e) {
        // whatever was being pressed underneath (a list row, a button) shouldn't stay pressed
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        super.dispatchTouchEvent(cancel);
        cancel.recycle();
    }

    @Override
    public void onEnemyFinished(Enemy e, boolean survived, double delta) {
        String who = e.type().label;
        if (survived) toast("survived " + who + "  +" + Fmt.holos(delta) + " holos");
        else toast(who + " got you  " + Fmt.holos(delta) + " holos");
        state.save(this);
        refreshAll();
    }

    @Override
    public void onDoor(int door, EnemyType spawned) {
        refreshTop();
    }

    // ---- ui refresh ----

    private void refreshTop() {
        holosText.setText(Fmt.holos(state.holos));
        ratesText.setText(Fmt.holos(eco.hps()) + "/sec  ·  " + Fmt.holos(eco.clickPower()) + "/tap");
        doorText.setText(state.difficulty < 0
                ? String.format(Locale.US, "DOOR %04d", state.door)
                : String.format(Locale.US, "%s \u00b7 DOOR %04d", state.diff().label.toUpperCase(Locale.US), state.door));
        stText.setText(state.stLifetime > 0 || state.stItems > 0
                ? state.stItems + " superterrestrial" : "");
    }

    private void refreshRebirth() {
        long reward = eco.rebirthReward();
        rebirthInfo.setText(reward > 0
                ? "Rebirth now for " + reward + " superterrestrial item" + (reward == 1 ? "" : "s") + "."
                : "Earn " + Fmt.holos(Economy.REBIRTH_MIN) + " holos in one run to rebirth ("
                + Fmt.holos(state.runEarned) + " so far).");
        rebirthButton.setEnabled(reward > 0);
        statsText.setText(String.format(Locale.US,
                "rebirths %d  ·  income x%.2f  ·  entities survived %d / failed %d",
                state.rebirths, eco.globalMult(), state.entitiesSurvived, state.entitiesFailed));
        adminPanel.setVisibility(state.adminUnlocked ? View.VISIBLE : View.GONE);
        if (adminSwitch.isChecked() != state.adminEnabled) adminSwitch.setChecked(state.adminEnabled);
        adminButtons.setVisibility(state.adminEnabled ? View.VISIBLE : View.GONE);
    }

    private void refreshAll() {
        refreshTop();
        if (currentTab == 2) refreshRebirth();
        RowAdapter.refreshVisible(lists[currentTab]);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
