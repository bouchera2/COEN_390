package com.coen390.team6;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AlertHistoryManager — Handles saving and retrieving fatigue alerts from Firestore.
 *
 * Firestore structure:
 * alerts/{alertId}
 *   - driverId: String (user UID)
 *   - heartRate: int
 *   - fatigueLevel: String ("HIGH", "CRITICAL")
 *   - driveTimeMinutes: long
 *   - latitude: double
 *   - longitude: double
 *   - timestamp: Timestamp
 *   - dismissed: boolean
 *   - dismissReason: String ("rest_stop", "call_dispatch", "false_alarm")
 *   - acknowledged: boolean (for fleet manager)
 */
public class AlertHistoryManager {

    private static final String TAG = "AlertHistory";
    private static final String COLLECTION_ALERTS = "alerts";

    private final FirebaseFirestore db;
    private final String driverId;

    public AlertHistoryManager() {
        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
    }

    /**
     * Save a new fatigue alert to Firestore.
     * Call this when fatigue threshold is exceeded.
     */
    public void saveAlert(int heartRate, String fatigueLevel, long driveTimeMinutes,
                          double latitude, double longitude) {
        if (driverId.isEmpty()) {
            Log.w(TAG, "No user logged in, cannot save alert");
            return;
        }

        Map<String, Object> alert = new HashMap<>();
        alert.put("driverId", driverId);
        alert.put("heartRate", heartRate);
        alert.put("fatigueLevel", fatigueLevel);
        alert.put("driveTimeMinutes", driveTimeMinutes);
        alert.put("latitude", latitude);
        alert.put("longitude", longitude);
        alert.put("timestamp", com.google.firebase.Timestamp.now());
        alert.put("dismissed", false);
        alert.put("dismissReason", "");
        alert.put("acknowledged", false);
        alert.put("batteryBars", BatteryEstimator.getBatteryBars(driveTimeMinutes));

        db.collection(COLLECTION_ALERTS)
                .add(alert)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Alert saved: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save alert", e);
                });
    }

    /**
     * Update an alert when the driver dismisses it.
     */
    public void dismissAlert(String alertId, String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("dismissed", true);
        updates.put("dismissReason", reason);

        db.collection(COLLECTION_ALERTS)
                .document(alertId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Alert dismissed: " + alertId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to dismiss alert", e));
    }

    /**
     * Get all alerts for the current driver, ordered by most recent first.
     */
    public void getAlertHistory(OnAlertHistoryLoadedListener listener) {
        if (driverId.isEmpty()) {
            listener.onLoaded(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_ALERTS)
                .whereEqualTo("driverId", driverId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> alerts = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> alert = new HashMap<>(doc.getData());
                        alert.put("alertId", doc.getId());
                        alerts.add(alert);
                    }
                    listener.onLoaded(alerts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load alert history", e);
                    listener.onLoaded(new ArrayList<>());
                });
    }

    /**
     * Get alerts for a specific driver (for fleet managers).
     */
    public void getAlertsForDriver(String targetDriverId, OnAlertHistoryLoadedListener listener) {
        db.collection(COLLECTION_ALERTS)
                .whereEqualTo("driverId", targetDriverId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> alerts = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> alert = new HashMap<>(doc.getData());
                        alert.put("alertId", doc.getId());
                        alerts.add(alert);
                    }
                    listener.onLoaded(alerts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load alerts for driver", e);
                    listener.onLoaded(new ArrayList<>());
                });
    }

    /**
     * Get today's alert count for the current driver.
     */
    public void getTodayAlertCount(OnAlertCountListener listener) {
        if (driverId.isEmpty()) {
            listener.onCount(0);
            return;
        }

        // Get start of today
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        com.google.firebase.Timestamp startOfDay = new com.google.firebase.Timestamp(cal.getTime());

        db.collection(COLLECTION_ALERTS)
                .whereEqualTo("driverId", driverId)
                .whereGreaterThan("timestamp", startOfDay)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onCount(querySnapshot.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get today's alert count", e);
                    listener.onCount(0);
                });
    }

    // Callback interfaces
    public interface OnAlertHistoryLoadedListener {
        void onLoaded(List<Map<String, Object>> alerts);
    }

    public interface OnAlertCountListener {
        void onCount(int count);
    }
}
