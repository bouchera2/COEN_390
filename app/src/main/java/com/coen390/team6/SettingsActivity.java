package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    // ── Nav ──────────────────────────────────────────────────────────────────
    private View navDashboardItem;
    private View navAlertsItem;
    private View navLogItem;
    private View navSettingsItem;

    private TextView tvDriverName;
    private TextView tvDriverEmail;
    private TextView tvDeviceStatus;
    private TextView tvDeviceSummary;

    private Button btnEditPersonalInfo;
    private Button btnOpenBlePage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        bindViews();
        bindNavigation();
        bindLogout();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    private void bindViews() {
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navAlertsItem    = findViewById(R.id.navAlertsItem);
        navLogItem       = findViewById(R.id.navLogItem);
        navSettingsItem  = findViewById(R.id.navSettingsItem);

        btnEditPersonalInfo = findViewById(R.id.btnEditPersonalInfo);
        btnOpenBlePage = findViewById(R.id.btnOpenBlePage);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvDriverEmail = findViewById(R.id.tvDriverEmail);
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus);
        tvDeviceSummary = findViewById(R.id.tvDeviceSummary);
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateAccountSummary();
        updateConnectionSummary();
    }

    //  Navigation
    private void bindNavigation() {
        navDashboardItem.setOnClickListener(v -> {
            startActivity(NavigationIntentFactory.createGpsIntent(this));
            finish();
        });
        navLogItem.setOnClickListener(v -> {
            startActivity(new Intent(this, DriverLogActivity.class));
            finish();
        });
        if (navAlertsItem != null) {
            navAlertsItem.setOnClickListener(v ->
                    startActivity(new Intent(this, AlertsActivity.class)));
        }
        navSettingsItem.setOnClickListener(v -> { /* already here */ });
        btnEditPersonalInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, DriverProfileSetupActivity.class);
            intent.putExtra(DriverProfileSetupActivity.EXTRA_EDIT_MODE, true);
            startActivity(intent);
        });
        btnOpenBlePage.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
    }

    private void populateAccountSummary() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            tvDriverName.setText("Driver");
            tvDriverEmail.setText("No email available");
            return;
        }
        String email = user.getEmail();
        tvDriverEmail.setText(
                email != null && !email.trim().isEmpty()
                        ? email
                        : "No email available"
        );

        FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String driverName = doc.getString("name");
                    if (driverName != null && !driverName.trim().isEmpty()) {
                        tvDriverName.setText(driverName);
                    } else if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                        tvDriverName.setText(user.getDisplayName());
                    } else {
                        tvDriverName.setText("Driver");
                    }
                })
                .addOnFailureListener(e -> {
                    if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                        tvDriverName.setText(user.getDisplayName());
                    } else {
                        tvDriverName.setText("Driver");
                    }
                });
    }

    private void updateConnectionSummary() {
        boolean connected = BleSensorPreferences.isConnected(this);
        int avgBpm = BleSensorPreferences.getAvgBpm(this);
        float gsrFiltered = BleSensorPreferences.getGsrFiltered(this);

        tvDeviceStatus.setText(connected ? "Connected" : "Disconnected");
        tvDeviceStatus.setTextColor(connected ? 0xFF22C55E : 0xFFEF4444);

        if (connected) {
            tvDeviceSummary.setText(String.format(
                    Locale.getDefault(),
                    "Streaming live data. Avg BPM: %d, filtered GSR: %.2f.",
                    avgBpm,
                    gsrFiltered
            ));
        } else {
            tvDeviceSummary.setText("");
        }
    }

    //  Logout
    private void bindLogout() {
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

}
