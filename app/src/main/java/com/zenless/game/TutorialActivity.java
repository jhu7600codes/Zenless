package com.zenless.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.zenless.game.enemy.EnemyType;

import java.util.Locale;

/** Walks through the game, every entity (with a practice button) and some money strats. */
public class TutorialActivity extends Activity {

    private static final class Page {
        final String title, body, image;
        final EnemyType practice;
        final boolean animated;

        Page(String title, String image, boolean animated, EnemyType practice, String body) {
            this.title = title;
            this.image = image;
            this.animated = animated;
            this.practice = practice;
            this.body = body;
        }
    }

    private static final Page[] PAGES = {
            new Page("Welcome to Zenless", null, false, null,
                    "tap the big blue circle to earn holos.\n\n"
                            + "holos buy buildings that earn for you every second, and upgrades that make everything stronger.\n\n"
                            + "but you're not alone in here. every so often a door opens, and something might be behind it."),
            new Page("Shop & upgrades", null, false, null,
                    "SHOP: buildings give holos every second. each one costs 15% more than the last. "
                            + "hold a shop row to buy 10 at once.\n\n"
                            + "UPGRADES: tiered boosts. Click Power doubles taps, Passive Boost multiplies income, "
                            + "Cost Reduction makes the shop cheaper, Critical Taps sometimes hit x10, "
                            + "Holo Synergy adds part of your per second income to every tap, "
                            + "and Night Light cuts what entities take from you."),
            new Page("Doors", null, false, null,
                    "a door opens every 30 seconds. every tap on the circle pulls the next one a little closer.\n\n"
                            + "each entity has a door where you always meet it for the first time:\n"
                            + "Rush at door 10, A-90 at 30, Figure at 50 and 100, A-90B at 70.\n\n"
                            + "after that they only have a small chance per door. "
                            + "the two doors after an encounter are quiet."),
            new Page("A-90", Textures.A90_STOP_SIGN, false, EnemyType.A90,
                    "A-90 flashes onto your screen for a second, then turns into a stop sign.\n\n"
                            + "while the sign is up: DON'T TOUCH ANYTHING.\n"
                            + "the only thing you're allowed to tap is the hand on the sign, which sends it away early.\n\n"
                            + "fail: a glitchy jumpscare and you lose 1/8 of your holos."),
            new Page("A-90B", Textures.A90B_HALT_SIGN, false, EnemyType.A90B,
                    "A-90B plays traffic light for about 15 seconds.\n\n"
                            + "HALT: put your finger down and hold.\n"
                            + "PROCEED: let go and tap at least once. those taps still earn holos.\n\n"
                            + "you get a short moment to react when the sign changes. "
                            + "holding through a proceed or letting go during a halt gets you.\n\n"
                            + "fail: you lose 1/6 of your holos."),
            new Page("Rush", Textures.RUSH_NORMAL, false, EnemyType.RUSH,
                    "you'll hear Rush before you see it. the lights start flickering.\n\n"
                            + "hold your finger on the screen to hide before it arrives, "
                            + "and keep holding until it's gone. you can still tap the circle while it's only a sound.\n\n"
                            + "fail: it lunges at you and takes 1/14 of your holos."),
            new Page("Figure", Textures.FIGURE_IDLE, true, EnemyType.FIGURE,
                    "Figure stays for 50 seconds and can't see, it listens.\n\n"
                            + "when it runs at you: HALT, hold the screen and don't move.\n"
                            + "when it walks away: PROCEED, let go. tapping is fine.\n\n"
                            + "one mistake and it gets you. survive the whole 50 seconds and it wanders off.\n\n"
                            + "fail: you lose 1/5 of your holos."),
            new Page("???", Textures.ICON, false, null,
                    "people say something waits behind the very first door.\n\n"
                            + "almost nobody has seen it. if you do, don't let it get away."),
            new Page("Rebirth", null, false, null,
                    "once you've earned 1M holos in a run you can rebirth.\n\n"
                            + "everything in the run resets, but you get superterrestrial items: "
                            + "+10% income each, forever, and you can spend them on permanent unlocks.\n\n"
                            + "more holos in a run means more items: 4M gives 2, 100M gives 10, 1B gives 31.\n\n"
                            + "your first rebirth also unlocks the admin panel. careful: turning it on "
                            + "disables achievements and the rift in that save for good."),
            new Page("The Rift", null, false, null,
                    "before you rebirth, put one thing in the rift: a building stack or an upgrade tier.\n\n"
                            + "it comes with you into the next run, then the rift empties again. "
                            + "one thing at a time, so pick well."),
            new Page("Money strats", null, false, null,
                    "• early on, tap like crazy and buy Holo Cursors and Tap Drones. Click Power is the best first upgrade.\n\n"
                            + "• once your per second income beats your taps, go for Passive Boost and bigger buildings.\n\n"
                            + "• Holo Synergy turns late game taps into a real income source.\n\n"
                            + "• surviving pays: A-90 gives 20s of income, Rush 30s, A-90B a minute, Figure two minutes.\n\n"
                            + "• Night Light makes failing hurt way less.\n\n"
                            + "• you earn while the app is closed too. the Dreaming unlock makes it better.\n\n"
                            + "• easy mode has no entities at all, perfect for farming a rebirth.\n\n"
                            + "• rift your best thing, usually Click Power or your biggest building stack."),
            new Page("Difficulty", null, false, null,
                    "every run starts with a difficulty:\n\n"
                            + "EASY: no entities, cheaper shop.\n"
                            + "NORMAL: only Figure, every 30 doors.\n"
                            + "HARD: everything, normal chances.\n"
                            + "EXTREME: double chances and A-90B in his original look.\n"
                            + "SUPER HARD MODE: pricier, weaker, way more entities, faster doors. good luck."),
    };

