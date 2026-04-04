package com.coen390.team6;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class FirestoreRepository {
    private static final String TAG        = "FirestoreRepo";
    private static final String COLLECTION = "sensor_readings";
    private static final long SAVE_INTERVAL_MS = 15_000;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private long lastSaveTime = 0;

    public FirestoreRepository() {
        this.db   = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

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

        // GSR + Driver State
        reading.put("gsrFiltered",   data.getGsrFiltered());
        reading.put("gsrBaseline",   data.getGsrBaseline());
        reading.put("driverState",   data.getDriverState());
        reading.put("possibleCrash", data.isPossibleCrash());

        db.collection(COLLECTION)
                .add(reading)
                .addOnSuccessListener(ref ->
                        Log.d(TAG, "Saved: bpm=" + bpm
                                + " state=" + data.getDriverState()
                                + " crash=" + data.isPossibleCrash()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Firestore write failed: " + e.getMessage()));
    }
}