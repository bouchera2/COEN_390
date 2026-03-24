package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

public final class DriverProfilePreferences {
    private static final String PREFS_NAME = "driver_profile_prefs";
    private static final String KEY_RESTING_HR = "resting_hr";
    private static final String KEY_NORMAL_HR_LOW = "normal_hr_low";
    private static final String KEY_NORMAL_HR_HIGH = "normal_hr_high";
    private static final String KEY_FATIGUE_HR_THRESHOLD = "fatigue_hr_threshold";
    private static final String KEY_STRESS_HR_THRESHOLD = "stress_hr_threshold";

    private DriverProfilePreferences() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveThresholds(
            Context context,
            int restingHeartRate,
            int normalHrLow,
            int normalHrHigh,
            int fatigueHrThreshold,
            int stressHrThreshold
    ) {
        getPrefs(context).edit()
                .putInt(KEY_RESTING_HR, restingHeartRate)
                .putInt(KEY_NORMAL_HR_LOW, normalHrLow)
                .putInt(KEY_NORMAL_HR_HIGH, normalHrHigh)
                .putInt(KEY_FATIGUE_HR_THRESHOLD, fatigueHrThreshold)
                .putInt(KEY_STRESS_HR_THRESHOLD, stressHrThreshold)
                .apply();
    }

    public static int getRestingHeartRate(Context context) {
        return getPrefs(context).getInt(KEY_RESTING_HR, 70);
    }

    public static int getNormalHrLow(Context context) {
        return getPrefs(context).getInt(KEY_NORMAL_HR_LOW, 90);
    }

    public static int getNormalHrHigh(Context context) {
        return getPrefs(context).getInt(KEY_NORMAL_HR_HIGH, 120);
    }

    public static int getFatigueHrThreshold(Context context) {
        return getPrefs(context).getInt(KEY_FATIGUE_HR_THRESHOLD, 80);
    }

    public static int getStressHrThreshold(Context context) {
        return getPrefs(context).getInt(KEY_STRESS_HR_THRESHOLD, 140);
    }
}
