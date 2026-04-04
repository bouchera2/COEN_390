package com.coen390.team6;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class AlertRepository {
    private static final String PREFS_NAME = "driver_alerts_prefs";
    private static final String KEY_ALERTS = "alerts_json";
    private static final int MAX_ALERTS = 50;

    private AlertRepository() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static List<AlertItem> getAllAlerts(Context context) {
        String raw = getPrefs(context).getString(KEY_ALERTS, "[]");
        List<AlertItem> alerts = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                alerts.add(AlertItem.fromJson(object));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }

        Collections.sort(alerts, Comparator.comparingLong(AlertItem::getTimestamp).reversed());
        return alerts;
    }

    public static List<AlertItem> getActiveAlerts(Context context) {
        List<AlertItem> activeAlerts = new ArrayList<>();
        for (AlertItem item : getAllAlerts(context)) {
            if (item.isActive()) {
                activeAlerts.add(item);
            }
        }
        return activeAlerts;
    }

    public static List<AlertItem> getResolvedAlerts(Context context) {
        List<AlertItem> resolvedAlerts = new ArrayList<>();
        for (AlertItem item : getAllAlerts(context)) {
            if (!item.isActive()) {
                resolvedAlerts.add(item);
            }
        }
        return resolvedAlerts;
    }

    public static boolean hasActiveAlert(Context context, String type) {
        for (AlertItem item : getAllAlerts(context)) {
            if (item.isActive() && type.equals(item.getType())) {
                return true;
            }
        }
        return false;
    }

    public static void addAlert(Context context, AlertItem alertItem) {
        List<AlertItem> alerts = getAllAlerts(context);
        alerts.add(alertItem);
        persist(context, alerts);
    }

    public static void resolveAlert(Context context, String id, String resolutionNote) {
        List<AlertItem> alerts = getAllAlerts(context);
        for (int i = 0; i < alerts.size(); i++) {
            AlertItem item = alerts.get(i);
            if (item.getId().equals(id) && item.isActive()) {
                alerts.set(i, item.withResolution(resolutionNote));
                break;
            }
        }
        persist(context, alerts);
    }

    public static void resolveAlertsByType(Context context, String type, String resolutionNote) {
        List<AlertItem> alerts = getAllAlerts(context);
        boolean changed = false;
        for (int i = 0; i < alerts.size(); i++) {
            AlertItem item = alerts.get(i);
            if (item.isActive() && type.equals(item.getType())) {
                alerts.set(i, item.withResolution(resolutionNote));
                changed = true;
            }
        }

        if (changed) {
            persist(context, alerts);
        }
    }

    private static void persist(Context context, List<AlertItem> alerts) {
        Collections.sort(alerts, Comparator.comparingLong(AlertItem::getTimestamp).reversed());
        if (alerts.size() > MAX_ALERTS) {
            alerts = new ArrayList<>(alerts.subList(0, MAX_ALERTS));
        }

        JSONArray array = new JSONArray();
        for (AlertItem item : alerts) {
            try {
                array.put(item.toJson());
            } catch (JSONException ignored) {
                // Skip malformed entries rather than losing the full alert history.
            }
        }

        getPrefs(context).edit().putString(KEY_ALERTS, array.toString()).apply();
    }
}