    private int page;
    private ImageView image;
    private TextView title, body, pageText;
    private Button back, next, practice;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_tutorial);
        Immersive.setup(this, findViewById(R.id.root));
        image = (ImageView) findViewById(R.id.tutImage);
        title = (TextView) findViewById(R.id.tutTitle);
        body = (TextView) findViewById(R.id.tutBody);
        pageText = (TextView) findViewById(R.id.tutPage);
        back = (Button) findViewById(R.id.tutBack);
        next = (Button) findViewById(R.id.tutNext);
        practice = (Button) findViewById(R.id.tutPractice);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page == 0) finish();
                else show(page - 1);
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page == PAGES.length - 1) finish();
                else show(page + 1);
            }
        });
        practice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(TutorialActivity.this, MainActivity.class);
                i.putExtra(MainActivity.EXTRA_PRACTICE, PAGES[page].practice.name());
                startActivity(i);
            }
        });
        show(saved != null ? saved.getInt("page", 0) : 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("page", page);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Immersive.hideBars(this);
    }

    private void show(int p) {
        page = p;
        Page pg = PAGES[p];
        pageText.setText(String.format(Locale.US, "TUTORIAL  %d/%d", p + 1, PAGES.length));
        title.setText(pg.title);
        body.setText(pg.body);
        if (pg.image == null) {
            image.setImageResource(R.drawable.logo);
        } else if (pg.animated) {
            AnimationDrawable a = Textures.animation(this, pg.image, 250);
            image.setImageDrawable(a);
            a.start();
        } else {
            image.setImageBitmap(Textures.get(this, pg.image));
        }
        // the secret stays a silhouette
        image.setAlpha(pg.title.equals("???") ? 0.08f : 1f);
        practice.setVisibility(pg.practice != null ? View.VISIBLE : View.GONE);
        practice.setText(pg.practice != null ? "PRACTICE " + pg.practice.label.toUpperCase(Locale.US) : "");
        back.setText(p == 0 ? "EXIT" : "BACK");
        next.setText(p == PAGES.length - 1 ? "DONE" : "NEXT");
    }
}
