package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

public final class ThresholdPreferences {
    private static final String PREFS_NAME = "threshold_prefs";

    // BPM thresholds (matches Arduino defaults)
    private static final String KEY_BPM_DROWSY_MAX   = "bpm_drowsy_max";   // beatAvg < X → drowsy
    private static final String KEY_BPM_STRESSED_MIN = "bpm_stressed_min"; // beatAvg > X → stressed

    // GSR ratio thresholds
    private static final String KEY_GSR_DROWSY_MAX   = "gsr_drowsy_max";   // ratio < X → drowsy
    private static final String KEY_GSR_STRESSED_MIN = "gsr_stressed_min"; // ratio > X → stressed

    // Arduino defaults
    public static final int   DEFAULT_BPM_DROWSY_MAX   = 60;
    public static final int   DEFAULT_BPM_STRESSED_MIN = 95;
    public static final float DEFAULT_GSR_DROWSY_MAX   = 0.90f;
    public static final float DEFAULT_GSR_STRESSED_MIN = 1.00f;

    private ThresholdPreferences() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

// setters
    public static void setBpmDrowsyMax(Context context, int value) {
        getPrefs(context).edit().putInt(KEY_BPM_DROWSY_MAX, value).apply();
    }

    public static void setBpmStressedMin(Context context, int value) {
        getPrefs(context).edit().putInt(KEY_BPM_STRESSED_MIN, value).apply();
    }

    public static void setGsrDrowsyMax(Context context, float value) {
        getPrefs(context).edit().putFloat(KEY_GSR_DROWSY_MAX, value).apply();
    }

    public static void setGsrStressedMin(Context context, float value) {
        getPrefs(context).edit().putFloat(KEY_GSR_STRESSED_MIN, value).apply();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public static int getBpmDrowsyMax(Context context) {
        return getPrefs(context).getInt(KEY_BPM_DROWSY_MAX, DEFAULT_BPM_DROWSY_MAX);
    }

    public static int getBpmStressedMin(Context context) {
        return getPrefs(context).getInt(KEY_BPM_STRESSED_MIN, DEFAULT_BPM_STRESSED_MIN);
    }

    public static float getGsrDrowsyMax(Context context) {
        return getPrefs(context).getFloat(KEY_GSR_DROWSY_MAX, DEFAULT_GSR_DROWSY_MAX);
    }

    public static float getGsrStressedMin(Context context) {
        return getPrefs(context).getFloat(KEY_GSR_STRESSED_MIN, DEFAULT_GSR_STRESSED_MIN);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    public static void resetToDefaults(Context context) {
        getPrefs(context).edit()
                .putInt(KEY_BPM_DROWSY_MAX,   DEFAULT_BPM_DROWSY_MAX)
                .putInt(KEY_BPM_STRESSED_MIN, DEFAULT_BPM_STRESSED_MIN)
                .putFloat(KEY_GSR_DROWSY_MAX,   DEFAULT_GSR_DROWSY_MAX)
                .putFloat(KEY_GSR_STRESSED_MIN, DEFAULT_GSR_STRESSED_MIN)
                .apply();
    }

    // ── Classification helper (mirrors Arduino classifyState logic) ───────────

    /**
     * Re-classify driver state on the Android side using stored thresholds.
     * Call this after receiving BLE data to override the Arduino's embedded state
     * when you want to demo threshold changes live.
     *
     * @param avgBpm      averaged BPM from sensor
     * @param gsrFiltered filtered GSR voltage
     * @param gsrBaseline GSR baseline voltage
     * @param baselineReady whether the 10-second calibration has finished
     * @return "DROWSY", "STRESSED", "NORMAL", "CALIBRATING", or "UNKNOWN"
     */
    public static String classifyDriverState(Context context,
                                             int avgBpm,
                                             float gsrFiltered,
                                             float gsrBaseline,
                                             boolean baselineReady) {
        if (!baselineReady) return "CALIBRATING";
        if (gsrBaseline < 0.01f) return "UNKNOWN";

        float gsrRatio = gsrFiltered / gsrBaseline;

        int   bpmDrowsyMax   = getBpmDrowsyMax(context);
        int   bpmStressedMin = getBpmStressedMin(context);
        float gsrDrowsyMax   = getGsrDrowsyMax(context);
        float gsrStressedMin = getGsrStressedMin(context);

        if (avgBpm < bpmDrowsyMax && gsrRatio < gsrDrowsyMax) {
            return "DROWSY";
        } else if (avgBpm > bpmStressedMin && gsrRatio > gsrStressedMin) {
            return "STRESSED";
        } else {
            return "NORMAL";
        }
    }
}