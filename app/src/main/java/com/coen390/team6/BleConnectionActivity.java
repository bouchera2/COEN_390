package com.coen390.team6;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class BleConnectionActivity extends AppCompatActivity {
    private BleConnectionManager bleConnectionManager;

    private ProgressBar progressScan;
    private TextView tvSearchStatus;
    private TextView tvDeviceName;
    private TextView tvDeviceDetail;
    private MaterialCardView deviceCard;

    private boolean isConnected;
    private boolean isConnecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ble_connection);

        bleConnectionManager = new BleConnectionManager(this);

        bindViews();
        bindToolbar();
        bindActions();
        applyInsets();

        if (checkPermissions()) {
            startScan();
        }
    }

    private void bindViews() {
        progressScan = findViewById(R.id.progressScan);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvDeviceDetail = findViewById(R.id.tvDeviceDetail);
        deviceCard = findViewById(R.id.deviceCard);
    }

    private void bindToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.bleToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindActions() {
        deviceCard.setOnClickListener(v -> connectIfPossible());
        findViewById(R.id.btnRetryScan).setOnClickListener(v -> {
            if (checkPermissions()) {
                startScan();
            }
        });
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bleRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bleConnectionManager.hasRequiredPermissions(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    bleConnectionManager.getRequiredPermissions(),
                    BleConnectionManager.PERMISSION_REQUEST_CODE
            );
            return false;
        }
        return true;
    }

    private void startScan() {
        isConnecting = false;
        isConnected = BleConnectionPreferences.isConnected(this);
        BleConnectionPreferences.setConnected(this, false);
        setScanningState();

        if (bleConnectionManager.prepareTargetDevice(this)) {
            showDeviceReady();
        } else {
            showDeviceUnavailable(getString(R.string.ble_device_not_found));
        }
    }

    private void connectIfPossible() {
        if (isConnected || isConnecting) {
            return;
        }
        if (!bleConnectionManager.prepareTargetDevice(this)) {
            if (checkPermissions()) {
                showDeviceUnavailable(getString(R.string.ble_device_not_found));
            }
            return;
        }
        connectToDevice();
    }

    private void connectToDevice() {
        bleConnectionManager.connect(this, new BleConnectionManager.ConnectionListener() {
            @Override
            public void onConnecting() {
                runOnUiThread(() -> {
                    isConnecting = true;
                    tvSearchStatus.setText(R.string.ble_connecting);
                    tvDeviceDetail.setText(R.string.ble_connecting_subtitle);
                    deviceCard.setEnabled(false);
                });
            }

            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    isConnected = true;
                    isConnecting = false;
                    BleConnectionPreferences.setConnected(BleConnectionActivity.this, true);
                    tvSearchStatus.setText(R.string.connected);
                    tvDeviceDetail.setText(R.string.ble_connected_subtitle);
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    isConnected = false;
                    isConnecting = false;
                    BleConnectionPreferences.setConnected(BleConnectionActivity.this, false);
                    tvSearchStatus.setText(R.string.ble_connection_failed);
                    tvDeviceDetail.setText(R.string.ble_retry_hint);
                    deviceCard.setEnabled(true);
                });
            }

            @Override
            public void onDeviceUnavailable() {
                runOnUiThread(() -> showDeviceUnavailable(getString(R.string.ble_device_not_found)));
            }

            @Override
            public void onPermissionsMissing() {
                runOnUiThread(() -> showDeviceUnavailable(getString(R.string.ble_permission_required)));
            }
        });
    }

    private void setScanningState() {
        progressScan.setVisibility(View.VISIBLE);
        tvSearchStatus.setText(R.string.ble_searching);
        tvDeviceName.setText(R.string.ble_device_name);
        tvDeviceDetail.setText(R.string.ble_scan_hint);
        deviceCard.setEnabled(true);
    }

    private void showDeviceReady() {
        progressScan.setVisibility(View.VISIBLE);
        tvSearchStatus.setText(R.string.ble_searching);
        tvDeviceName.setText(R.string.ble_device_name);
        tvDeviceDetail.setText(R.string.ble_device_ready);
        deviceCard.setEnabled(true);
    }

    private void showDeviceUnavailable(String message) {
        progressScan.setVisibility(View.GONE);
        tvSearchStatus.setText(R.string.ble_search_complete);
        tvDeviceName.setText(R.string.ble_no_device_title);
        tvDeviceDetail.setText(message);
        deviceCard.setEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != BleConnectionManager.PERMISSION_REQUEST_CODE) {
            return;
        }

        boolean granted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }

        if (granted) {
            startScan();
        } else {
            showDeviceUnavailable(getString(R.string.ble_permission_required));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isConnected) {
            bleConnectionManager.close();
        }
    }
}
