package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnDashboard = findViewById(R.id.btn_dashboard);
        Button btnDisconnect = findViewById(R.id.btn_disconnect);
        Button btnScan = findViewById(R.id.btnScan);
        Button btnConnect = findViewById(R.id.btnConnect);
        tvStatus = findViewById(R.id.tvStatus);

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
        });

        btnDisconnect.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Legacy BLE controls are intentionally disabled while the new dashboard
        // -> BLE connection flow is being validated.
        btnScan.setEnabled(false);
        btnConnect.setEnabled(false);
        tvStatus.setText(R.string.main_ble_legacy_disabled);
    }
}
