package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemePreferenceManager {

    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private ThemePreferenceManager() {
    }

    public static void applySavedNightMode(Context context) {
        boolean darkModeEnabled = getPrefs(context).getBoolean(KEY_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean isDarkModeEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, true);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
