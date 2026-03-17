package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private View navDashboardItem;
    private View navLogItem;
    private View navSettingsItem;
    private MaterialSwitch switchDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        bindViews();
        bindNavigation();
        bindThemeToggle();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
        switchDarkMode = findViewById(R.id.switchDarkMode);
    }

    private void bindNavigation() {
        navDashboardItem.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, DashboardActivity.class));
            finish();
        });

        navLogItem.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, DriverLogActivity.class));
            finish();
        });

        navSettingsItem.setOnClickListener(v -> {
            // Already on settings.
        });
    }

    private void bindThemeToggle() {
        switchDarkMode.setChecked(ThemePreferenceManager.isDarkModeEnabled(this));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                ThemePreferenceManager.setDarkModeEnabled(SettingsActivity.this, isChecked));
    }
}
