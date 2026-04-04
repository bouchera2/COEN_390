package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
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

    // ── Threshold UI ─────────────────────────────────────────────────────────
    // BPM DROWSY max  (range 40–90, default 60)
    private SeekBar  seekBpmDrowsy;
    private TextView tvBpmDrowsyVal;

    // BPM STRESSED min (range 70–130, default 95)
    private SeekBar  seekBpmStressed;
    private TextView tvBpmStressedVal;

    // GSR DROWSY max ratio (range 0.50–1.20, step 0.01, default 0.90)
    private SeekBar  seekGsrDrowsy;
    private TextView tvGsrDrowsyVal;

    // GSR STRESSED min ratio (range 0.70–1.50, step 0.01, default 1.00)
    private SeekBar  seekGsrStressed;
    private TextView tvGsrStressedVal;

    private TextView tvDriverName;
    private TextView tvDriverEmail;
    private TextView tvDeviceStatus;
    private TextView tvDeviceSummary;

    // Reset button
    private Button btnResetThresholds;
    private Button btnEditPersonalInfo;

    // ── SeekBar config ───────────────────────────────────────────────────────
    private static final int BPM_DROWSY_MIN   = 40;
    private static final int BPM_STRESSED_MIN_OFFSET = 70;

    // GSR seekbars: stored as int (value * 100), e.g. 0.90 → 90
    private static final int GSR_DROWSY_SEEKBAR_OFFSET   = 50;  // 0.50
    private static final int GSR_STRESSED_SEEKBAR_OFFSET = 70;  // 0.70

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        bindViews();
        bindNavigation();
        bindThresholdControls();
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

        seekBpmDrowsy    = findViewById(R.id.seekBpmDrowsy);
        tvBpmDrowsyVal   = findViewById(R.id.tvBpmDrowsyVal);
        seekBpmStressed  = findViewById(R.id.seekBpmStressed);
        tvBpmStressedVal = findViewById(R.id.tvBpmStressedVal);
        seekGsrDrowsy    = findViewById(R.id.seekGsrDrowsy);
        tvGsrDrowsyVal   = findViewById(R.id.tvGsrDrowsyVal);
        seekGsrStressed  = findViewById(R.id.seekGsrStressed);
        tvGsrStressedVal = findViewById(R.id.tvGsrStressedVal);
        btnResetThresholds = findViewById(R.id.btnResetThresholds);
        btnEditPersonalInfo = findViewById(R.id.btnEditPersonalInfo);
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
            startActivity(new Intent(this, GpsNavigationActivity.class));
            finish();
        });
        navLogItem.setOnClickListener(v -> {
            startActivity(new Intent(this, DriverLogActivity.class));
            finish();
        });
        navAlertsItem.setOnClickListener(v -> {
            startActivity(new Intent(this, AlertsActivity.class));
            finish();
        });
        navSettingsItem.setOnClickListener(v -> { /* already here */ });
        btnEditPersonalInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, DriverProfileSetupActivity.class);
            intent.putExtra(DriverProfileSetupActivity.EXTRA_EDIT_MODE, true);
            startActivity(intent);
        });
    }

    // ── Thresholds
    private void bindThresholdControls() {
        // ── BPM DROWSY MAX (seekbar range 0-50 → actual 40-90)
        seekBpmDrowsy.setMax(50);
        int bpmDrowsyCurrent = ThresholdPreferences.getBpmDrowsyMax(this);
        seekBpmDrowsy.setProgress(bpmDrowsyCurrent - BPM_DROWSY_MIN);
        tvBpmDrowsyVal.setText("< " + bpmDrowsyCurrent + " BPM");

        seekBpmDrowsy.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = progress + BPM_DROWSY_MIN;
                tvBpmDrowsyVal.setText("< " + val + " BPM");
                if (fromUser) {
                    ThresholdPreferences.setBpmDrowsyMax(SettingsActivity.this, val);
                }
            }
        });

        // ── BPM STRESSED MIN (seekbar range 0-60 → actual 70-130)
        seekBpmStressed.setMax(60);
        int bpmStressedCurrent = ThresholdPreferences.getBpmStressedMin(this);
        seekBpmStressed.setProgress(bpmStressedCurrent - BPM_STRESSED_MIN_OFFSET);
        tvBpmStressedVal.setText("> " + bpmStressedCurrent + " BPM");

        seekBpmStressed.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = progress + BPM_STRESSED_MIN_OFFSET;
                tvBpmStressedVal.setText("> " + val + " BPM");
                if (fromUser) {
                    ThresholdPreferences.setBpmStressedMin(SettingsActivity.this, val);
                }
            }
        });

        // ── GSR DROWSY MAX ratio (seekbar range 0-70 → 0.50-1.20)
        seekGsrDrowsy.setMax(70);
        int gsrDrowsyProgress = Math.round(ThresholdPreferences.getGsrDrowsyMax(this) * 100) - GSR_DROWSY_SEEKBAR_OFFSET;
        seekGsrDrowsy.setProgress(gsrDrowsyProgress);
        tvGsrDrowsyVal.setText(String.format(Locale.getDefault(), "< %.2f",
                ThresholdPreferences.getGsrDrowsyMax(this)));

        seekGsrDrowsy.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = (progress + GSR_DROWSY_SEEKBAR_OFFSET) / 100f;
                tvGsrDrowsyVal.setText(String.format(Locale.getDefault(), "< %.2f", val));
                if (fromUser) {
                    ThresholdPreferences.setGsrDrowsyMax(SettingsActivity.this, val);
                }
            }
        });

        // ── GSR STRESSED MIN ratio (seekbar range 0-80 → 0.70-1.50) ─────────
        seekGsrStressed.setMax(80);
        int gsrStressedProgress = Math.round(ThresholdPreferences.getGsrStressedMin(this) * 100) - GSR_STRESSED_SEEKBAR_OFFSET;
        seekGsrStressed.setProgress(gsrStressedProgress);
        tvGsrStressedVal.setText(String.format(Locale.getDefault(), "> %.2f",
                ThresholdPreferences.getGsrStressedMin(this)));

        seekGsrStressed.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = (progress + GSR_STRESSED_SEEKBAR_OFFSET) / 100f;
                tvGsrStressedVal.setText(String.format(Locale.getDefault(), "> %.2f", val));
                if (fromUser) {
                    ThresholdPreferences.setGsrStressedMin(SettingsActivity.this, val);
                }
            }
        });

        // ── Reset button
        btnResetThresholds.setOnClickListener(v -> {
            ThresholdPreferences.resetToDefaults(this);
            // Re-apply seekbars to default values
            seekBpmDrowsy.setProgress(ThresholdPreferences.DEFAULT_BPM_DROWSY_MAX - BPM_DROWSY_MIN);
            seekBpmStressed.setProgress(ThresholdPreferences.DEFAULT_BPM_STRESSED_MIN - BPM_STRESSED_MIN_OFFSET);
            seekGsrDrowsy.setProgress(Math.round(ThresholdPreferences.DEFAULT_GSR_DROWSY_MAX * 100) - GSR_DROWSY_SEEKBAR_OFFSET);
            seekGsrStressed.setProgress(Math.round(ThresholdPreferences.DEFAULT_GSR_STRESSED_MIN * 100) - GSR_STRESSED_SEEKBAR_OFFSET);
        });
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

    // ── Helper: blank SeekBar listener
    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar)  {}
    }
}
