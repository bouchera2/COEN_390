package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

public final class DrivingSessionPreferences {
    private static final String PREFS_NAME = "driving_session_prefs";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_STARTED_AT_MS = "started_at_ms";

    private DrivingSessionPreferences() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void start(Context context, long startedAtMs) {
        getPrefs(context).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putLong(KEY_STARTED_AT_MS, startedAtMs)
                .apply();
    }

    public static void stop(Context context) {
        getPrefs(context).edit()
                .putBoolean(KEY_ACTIVE, false)
                .remove(KEY_STARTED_AT_MS)
                .apply();
    }

    public static boolean isActive(Context context) {
        return getPrefs(context).getBoolean(KEY_ACTIVE, false);
    }

    public static long getStartedAtMs(Context context) {
        return getPrefs(context).getLong(KEY_STARTED_AT_MS, 0L);
    }
}
