package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

public final class NavigationSessionPreferences {
    private static final String PREFS_NAME = "navigation_session_prefs";
    private static final String KEY_DESTINATION = "destination";

    private NavigationSessionPreferences() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveDestination(Context context, String destination) {
        getPrefs(context).edit()
                .putString(KEY_DESTINATION, destination)
                .apply();
    }

    public static String getDestination(Context context) {
        return getPrefs(context).getString(KEY_DESTINATION, "");
    }

    public static void clear(Context context) {
        getPrefs(context).edit()
                .remove(KEY_DESTINATION)
                .apply();
    }
}
