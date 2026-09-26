package com.zenless.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zenless.game.enemy.Enemy;
import com.zenless.game.enemy.EnemyType;
import com.zenless.game.enemy.EventManager;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements EventManager.Listener {
    public static final String EXTRA_SLOT = "slot";
    /** tutorial practice: EnemyType name to spawn in a throwaway save */
    public static final String EXTRA_PRACTICE = "practice";

    private static final long OFFLINE_CAP_MS = 8 * 60 * 60 * 1000L;
    private static final int AUTOSAVE_EVERY_S = 30;

    private GameState state;
    private Economy eco;
    private int slot;
    private EnemyType practice;
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
    private View adminButton;
    private AdminPanel admin;
    private long autoClickAcc;
    private TextView riftInfo;
    private Button riftButton;

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
        Immersive.setup(this, findViewById(R.id.root));
        Settings.load(this);
        SaveSlots.migrate(this);

        String p = getIntent().getStringExtra(EXTRA_PRACTICE);
        practice = p == null ? null : EnemyType.valueOf(p);
        slot = practice != null ? SaveSlots.PRACTICE : getIntent().getIntExtra(EXTRA_SLOT, 1);
        if (practice != null) SaveSlots.delete(this, SaveSlots.PRACTICE);
        state = SaveSlots.load(this, slot);
        eco = new Economy(state);
        if (practice != null) state.difficulty = Difficulty.HARD.ordinal();
        sfx = new Sfx(this);
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
        admin = new AdminPanel(this, state, eco, events, new AdminPanel.Host() {
            @Override
            public void refreshAll() {
                MainActivity.this.refreshAll();
            }

            @Override
            public boolean spawn(EnemyType t, boolean buffed) {
                return events.trigger(t, buffed);
            }

            @Override
            public void askDifficulty() {
                refreshAll();
                MainActivity.this.askDifficulty();
            }
        });
        adminButton = findViewById(R.id.adminButton);
        adminButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAdmin();
            }
        });
        if (practice != null) {
            events.setDoorsEnabled(false);
            // wait for layout so the entity knows the screen size
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!events.trigger(practice, false)) handler.postDelayed(this, 200);
                }
            }, 700);
        }

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
        if (practice == null) grantOffline();
        running = true;
        lastFrame = SystemClock.uptimeMillis();
        handler.post(frameLoop);
        handler.postDelayed(secondLoop, 1000);
        if (state.difficulty < 0 && practice == null) askDifficulty();
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        handler.removeCallbacks(frameLoop);
        handler.removeCallbacks(secondLoop);
        events.pause();
        sfx.stopAll();
        holding = false;
        if (practice == null) {
            state.save(this);
        } else {
            // leaving practice mid entity just ends it
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (difficultyDialog != null) difficultyDialog.dismiss();
        sfx.release();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // dialogs and the swipe-to-peek bring the bars back, hide them again
        if (hasFocus) Immersive.hideBars(this);
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
            if (state.cheatAutoClick && !events.isActive()) {
                // 15 taps a second
                autoClickAcc += dt;
                while (autoClickAcc >= 66) {
                    autoClickAcc -= 66;
                    circle.tapCenter();
                }
            }
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
            if (practice == null && ++secondsSinceSave >= AUTOSAVE_EVERY_S) {
                secondsSinceSave = 0;
                state.save(MainActivity.this);
            }
            if (practice == null) announce(Achievement.check(MainActivity.this, state));
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
        state.tapped();
        if (Settings.haptics) circle.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        events.onPlayerTap();
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
        rebirthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmRebirth();
            }
        });

        riftInfo = (TextView) header.findViewById(R.id.riftInfo);
        riftButton = (Button) header.findViewById(R.id.riftButton);
        riftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickRift();
            }
        });
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
                r.cost.setText(maxed ? "MAXED" : cost + " Super" + (cost == 1 ? "" : "s"));
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

    /** ADMIN button. First time only: warn that progression goes away for this save forever. */
    private void openAdmin() {
        if (events.isActive()) return;
        if (state.adminEverUsed) {
            admin.show();
            return;
        }
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Open the admin panel?")
                .setMessage("progression disabled. you can't get achievements or use the rift inside this save."
                        + "\n\nthis stays for good once you open it.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        state.adminEverUsed = true;
                        state.adminEnabled = true;
                        state.riftKind = GameState.RIFT_EMPTY;
                        state.save(MainActivity.this);
                        refreshAll();
                        admin.show();
                    }
                })
                .show();
    }

    /** The rift carries one building stack or one upgrade tier into the next run. */
    private void pickRift() {
        if (state.progressionDisabled()) return;
        final java.util.ArrayList<int[]> choices = new java.util.ArrayList<>();
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < Building.ALL.length; i++) {
            if (state.buildings[i] <= 0) continue;
            choices.add(new int[]{GameState.RIFT_BUILDING, i});
            names.add(Building.ALL[i].name + " x" + state.buildings[i]);
        }
        for (int i = 0; i < Upgrade.ALL.length; i++) {
            if (state.upgrades[i] <= 0) continue;
            choices.add(new int[]{GameState.RIFT_UPGRADE, i});
            names.add(Upgrade.ALL[i].name + " tier " + state.upgrades[i]);
        }
        if (choices.isEmpty()) {
            toast("buy something first, the rift needs something to carry");
            return;
        }
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Put in the rift")
                .setItems(names.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        state.riftKind = choices.get(which)[0];
                        state.riftIndex = choices.get(which)[1];
                        state.save(MainActivity.this);
                        refreshRebirth();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                        + " Super" + (reward == 1 ? "" : "s")
                        + " (+10% income each, forever)."
                        + (state.riftLabel() != null && !state.progressionDisabled()
                        ? "\n\nThe rift carries " + state.riftLabel() + " into your next run." : "")
                        + (state.adminUnlocked ? "" : "\n\nRebirthing also unlocks the ADMIN button."))
                .setNegativeButton("Not yet", null)
                .setPositiveButton("Rebirth", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        boolean superHard = state.diff() == Difficulty.SUPER_HARD && state.difficulty >= 0;
                        boolean rift = state.riftKind != GameState.RIFT_EMPTY && !state.progressionDisabled();
                        long got = eco.rebirth();
                        if (got > 0) {
                            if (rift) state.riftEverUsed = true;
                            state.save(MainActivity.this);
                            announce(Achievement.check(MainActivity.this, state,
                                    superHard ? Achievement.MASOCHIST : null));
                            toast("reborn. +" + got + " Supers");
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
        String who = e.label();
        if (practice != null) {
            toast(survived ? "you survived " + who + ". nice." : who + " got you. try again from the tutorial.");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 1200);
            return;
        }
        if (survived) toast("survived " + who + "  +" + Fmt.holos(delta) + " holos");
        else toast(who + " got you  " + Fmt.holos(delta) + " holos");
        if (survived && e.isBuffed()) state.survivedSuper++;
        state.save(this);
        announce(Achievement.check(this, state, survived ? Achievement.forEntity(e.type()) : null));
        refreshAll();
    }

    private void announce(List<Achievement> fresh) {
        for (Achievement a : fresh) {
            Toast.makeText(this, "achievement unlocked: " + a.title + "\n" + a.desc, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDoor(int door, EnemyType spawned) {
        refreshTop();
    }

    // ---- ui refresh ----

    private void refreshTop() {
        adminButton.setVisibility(state.adminUnlocked && practice == null ? View.VISIBLE : View.GONE);
        holosText.setText(Fmt.holos(state.holos));
        ratesText.setText(Fmt.holos(eco.hps()) + "/sec  ·  " + Fmt.holos(eco.clickPower()) + "/tap");
        doorText.setText(state.difficulty < 0
                ? String.format(Locale.US, "DOOR %04d", state.door)
                : String.format(Locale.US, "%s \u00b7 DOOR %04d", state.diff().label.toUpperCase(Locale.US), state.door));
        stText.setText(state.stLifetime > 0 || state.stItems > 0
                ? state.stItems + " Supers" : "");
    }

    private void refreshRebirth() {
        long reward = eco.rebirthReward();
        rebirthInfo.setText(reward > 0
                ? "Rebirth now for " + reward + " Super" + (reward == 1 ? "" : "s") + "."
                : "Earn " + Fmt.holos(Economy.REBIRTH_MIN) + " holos in one run to rebirth ("
                + Fmt.holos(state.runEarned) + " so far).");
        rebirthButton.setEnabled(reward > 0);
        statsText.setText(String.format(Locale.US,
                "rebirths %d  ·  income x%.2f  ·  entities survived %d / failed %d",
                state.rebirths, eco.globalMult(), state.entitiesSurvived, state.entitiesFailed));
        if (state.progressionDisabled()) {
            riftInfo.setText("the rift is closed in this save.");
            riftButton.setEnabled(false);
        } else {
            String r = state.riftLabel();
            riftInfo.setText(r == null
                    ? "empty. put one building stack or upgrade tier in and it comes with you through the next rebirth."
                    : "carrying " + r + " into your next run.");
            riftButton.setEnabled(true);
            riftButton.setText(r == null ? "PUT SOMETHING IN THE RIFT" : "SWAP WHAT'S IN THE RIFT");
        }
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
