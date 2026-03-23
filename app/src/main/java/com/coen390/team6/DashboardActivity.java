package com.coen390.team6;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.graphics.Color;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class DashboardActivity extends AppCompatActivity {

    private android.widget.TextView heartRateText, fatigueText, batteryText;
    private MaterialButton bluetoothButton;
    private MaterialToolbar dashboardToolbar;
    private View navDashboardItem;
    private View navLogItem;
    private View navSettingsItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        bindText();
        bindData();
        bindNavigation();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void bindText() {
        heartRateText = findViewById(R.id.heartRateText);
        fatigueText = findViewById(R.id.fatigueText);
        bluetoothButton = findViewById(R.id.bluetoothText);
        batteryText = findViewById(R.id.batteryText);
        dashboardToolbar = findViewById(R.id.dashboardToolbar);
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
    }

    public void bindData() {
        heartRateText.setText(getString(R.string.heartrate_value_placeholder));
        fatigueText.setText(getString(R.string.fatigue_value_placeholder));
        batteryText.setText(getString(R.string.battery_value_placeholder));
        fatigueText.setTextColor(Color.parseColor("#4CAF50"));
        updateBluetoothButtonState();
    }

    private void bindNavigation() {
        navDashboardItem.setOnClickListener(v -> {
            // Already on the dashboard tab.
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBluetoothButtonState();
    }

    private void updateBluetoothButtonState() {
        boolean isConnected = BleConnectionPreferences.isConnected(this);
        int backgroundColor = ContextCompat.getColor(
                this,
                isConnected ? android.R.color.holo_green_dark : android.R.color.darker_gray
        );
        int strokeColor = ContextCompat.getColor(
                this,
                isConnected ? android.R.color.holo_green_dark : android.R.color.darker_gray
        );

        bluetoothButton.setText(isConnected ? R.string.connected : R.string.connect);
        bluetoothButton.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        bluetoothButton.setStrokeColor(ColorStateList.valueOf(strokeColor));
        bluetoothButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        bluetoothButton.setOnClickListener(isConnected ? null : v -> {
            Intent intent = new Intent(DashboardActivity.this, BleConnectionActivity.class);
            startActivity(intent);
        });
    }
}
