package com.coen390.team6;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemePreferenceManager {

    private ThemePreferenceManager() {
    }

    public static void applySavedNightMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
