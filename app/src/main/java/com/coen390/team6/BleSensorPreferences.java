package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

public final class BleSensorPreferences {
    private static final String PREFS_NAME = "ble_sensor_prefs";
    private static final String KEY_CONNECTED = "connected";
    private static final String KEY_FINGER_DETECTED = "finger_detected";
    private static final String KEY_BPM = "bpm";
    private static final String KEY_AVG_BPM = "avg_bpm";
    private static final String KEY_SUDDEN_MOVEMENT = "sudden_movement";
    private static final String KEY_LAST_PAYLOAD = "last_payload";

    private BleSensorPreferences() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setConnected(Context context, boolean connected) {
        getPrefs(context).edit().putBoolean(KEY_CONNECTED, connected).apply();
    }

    public static boolean isConnected(Context context) {
        return getPrefs(context).getBoolean(KEY_CONNECTED, false);
    }

    public static void saveSensorData(Context context, BleSensorData data) {
        getPrefs(context).edit()
                .putBoolean(KEY_FINGER_DETECTED, data.isFingerDetected())
                .putFloat(KEY_BPM, data.getBpm())
                .putInt(KEY_AVG_BPM, data.getAvgBpm())
                .putBoolean(KEY_SUDDEN_MOVEMENT, data.isSuddenMovement())
                .putString(KEY_LAST_PAYLOAD, data.getRawPayload())
                .apply();
    }

    public static boolean isFingerDetected(Context context) {
        return getPrefs(context).getBoolean(KEY_FINGER_DETECTED, false);
    }

    public static float getBpm(Context context) {
        return getPrefs(context).getFloat(KEY_BPM, 0f);
    }

    public static int getAvgBpm(Context context) {
        return getPrefs(context).getInt(KEY_AVG_BPM, 0);
    }

    public static boolean hasSuddenMovement(Context context) {
        return getPrefs(context).getBoolean(KEY_SUDDEN_MOVEMENT, false);
    }

    public static String getLastPayload(Context context) {
        return getPrefs(context).getString(KEY_LAST_PAYLOAD, "");
    }
}
