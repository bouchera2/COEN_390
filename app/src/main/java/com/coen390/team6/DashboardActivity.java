package com.coen390.team6;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DashboardActivity extends AppCompatActivity {

    private TextView heartRateText, fatigueText, bluetoothText, batteryText;
    private TextView fatigueLabelText;
    private TextView fatigueDescriptionText;
    private TextView fatigueGaugeIcon;
    private TextView drivingTimeText;
    private TextView drivingTimeUnitText;
    private MaterialToolbar dashboardToolbar;
    private CircularProgressIndicator fatigueGauge;
    private ProgressBar drivingTimeProgress;
    private ImageView routeSnapshotImage;
    private View[] heartRateBars;
    private View batteryChip;
    private View navDashboardItem;
    private View navAlertsItem;
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
        fatigueLabelText = findViewById(R.id.fatigueLabelText);
        fatigueDescriptionText = findViewById(R.id.fatigueDescriptionText);
        fatigueGaugeIcon = findViewById(R.id.fatigueGaugeIcon);
        fatigueGauge = findViewById(R.id.fatigueGauge);
        drivingTimeText = findViewById(R.id.drivingTimeText);
        drivingTimeUnitText = findViewById(R.id.drivingTimeUnitText);
        drivingTimeProgress = findViewById(R.id.drivingTimeProgress);
        routeSnapshotImage = findViewById(R.id.routeSnapshotImage);
        dashboardToolbar = findViewById(R.id.dashboardToolbar);
        batteryChip = findViewById(R.id.batteryChip);
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navAlertsItem = findViewById(R.id.navAlertsItem);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
        heartRateBars = new View[]{
                findViewById(R.id.heartBar1),
                findViewById(R.id.heartBar2),
                findViewById(R.id.heartBar3),
                findViewById(R.id.heartBar4),
                findViewById(R.id.heartBar5)
        };
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
        if (navDashboardItem != null) {
            navDashboardItem.setOnClickListener(v -> {
                startActivity(new Intent(this, GpsNavigationActivity.class));
                finish();
            });
        }

        if (navLogItem != null) {
            navLogItem.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, DriverLogActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (navAlertsItem != null) {
            navAlertsItem.setOnClickListener(v -> {
                startActivity(new Intent(this, AlertsActivity.class));
                finish();
            });
        }

        if (navSettingsItem != null) {
            navSettingsItem.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
            });
        }

        View activeRouteCard = findViewById(R.id.activeRouteCard);
        if (activeRouteCard != null) {
            activeRouteCard.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, GpsNavigationActivity.class);
                startActivity(intent);
            });
        }

        View detailedAnalysisButton = findViewById(R.id.btnDetailedAnalysis);
        if (detailedAnalysisButton != null) {
            detailedAnalysisButton.setOnClickListener(v ->
                    startActivity(new Intent(this, DetailedAnalysisActivity.class)));
        }
    }

    private void refreshSensorData() {
        boolean isConnected = BleSensorPreferences.isConnected(this);
        int avgBpm = BleSensorPreferences.getAvgBpm(this);
        float bpm = BleSensorPreferences.getBpm(this);
        DriverFatigueStatus fatigueStatus = DriverFatigueStatus.from(this);
        boolean fingerDetected = BleSensorPreferences.isFingerDetected(this);

        if (heartRateText != null) {
            heartRateText.setText(formatHeartRate(fingerDetected, avgBpm, bpm));
        }
        if (bluetoothText != null) {
            bluetoothText.setText(isConnected ? getString(R.string.connected) : getString(R.string.bt_status_disconnected));
        }
        if (batteryChip != null) {
            batteryChip.setVisibility(isConnected ? View.VISIBLE : View.GONE);
        }
        if (batteryText != null) {
            batteryText.setText(getString(R.string.battery_value_unavailable));
        }
        if (fatigueText != null) {
            fatigueText.setText(fatigueStatus.getRiskPercent() + "%");
            fatigueText.setTextColor(Color.parseColor("#3B82F6"));
        }
        if (fatigueLabelText != null) {
            fatigueLabelText.setText(fatigueStatus.getDashboardTitle());
            fatigueLabelText.setTextColor(fatigueStatus.getAccentColor());
        }
        if (fatigueDescriptionText != null) {
            fatigueDescriptionText.setText(fatigueStatus.getDashboardDescription());
        }
        if (fatigueGaugeIcon != null) {
            fatigueGaugeIcon.setText(fatigueStatus.getEmoji());
            fatigueGaugeIcon.setTextColor(fatigueStatus.getAccentColor());
        }
        if (fatigueGauge != null) {
            fatigueGauge.setIndicatorColor(fatigueStatus.getAccentColor());
            fatigueGauge.setProgress(fatigueStatus.getRiskPercent());
        }

        updateHeartRateBars(fingerDetected, avgBpm, bpm);
        updateDrivingTimeCard();
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

    private void updateHeartRateBars(boolean fingerDetected, int avgBpm, float bpm) {
        if (heartRateBars == null) {
            return;
        }

        if (!fingerDetected) {
            for (View bar : heartRateBars) {
                if (bar != null) {
                    setBarHeight(bar, 8);
                    bar.setAlpha(0.25f);
                }
            }
            return;
        }

        int displayBpm = avgBpm > 0 ? avgBpm : Math.round(bpm);
        int normalized = Math.max(45, Math.min(displayBpm, 140));
        int baseHeight = 10 + ((normalized - 45) * 18 / 95);
        int[] offsets = {-6, 2, -3, 8, 0};

        for (int i = 0; i < heartRateBars.length; i++) {
            View bar = heartRateBars[i];
            if (bar == null) {
                continue;
            }

            int heightDp = Math.max(8, Math.min(baseHeight + offsets[i], 32));
            setBarHeight(bar, heightDp);
            bar.setAlpha(i == heartRateBars.length - 1 ? 1f : 0.35f);
        }
    }

    private void updateDrivingTimeCard() {
        if (!DrivingSessionPreferences.isActive(this)) {
            if (drivingTimeText != null) {
                drivingTimeText.setText("--:--");
            }
            if (drivingTimeUnitText != null) {
                drivingTimeUnitText.setText("");
            }
            if (drivingTimeProgress != null) {
                drivingTimeProgress.setProgress(0);
            }
            return;
        }

        long startedAtMs = DrivingSessionPreferences.getStartedAtMs(this);
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAtMs);
        long totalMinutes = elapsedMs / 60000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;

        if (drivingTimeText != null) {
            drivingTimeText.setText(String.format(java.util.Locale.US, "%02d:%02d", hours, minutes));
        }
        if (drivingTimeUnitText != null) {
            drivingTimeUnitText.setText(hours > 0 ? "Hrs" : "Min");
        }
        if (drivingTimeProgress != null) {
            int progress = (int) Math.min(100L, (totalMinutes * 100L) / 480L);
            drivingTimeProgress.setProgress(progress);
        }
    }

    private void setBarHeight(View bar, int heightDp) {
        ViewGroup.LayoutParams params = bar.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                heightDp,
                getResources().getDisplayMetrics()
        );
        bar.setLayoutParams(params);
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
                    Double lastKnownLat = doc.getDouble("lastKnownLat");
                    Double lastKnownLng = doc.getDouble("lastKnownLng");
                    if (lastKnownLat != null && lastKnownLng != null) {
                        loadRouteSnapshot(lastKnownLat, lastKnownLng);
                    }
                    refreshSensorData();
                });
    }

    private void loadRouteSnapshot(double lat, double lng) {
        if (routeSnapshotImage == null) {
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            try {
                String urlString = "https://maps.googleapis.com/maps/api/staticmap"
                        + "?center=" + lat + "," + lng
                        + "&zoom=13"
                        + "&size=1200x700"
                        + "&scale=2"
                        + "&maptype=roadmap"
                        + "&style=feature:all|element:labels.text.fill|color:0xffffff"
                        + "&style=feature:all|element:labels.text.stroke|color:0x0a0c10"
                        + "&style=feature:administrative|element:geometry|color:0x1e293b"
                        + "&style=feature:poi|visibility:off"
                        + "&style=feature:road|element:geometry|color:0x263247"
                        + "&style=feature:road.highway|element:geometry|color:0x135bec"
                        + "&style=feature:transit|visibility:off"
                        + "&style=feature:water|element:geometry|color:0x0f172a"
                        + "&markers=color:blue%7C" + lat + "," + lng
                        + "&key=" + getMapsApiKey();

                connection = (HttpURLConnection) new URL(urlString).openConnection();
                connection.setDoInput(true);
                connection.connect();
                inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    runOnUiThread(() -> routeSnapshotImage.setImageBitmap(bitmap));
                }
            } catch (Exception ignored) {
                // Keep the fallback placeholder if the snapshot request fails.
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception ignored) {
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String getMapsApiKey() {
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager()
                    .getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) {
            return "";
        }
    }
}
