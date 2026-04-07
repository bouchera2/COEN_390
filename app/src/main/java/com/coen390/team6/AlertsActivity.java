package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AlertsActivity extends AppCompatActivity {

    private View navDashboardItem;
    private View navAlertsItem;
    private View navLogItem;
    private View navSettingsItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alerts);

        bindViews();
        bindNavigation();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navAlertsItem = findViewById(R.id.navAlertsItem);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
    }

    private void bindNavigation() {
        if (navDashboardItem != null) {
            navDashboardItem.setOnClickListener(v -> {
                startActivity(new Intent(this, GpsNavigationActivity.class));
                finish();
            });
        }

        if (navAlertsItem != null) {
            navAlertsItem.setOnClickListener(v -> {
                // Already on alerts.
            });
        }

        if (navLogItem != null) {
            navLogItem.setOnClickListener(v -> {
                startActivity(new Intent(this, DriverLogActivity.class));
                finish();
            });
        }

        if (navSettingsItem != null) {
            navSettingsItem.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
            });
        }
    }
}
