package com.coen390.team6;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all Firestore writes for sensor data.
 * Call saveSensorReading() from MainActivity's onCharacteristicChanged.
 */
public final class FirestoreRepository {
    private static final String TAG = "FirestoreRepo";
    private static final String COLLECTION = "sensor_readings";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // Throttle — save at most once every 15 seconds
    private static final long SAVE_INTERVAL_MS = 15_000;
    private long lastSaveTime = 0;

    public FirestoreRepository() {
        this.db   = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Saves a sensor reading to Firestore if the user is logged in,
     * finger is detected, BPM is valid, and throttle interval has passed.
     */
    public void saveSensorReading(BleSensorData data) {
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No user logged in, skipping save");
            return;
        }
        if (!data.isFingerDetected()) return;

        int bpm = data.getAvgBpm() > 0 ? data.getAvgBpm() : Math.round(data.getBpm());
        if (bpm <= 0) return;

        long now = System.currentTimeMillis();
        if (now - lastSaveTime < SAVE_INTERVAL_MS) return;
        lastSaveTime = now;

        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> reading = new HashMap<>();
        // Identity
        reading.put("userId",    userId);
        reading.put("timestamp", now);

        // Heart rate
        reading.put("bpm",    bpm);
        reading.put("bpmRaw", data.getBpm());

        // IMU
        reading.put("suddenMovement", data.isSuddenMovement());
        reading.put("motionAccel",    data.getMotionAccel());
        reading.put("gyroMag",        data.getGyroMag());
        reading.put("ax",             data.getAx());
        reading.put("ay",             data.getAy());
        reading.put("az",             data.getAz());

        db.collection(COLLECTION)
                .add(reading)
                .addOnSuccessListener(ref ->
                        Log.d(TAG, "Saved reading: bpm=" + bpm + " sudden=" + data.isSuddenMovement()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Firestore write failed: " + e.getMessage()));
    }
}