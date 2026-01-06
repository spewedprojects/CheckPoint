package com.gratus.retrack;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {

    private static final String PREFS_NAME = "app_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Allowed values
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK  = "dark";
    public static final String MODE_AUTO  = "auto";

    private ThemeManager() {
        // no instances
    }

    /* ---------------------------------------------------------
       Apply saved theme ONCE (call at startup)
       --------------------------------------------------------- */
    public static void applySavedTheme(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String mode = prefs.getString(KEY_THEME_MODE, MODE_AUTO);
        applyMode(mode);
    }

    /* ---------------------------------------------------------
       Set theme from user action (buttons)
       --------------------------------------------------------- */
    public static void setLight(Context context) {
        save(context, MODE_LIGHT);
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static void setDark(Context context) {
        save(context, MODE_DARK);
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
        );
    }

    public static void setAuto(Context context) {
        save(context, MODE_AUTO);
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
    }

    /* ---------------------------------------------------------
       Query current user-selected mode (for button visibility)
       --------------------------------------------------------- */
    public static String getSavedMode(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString(KEY_THEME_MODE, MODE_AUTO);
    }

    /* ---------------------------------------------------------
       Internal helpers
       --------------------------------------------------------- */
    private static void save(Context context, String mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME_MODE, mode)
                .apply();
    }

    private static void applyMode(String mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                );
                break;

            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                );
                break;

            case MODE_AUTO:
            default:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                );
                break;
        }
    }
}

