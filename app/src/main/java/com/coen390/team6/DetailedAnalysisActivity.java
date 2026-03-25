package com.coen390.team6;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailedAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "DetailedAnalysis";

    // Live section
    private TextView tvLiveBpm, tvLiveFinger, tvLiveSudden,
            tvLiveMotionAccel, tvLiveGyro, tvLiveAx, tvLiveAy, tvLiveAz;

    // Last saved section
    private TextView tvLastBpm, tvLastSudden, tvLastMotionAccel,
            tvLastGyro, tvLastAx, tvLastAy, tvLastAz, tvLastTimestamp;

    // Stats section
    private TextView tvAvgBpm, tvMinBpm, tvMaxBpm, tvReadingCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_analysis);

        bindViews();
        loadLiveData();
        loadFirestoreData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLiveData();
    }

    private void bindViews() {
        tvLiveBpm         = findViewById(R.id.tvLiveBpm);
        tvLiveFinger      = findViewById(R.id.tvLiveFinger);
        tvLiveSudden      = findViewById(R.id.tvLiveSudden);
        tvLiveMotionAccel = findViewById(R.id.tvLiveMotionAccel);
        tvLiveGyro        = findViewById(R.id.tvLiveGyro);
        tvLiveAx          = findViewById(R.id.tvLiveAx);
        tvLiveAy          = findViewById(R.id.tvLiveAy);
        tvLiveAz          = findViewById(R.id.tvLiveAz);

        tvLastBpm         = findViewById(R.id.tvLastBpm);
        tvLastSudden      = findViewById(R.id.tvLastSudden);
        tvLastMotionAccel = findViewById(R.id.tvLastMotionAccel);
        tvLastGyro        = findViewById(R.id.tvLastGyro);
        tvLastAx          = findViewById(R.id.tvLastAx);
        tvLastAy          = findViewById(R.id.tvLastAy);
        tvLastAz          = findViewById(R.id.tvLastAz);
        tvLastTimestamp   = findViewById(R.id.tvLastTimestamp);

        tvAvgBpm       = findViewById(R.id.tvAvgBpm);
        tvMinBpm       = findViewById(R.id.tvMinBpm);
        tvMaxBpm       = findViewById(R.id.tvMaxBpm);
        tvReadingCount = findViewById(R.id.tvReadingCount);
    }

    private void loadLiveData() {
        boolean finger = BleSensorPreferences.isFingerDetected(this);
        int avgBpm     = BleSensorPreferences.getAvgBpm(this);
        float bpm      = BleSensorPreferences.getBpm(this);
        boolean sudden = BleSensorPreferences.hasSuddenMovement(this);
        float accel    = BleSensorPreferences.getMotionAccel(this);
        float gyro     = BleSensorPreferences.getGyroMag(this);
        float ax       = BleSensorPreferences.getAx(this);
        float ay       = BleSensorPreferences.getAy(this);
        float az       = BleSensorPreferences.getAz(this);

        int displayBpm = avgBpm > 0 ? avgBpm : Math.round(bpm);

        tvLiveBpm.setText(finger && displayBpm > 0 ? displayBpm + " BPM" : "--");
        tvLiveFinger.setText(finger ? "✓ Detected" : "✗ Not detected");
        tvLiveSudden.setText(sudden ? "⚠ YES" : "No");
        tvLiveMotionAccel.setText(String.format(Locale.getDefault(), "%.2f m/s²", accel));
        tvLiveGyro.setText(String.format(Locale.getDefault(), "%.2f rad/s", gyro));
        tvLiveAx.setText(String.format(Locale.getDefault(), "%.2f", ax));
        tvLiveAy.setText(String.format(Locale.getDefault(), "%.2f", ay));
        tvLiveAz.setText(String.format(Locale.getDefault(), "%.2f", az));
    }

    private void loadFirestoreData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("sensor_readings")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        tvLastBpm.setText("No data yet");
                        tvReadingCount.setText("0 readings");
                        return;
                    }
                    populateLastReading((QueryDocumentSnapshot) query.getDocuments().get(0));
                    computeStats(query);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firestore error: " + e.getMessage()));
    }

    private void populateLastReading(QueryDocumentSnapshot doc) {
        Long bpm       = doc.getLong("bpm");
        Boolean sudden = doc.getBoolean("suddenMovement");
        Double accel   = doc.getDouble("motionAccel");
        Double gyro    = doc.getDouble("gyroMag");
        Double ax      = doc.getDouble("ax");
        Double ay      = doc.getDouble("ay");
        Double az      = doc.getDouble("az");
        Long ts        = doc.getLong("timestamp");

        tvLastBpm.setText(bpm != null ? bpm + " BPM" : "--");
        tvLastSudden.setText(Boolean.TRUE.equals(sudden) ? "⚠ YES" : "No");
        tvLastMotionAccel.setText(accel != null
                ? String.format(Locale.getDefault(), "%.2f m/s²", accel) : "--");
        tvLastGyro.setText(gyro != null
                ? String.format(Locale.getDefault(), "%.2f rad/s", gyro) : "--");
        tvLastAx.setText(ax != null ? String.format(Locale.getDefault(), "%.2f", ax) : "--");
        tvLastAy.setText(ay != null ? String.format(Locale.getDefault(), "%.2f", ay) : "--");
        tvLastAz.setText(az != null ? String.format(Locale.getDefault(), "%.2f", az) : "--");

        if (ts != null) {
            tvLastTimestamp.setText(new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
                    .format(new Date(ts)));
        }
    }

    private void computeStats(QuerySnapshot query) {
        int count = 0, sum = 0, min = Integer.MAX_VALUE, max = 0;
        for (DocumentSnapshot doc : query.getDocuments()) {
            Long bpm = doc.getLong("bpm");
            if (bpm == null || bpm <= 0) continue;
            int b = bpm.intValue();
            sum += b; count++;
            if (b < min) min = b;
            if (b > max) max = b;
        }
        tvReadingCount.setText(count + " readings");
        tvAvgBpm.setText(count > 0 ? sum / count + " BPM" : "--");
        tvMinBpm.setText(count > 0 ? min + " BPM" : "--");
        tvMaxBpm.setText(count > 0 ? max + " BPM" : "--");
    }
}