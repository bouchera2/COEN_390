package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

public final class BleSensorPreferences {
    private static final String PREFS_NAME = "ble_sensor_prefs";

    private static final String KEY_CONNECTED       = "connected";
    private static final String KEY_FINGER_DETECTED = "finger_detected";
    private static final String KEY_BPM             = "bpm";
    private static final String KEY_AVG_BPM         = "avg_bpm";
    private static final String KEY_SUDDEN_MOVEMENT = "sudden_movement";
    private static final String KEY_MOTION_ACCEL    = "motion_accel";
    private static final String KEY_GYRO_MAG        = "gyro_mag";
    private static final String KEY_AX              = "ax";
    private static final String KEY_AY              = "ay";
    private static final String KEY_AZ              = "az";
    private static final String KEY_GSR_FILTERED    = "gsr_filtered";
    private static final String KEY_GSR_BASELINE    = "gsr_baseline";
    private static final String KEY_DRIVER_STATE    = "driver_state";
    private static final String KEY_POSSIBLE_CRASH  = "possible_crash";
    private static final String KEY_LAST_PAYLOAD    = "last_payload";

    private BleSensorPreferences() {}

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
                .putFloat(KEY_BPM,               data.getBpm())
                .putInt(KEY_AVG_BPM,             data.getAvgBpm())
                .putBoolean(KEY_SUDDEN_MOVEMENT, data.isSuddenMovement())
                .putFloat(KEY_MOTION_ACCEL,      data.getMotionAccel())
                .putFloat(KEY_GYRO_MAG,          data.getGyroMag())
                .putFloat(KEY_AX,                data.getAx())
                .putFloat(KEY_AY,                data.getAy())
                .putFloat(KEY_AZ,                data.getAz())
                .putFloat(KEY_GSR_FILTERED,      data.getGsrFiltered())
                .putFloat(KEY_GSR_BASELINE,      data.getGsrBaseline())
                .putString(KEY_DRIVER_STATE,     data.getDriverState())
                .putBoolean(KEY_POSSIBLE_CRASH,  data.isPossibleCrash())
                .putString(KEY_LAST_PAYLOAD,     data.getRawPayload())
                .apply();
    }

    // Getters existants
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
    public static float getMotionAccel(Context context) {
        return getPrefs(context).getFloat(KEY_MOTION_ACCEL, 0f);
    }
    public static float getGyroMag(Context context) {
        return getPrefs(context).getFloat(KEY_GYRO_MAG, 0f);
    }
    public static float getAx(Context context) {
        return getPrefs(context).getFloat(KEY_AX, 0f);
    }
    public static float getAy(Context context) {
        return getPrefs(context).getFloat(KEY_AY, 0f);
    }
    public static float getAz(Context context) {
        return getPrefs(context).getFloat(KEY_AZ, 0f);
    }
    public static String getLastPayload(Context context) {
        return getPrefs(context).getString(KEY_LAST_PAYLOAD, "");
    }

    // Nouveaux getters
    public static float getGsrFiltered(Context context) {
        return getPrefs(context).getFloat(KEY_GSR_FILTERED, 0f);
    }
    public static float getGsrBaseline(Context context) {
        return getPrefs(context).getFloat(KEY_GSR_BASELINE, 0f);
    }
    public static String getDriverState(Context context) {
        return getPrefs(context).getString(KEY_DRIVER_STATE, "UNKNOWN");
    }
    public static boolean isPossibleCrash(Context context) {
        return getPrefs(context).getBoolean(KEY_POSSIBLE_CRASH, false);
    }
}