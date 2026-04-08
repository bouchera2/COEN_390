package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AlertHistoryPreferences {

    private static final String PREFS_NAME = "alert_history_prefs";
    private static final String KEY_ACTIVE_ALERT = "active_alert";
    private static final String KEY_HISTORY_ALERTS = "history_alerts";
    private static final int MAX_HISTORY_ITEMS = 20;

    private AlertHistoryPreferences() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveActiveAlert(
            Context context,
            String title,
            String message,
            int score,
            int heartRate,
            long timestampMs
    ) {
        JSONObject json = new JSONObject();
        try {
            json.put("title", title);
            json.put("message", message);
            json.put("score", score);
            json.put("heartRate", heartRate);
            json.put("timestampMs", timestampMs);
        } catch (JSONException ignored) {
        }

        getPrefs(context).edit()
                .putString(KEY_ACTIVE_ALERT, json.toString())
                .apply();
    }

    public static JSONObject getActiveAlert(Context context) {
        String raw = getPrefs(context).getString(KEY_ACTIVE_ALERT, null);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    public static void clearActiveAlert(Context context) {
        getPrefs(context).edit().remove(KEY_ACTIVE_ALERT).apply();
    }

    public static void archiveActiveAlert(Context context, String status, String resolutionMessage) {
        JSONObject activeAlert = getActiveAlert(context);
        if (activeAlert == null) {
            return;
        }

        try {
            activeAlert.put("status", status);
            activeAlert.put("resolutionMessage", resolutionMessage);
            activeAlert.put("resolvedAtMs", System.currentTimeMillis());
        } catch (JSONException ignored) {
        }

        JSONArray history = getHistoryAlerts(context);
        JSONArray updated = new JSONArray();
        updated.put(activeAlert);
        for (int i = 0; i < history.length() && i < MAX_HISTORY_ITEMS - 1; i++) {
            updated.put(history.optJSONObject(i));
        }

        getPrefs(context).edit()
                .putString(KEY_HISTORY_ALERTS, updated.toString())
                .remove(KEY_ACTIVE_ALERT)
                .apply();
    }

    public static JSONArray getHistoryAlerts(Context context) {
        String raw = getPrefs(context).getString(KEY_HISTORY_ALERTS, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
}
