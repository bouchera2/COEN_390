package com.coen390.team6;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailedAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "DetailedAnalysis";

    // ── Driver State card
    private TextView tvDriverState;
    private TextView tvDriverStateDesc;

    // ── Fatigue Score card
    private TextView tvFatigueScore;
    private TextView tvFatigueLabel;
    private android.widget.ProgressBar progressFatigue;

    // ── Vitals card
    private TextView tvLiveBpm;
    private TextView tvLiveFinger;
    private TextView tvLiveSudden;
    private TextView tvLiveCrash;

    // ── Fatigue chart (canvas view)
    private FatigueChartView fatigueChartView;

    // ── Stats
    private TextView tvAvgBpm;
    private TextView tvMinBpm;
    private TextView tvMaxBpm;
    private TextView tvReadingCount;
    private TextView tvLastTimestamp;

    //  Live fatigue history (in-memory for current session)
    private final List<Float> liveScores      = new ArrayList<>();
    private final List<Long>  liveTimestamps  = new ArrayList<>();
    private static final int  MAX_LIVE_POINTS = 30;

    // Auto-refresh
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            loadLiveData();
            refreshHandler.postDelayed(this, 1000);
        }
    };

    public static FatigueSnapshot getFatigueSnapshot(Context context) {
        boolean connected = BleSensorPreferences.isConnected(context);
        boolean fingerDetected = BleSensorPreferences.isFingerDetected(context);
        int avgBpm = BleSensorPreferences.getAvgBpm(context);
        float bpm = BleSensorPreferences.getBpm(context);
        float gsrFiltered = BleSensorPreferences.getGsrFiltered(context);
        float gsrBaseline = BleSensorPreferences.getGsrBaseline(context);

        int displayBpm = avgBpm > 0 ? avgBpm : Math.round(bpm);
        if (!connected || !fingerDetected || displayBpm <= 0) {
            return waitingSnapshot();
        }

        String state = ThresholdPreferences.classifyDriverState(
                context,
                displayBpm,
                gsrFiltered,
                gsrBaseline,
                gsrBaseline > 0.01f
        );
        BleSensorPreferences.setDriverState(context, state);
        return snapshotForDriverState(state);
    }

    public static float computeFatigueScore(int bpm, float gsrFiltered, float gsrBaseline) {
        if (bpm <= 0) {
            return 0f;
        }

        float bpmScore;
        if (bpm <= 50) {
            bpmScore = 100f;
        } else if (bpm < 70) {
            bpmScore = (70f - bpm) * 5f;
        } else {
            bpmScore = 0f;
        }

        float gsrScore = 0f;
        if (gsrBaseline > 0.01f) {
            float gsrRatio = gsrFiltered / gsrBaseline;
            if (gsrRatio <= 0.7f) {
                gsrScore = 100f;
            } else if (gsrRatio < 1.0f) {
                gsrScore = (1.0f - gsrRatio) * 100f / 0.3f;
            }
        }

        float score = 0.6f * bpmScore + 0.4f * gsrScore;
        return Math.max(0f, Math.min(100f, score));
    }

    private static FatigueSnapshot waitingSnapshot() {
        return new FatigueSnapshot(
                0,
                "Waiting",
                "•",
                "Fatigue Risk: Waiting",
                "Waiting for enough sensor data to estimate fatigue.",
                Color.parseColor("#94A3B8")
        );
    }

    private static FatigueSnapshot snapshotForDriverState(String state) {
        if (state == null) {
            return waitingSnapshot();
        }

        switch (state) {
            case "DROWSY":
                return new FatigueSnapshot(
                        92,
                        "Drowsy",
                        "☹",
                        "Fatigue Risk: High",
                        "Low heart rate and GSR detected. Stop and rest.",
                        Color.parseColor("#EF4444")
                );
            case "STRESSED":
                return new FatigueSnapshot(
                        72,
                        "Stressed",
                        "⚠",
                        "Stress Risk: Elevated",
                        "Elevated heart rate and GSR detected. Check the driver condition.",
                        Color.parseColor("#F97316")
                );
            case "NORMAL":
                return new FatigueSnapshot(
                        18,
                        "Normal",
                        "🙂",
                        "Fatigue Risk: Low",
                        "Driver metrics are within a normal range.",
                        Color.parseColor("#22C55E")
                );
            case "CALIBRATING":
                return new FatigueSnapshot(
                        0,
                        "Calibrating",
                        "•",
                        "Fatigue Risk: Waiting",
                        "Calibrating GSR baseline. Please wait.",
                        Color.parseColor("#94A3B8")
                );
            default:
                return waitingSnapshot();
        }
    }

    // =========================================================================
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

    @Override protected void onResume() {
        super.onResume();
        loadLiveData();
        loadFirestoreData();
        refreshHandler.post(refreshRunnable);
    }

    @Override protected void onPause() {
        refreshHandler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    // ── Bind
    private void bindViews() {
        tvDriverState     = findViewById(R.id.tvDriverState);
        tvDriverStateDesc = findViewById(R.id.tvDriverStateDesc);

        tvFatigueScore    = findViewById(R.id.tvFatigueScore);
        tvFatigueLabel    = findViewById(R.id.tvFatigueLabel);
        progressFatigue   = findViewById(R.id.progressFatigue);

        tvLiveBpm         = findViewById(R.id.tvLiveBpm);
        tvLiveFinger      = findViewById(R.id.tvLiveFinger);
        tvLiveSudden      = findViewById(R.id.tvLiveSudden);
        tvLiveCrash       = findViewById(R.id.tvLiveCrash);

        fatigueChartView  = findViewById(R.id.fatigueChartView);

        tvAvgBpm          = findViewById(R.id.tvAvgBpm);
        tvMinBpm          = findViewById(R.id.tvMinBpm);
        tvMaxBpm          = findViewById(R.id.tvMaxBpm);
        tvReadingCount    = findViewById(R.id.tvReadingCount);
        tvLastTimestamp   = findViewById(R.id.tvLastTimestamp);
    }

    // ── Live data (SharedPrefs, refreshed every second)
    private void loadLiveData() {
        boolean finger     = BleSensorPreferences.isFingerDetected(this);
        int     avgBpm     = BleSensorPreferences.getAvgBpm(this);
        float   bpm        = BleSensorPreferences.getBpm(this);
        boolean sudden     = BleSensorPreferences.hasSuddenMovement(this);
        boolean crash      = BleSensorPreferences.isPossibleCrash(this);
        float   gsrF       = BleSensorPreferences.getGsrFiltered(this);
        float   gsrB       = BleSensorPreferences.getGsrBaseline(this);
        String  state      = BleSensorPreferences.getDriverState(this);

        int displayBpm = avgBpm > 0 ? avgBpm : Math.round(bpm);

        // ── Vitals
        tvLiveFinger.setText(finger ? "✓ Detected" : "✗ Not detected");
        tvLiveFinger.setTextColor(finger ? Color.parseColor("#22C55E") : Color.parseColor("#EF4444"));

        tvLiveBpm.setText(finger && displayBpm > 0 ? displayBpm + " BPM" : "--");

        tvLiveSudden.setText(sudden ? "⚠ YES" : "No");
        tvLiveSudden.setTextColor(sudden ? Color.parseColor("#EF4444") : Color.parseColor("#22C55E"));

        tvLiveCrash.setText(crash ? "⚠ INCIDENT DETECTED" : "No incident");
        tvLiveCrash.setTextColor(crash ? Color.parseColor("#EF4444") : Color.parseColor("#22C55E"));

        // ── Driver State
        applyDriverState(state);

        // ── Fatigue Score
        float score = computeFatigueScore(displayBpm, gsrF, gsrB);
        applyFatigueScore(score);

        // ── Add to live chart history
        if (finger && displayBpm > 0) {
            liveScores.add(score);
            liveTimestamps.add(System.currentTimeMillis());
            if (liveScores.size() > MAX_LIVE_POINTS) {
                liveScores.remove(0);
                liveTimestamps.remove(0);
            }
            fatigueChartView.setLiveData(liveScores, liveTimestamps);
        }
    }

    // ── Apply fatigue score to UI
    private void applyFatigueScore(float score) {
        int scoreInt = Math.round(score);
        tvFatigueScore.setText(String.valueOf(scoreInt));
        progressFatigue.setProgress(scoreInt);

        int color;
        String label;
        if (score < 30) {
            color = Color.parseColor("#22C55E"); // green
            label = "Normal";
        } else if (score < 60) {
            color = Color.parseColor("#F97316"); // orange
            label = "Fatigued";
        } else {
            color = Color.parseColor("#EF4444"); // red
            label = "Drowsy";
        }
        tvFatigueScore.setTextColor(color);
        tvFatigueLabel.setText(label);
        tvFatigueLabel.setTextColor(color);
        progressFatigue.getProgressDrawable().setColorFilter(
                color, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    // ── Apply driver state to UI
    private void applyDriverState(String state) {
        if (state == null) state = "UNKNOWN";
        tvDriverState.setText(state);
        String desc;
        int color;
        switch (state) {
            case "DROWSY":
                color = Color.parseColor("#EF4444");
                desc  = "Low heart rate & low GSR — driver may be falling asleep";
                break;
            case "STRESSED":
                color = Color.parseColor("#F97316");
                desc  = "Elevated heart rate & GSR — driver under stress";
                break;
            case "NORMAL":
                color = Color.parseColor("#22C55E");
                desc  = "Heart rate and GSR within normal range";
                break;
            case "CALIBRATING":
                color = Color.parseColor("#94A3B8");
                desc  = "Calibrating GSR baseline… please wait";
                break;
            default:
                color = Color.parseColor("#94A3B8");
                desc  = "No data";
        }
        tvDriverState.setTextColor(color);
        tvDriverStateDesc.setText(desc);
        tvDriverStateDesc.setTextColor(color);
    }

    // ── Firestore: last saved + stats + historical chart
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
                        tvReadingCount.setText("0 readings");
                        return;
                    }
                    populateLastReading((QueryDocumentSnapshot) query.getDocuments().get(0));
                    computeStats(query);
                    buildHistoricalChart(query);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firestore error: " + e.getMessage()));
    }

    private void populateLastReading(QueryDocumentSnapshot doc) {
        Long ts = doc.getLong("timestamp");
        if (ts != null) {
            tvLastTimestamp.setText("Last saved: " +
                    new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
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

    private void buildHistoricalChart(QuerySnapshot query) {
        List<Float> scores     = new ArrayList<>();
        List<Long>  timestamps = new ArrayList<>();

        // Docs are DESC — reverse to get chronological order
        List<DocumentSnapshot> docs = new ArrayList<>(query.getDocuments());
        Collections.reverse(docs);

        for (DocumentSnapshot doc : docs) {
            Long bpmL = doc.getLong("bpm");
            Double gsrF = doc.getDouble("gsrFiltered");
            Double gsrB = doc.getDouble("gsrBaseline");
            Long ts     = doc.getLong("timestamp");
            if (bpmL == null || ts == null) continue;

            float score = computeFatigueScore(
                    bpmL.intValue(),
                    gsrF != null ? gsrF.floatValue() : 0f,
                    gsrB != null ? gsrB.floatValue() : 0f
            );
            scores.add(score);
            timestamps.add(ts);
        }
        fatigueChartView.setHistoricalData(scores, timestamps);
    }

    public static final class FatigueSnapshot {
        private final int score;
        private final String label;
        private final String emoji;
        private final String dashboardTitle;
        private final String dashboardDescription;
        private final int accentColor;

        private FatigueSnapshot(
                int score,
                String label,
                String emoji,
                String dashboardTitle,
                String dashboardDescription,
                int accentColor
        ) {
            this.score = score;
            this.label = label;
            this.emoji = emoji;
            this.dashboardTitle = dashboardTitle;
            this.dashboardDescription = dashboardDescription;
            this.accentColor = accentColor;
        }

        public int getScore() {
            return score;
        }

        public String getLabel() {
            return label;
        }

        public String getEmoji() {
            return emoji;
        }

        public String getScoreText() {
            return score + "/100";
        }

        public String getDashboardTitle() {
            return dashboardTitle;
        }

        public String getDashboardDescription() {
            return dashboardDescription;
        }

        public int getAccentColor() {
            return accentColor;
        }
    }
}
