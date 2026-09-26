package com.zenless.game;

import android.app.Activity;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/** Fullscreen with hidden bars (swipe to peek), content kept clear of display cutouts. */
public final class Immersive {
    private Immersive() {}

    /** Call after setContentView. {@code root} gets padded away from notches. */
    public static void setup(Activity a, View root) {
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = a.getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            a.getWindow().setAttributes(lp);
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    DisplayCutout c = insets.getDisplayCutout();
                    if (c != null) {
                        v.setPadding(c.getSafeInsetLeft(), c.getSafeInsetTop(), c.getSafeInsetRight(), c.getSafeInsetBottom());
                    } else {
                        v.setPadding(0, 0, 0, 0);
                    }
                    return insets;
                }
            });
        }
        hideBars(a);
    }

    /** Also call from onWindowFocusChanged, dialogs bring the bars back. */
    @SuppressWarnings("deprecation")
    public static void hideBars(Activity a) {
        if (Build.VERSION.SDK_INT >= 30) {
            a.getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = a.getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            a.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
}
