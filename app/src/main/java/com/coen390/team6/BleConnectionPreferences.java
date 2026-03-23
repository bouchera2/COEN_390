package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

public final class BleConnectionPreferences {
    private static final String PREFS_NAME = "ble_connection_prefs";
    private static final String KEY_CONNECTED = "connected";

    private BleConnectionPreferences() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isConnected(Context context) {
        return getPrefs(context).getBoolean(KEY_CONNECTED, false);
    }

    public static void setConnected(Context context, boolean connected) {
        getPrefs(context).edit().putBoolean(KEY_CONNECTED, connected).apply();
    }
}
