package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.appbar.MaterialToolbar;

public class DashboardActivity extends AppCompatActivity {

    private TextView heartRateText, fatigueText, bluetoothText, batteryText;
    private MaterialToolbar dashboardToolbar;
    private View navDashboardItem;
    private View navLogItem;
    private View navSettingsItem;
    private final Handler sensorRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable sensorRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshSensorData();
            sensorRefreshHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        bindText();
        bindData();
        bindNavigation();
        syncDriverThresholds();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void bindText() {
        heartRateText = findViewById(R.id.heartRateText);
        fatigueText = findViewById(R.id.fatigueText);
        bluetoothText = findViewById(R.id.bluetoothText);
        batteryText = findViewById(R.id.batteryText);
        dashboardToolbar = findViewById(R.id.dashboardToolbar);
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
    }

    public void bindData() {
        refreshSensorData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSensorData();
        syncDriverThresholds();
        sensorRefreshHandler.post(sensorRefreshRunnable);
    }

    @Override
    protected void onPause() {
        sensorRefreshHandler.removeCallbacks(sensorRefreshRunnable);
        super.onPause();
    }

    private void bindNavigation() {
        dashboardToolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        dashboardToolbar.setNavigationOnClickListener(v -> {
            startActivity(new Intent(this, GpsNavigationActivity.class));
            finish();
        });

        navDashboardItem.setOnClickListener(v -> {
            startActivity(new Intent(this, GpsNavigationActivity.class));
            finish();
        });

        navLogItem.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, DriverLogActivity.class);
            startActivity(intent);
            finish();
        });

        navSettingsItem.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });


        // GPS Map card — tap to open full GPS navigation
        View activeRouteCard = findViewById(R.id.activeRouteCard);
        activeRouteCard.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, GpsNavigationActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnDetailedAnalysis).setOnClickListener(v ->
                startActivity(new Intent(this, DetailedAnalysisActivity.class)));
    }

    private void refreshSensorData() {
        boolean isConnected = BleSensorPreferences.isConnected(this);
        int avgBpm = BleSensorPreferences.getAvgBpm(this);
        float bpm = BleSensorPreferences.getBpm(this);
        DriverFatigueStatus fatigueStatus = DriverFatigueStatus.from(this);

        heartRateText.setText(formatHeartRate(BleSensorPreferences.isFingerDetected(this), avgBpm, bpm));
        bluetoothText.setText(isConnected ? getString(R.string.connected) : getString(R.string.bt_status_disconnected));
        batteryText.setText(getString(R.string.battery_value_unavailable));
        fatigueText.setText(fatigueStatus.getLabel());
        fatigueText.setTextColor(fatigueStatus.getAccentColor());
    }

    private String formatHeartRate(boolean fingerDetected, int avgBpm, float bpm) {
        if (!fingerDetected) {
            return getString(R.string.heartrate_value_waiting);
        }

        int displayBpm = avgBpm > 0 ? avgBpm : Math.round(bpm);
        if (displayBpm <= 0) {
            return getString(R.string.heartrate_value_waiting);
        }
        return getString(R.string.heartrate_value_format, displayBpm);
    }

    private void syncDriverThresholds() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }

                    Long restingHeartRate = doc.getLong("restingHeartRate");
                    Long normalHrLow = doc.getLong("normalHRLow");
                    Long normalHrHigh = doc.getLong("normalHRHigh");
                    Long fatigueHrThreshold = doc.getLong("fatigueHRThreshold");
                    Long stressHrThreshold = doc.getLong("stressHRThreshold");

                    if (restingHeartRate == null
                            || normalHrLow == null
                            || normalHrHigh == null
                            || fatigueHrThreshold == null
                            || stressHrThreshold == null) {
                        return;
                    }

                    DriverProfilePreferences.saveThresholds(
                            this,
                            restingHeartRate.intValue(),
                            normalHrLow.intValue(),
                            normalHrHigh.intValue(),
                            fatigueHrThreshold.intValue(),
                            stressHrThreshold.intValue()
                    );
                    refreshSensorData();
                });
    }
}
