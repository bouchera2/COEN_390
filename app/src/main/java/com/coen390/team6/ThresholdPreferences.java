package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

public final class ThresholdPreferences {
    private static final String PREFS_NAME = "threshold_prefs";
    public static final String DEMO_MODE_NORMAL = "NORMAL";
    public static final String DEMO_MODE_DROWSY = "DROWSY";
    public static final String DEMO_MODE_STRESSED = "STRESSED";

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

    private static RuntimeThresholdOverride runtimeOverride;
    private static String activeDemoMode = DEMO_MODE_NORMAL;

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
        if (runtimeOverride != null) return runtimeOverride.bpmDrowsyMax;
        return getPrefs(context).getInt(KEY_BPM_DROWSY_MAX, DEFAULT_BPM_DROWSY_MAX);
    }

    public static int getBpmStressedMin(Context context) {
        if (runtimeOverride != null) return runtimeOverride.bpmStressedMin;
        return getPrefs(context).getInt(KEY_BPM_STRESSED_MIN, DEFAULT_BPM_STRESSED_MIN);
    }

    public static float getGsrDrowsyMax(Context context) {
        if (runtimeOverride != null) return runtimeOverride.gsrDrowsyMax;
        return getPrefs(context).getFloat(KEY_GSR_DROWSY_MAX, DEFAULT_GSR_DROWSY_MAX);
    }

    public static float getGsrStressedMin(Context context) {
        if (runtimeOverride != null) return runtimeOverride.gsrStressedMin;
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
        clearRuntimeOverride();
    }

    public static void applyDemoMode(String demoMode,
                                     int avgBpm,
                                     float gsrFiltered,
                                     float gsrBaseline) {
        if (DEMO_MODE_NORMAL.equals(demoMode)) {
            clearRuntimeOverride();
            return;
        }

        int currentBpm = avgBpm > 0 ? avgBpm : DEFAULT_BPM_STRESSED_MIN;
        float currentRatio = gsrBaseline > 0.01f ? (gsrFiltered / gsrBaseline) : 1.0f;

        if (DEMO_MODE_DROWSY.equals(demoMode)) {
            int bpmDrowsyMax = clampInt(currentBpm + 8, 45, 140);
            float gsrDrowsyMax = clampFloat(currentRatio + 0.12f, 0.55f, 1.60f);
            int bpmStressedMin = clampInt(Math.max(DEFAULT_BPM_STRESSED_MIN, bpmDrowsyMax + 15), 70, 170);
            float gsrStressedMin = clampFloat(Math.max(DEFAULT_GSR_STRESSED_MIN, gsrDrowsyMax + 0.10f), 0.70f, 1.80f);
            runtimeOverride = new RuntimeThresholdOverride(
                    bpmDrowsyMax,
                    bpmStressedMin,
                    gsrDrowsyMax,
                    gsrStressedMin
            );
            activeDemoMode = DEMO_MODE_DROWSY;
            return;
        }

        if (DEMO_MODE_STRESSED.equals(demoMode)) {
            int bpmStressedMin = clampInt(currentBpm - 8, 35, 120);
            float gsrStressedMin = clampFloat(currentRatio - 0.12f, 0.45f, 1.30f);
            int bpmDrowsyMax = clampInt(Math.min(DEFAULT_BPM_DROWSY_MAX, bpmStressedMin - 15), 30, 90);
            float gsrDrowsyMax = clampFloat(Math.min(DEFAULT_GSR_DROWSY_MAX, gsrStressedMin - 0.10f), 0.30f, 1.10f);
            runtimeOverride = new RuntimeThresholdOverride(
                    bpmDrowsyMax,
                    bpmStressedMin,
                    gsrDrowsyMax,
                    gsrStressedMin
            );
            activeDemoMode = DEMO_MODE_STRESSED;
        }
    }

    public static String getActiveDemoMode() {
        return activeDemoMode;
    }

    public static boolean hasRuntimeOverride() {
        return runtimeOverride != null;
    }

    public static void clearRuntimeOverride() {
        runtimeOverride = null;
        activeDemoMode = DEMO_MODE_NORMAL;
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

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class RuntimeThresholdOverride {
        private final int bpmDrowsyMax;
        private final int bpmStressedMin;
        private final float gsrDrowsyMax;
        private final float gsrStressedMin;

        private RuntimeThresholdOverride(int bpmDrowsyMax,
                                         int bpmStressedMin,
                                         float gsrDrowsyMax,
                                         float gsrStressedMin) {
            this.bpmDrowsyMax = bpmDrowsyMax;
            this.bpmStressedMin = bpmStressedMin;
            this.gsrDrowsyMax = gsrDrowsyMax;
            this.gsrStressedMin = gsrStressedMin;
        }
    }
}
