package com.coen390.team6;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLE_APP";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String TARGET_DEVICE_NAME = "esp32_bracelet_test";
    private static final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("abcd1234-1234-1234-1234-1234567890ab");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private Button btnScanConnect;
    private TextView tvStatus;

    private BluetoothDevice esp32Device;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private int reconnectDelay = 2000;
    private boolean shouldReconnect = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnDashboard = findViewById(R.id.btn_dashboard);
        Button btnDisconnect = findViewById(R.id.btn_disconnect);
        btnDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                startActivity(intent);
            }
        });
        btnDisconnect.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnScanConnect = findViewById(R.id.btnScan);
        tvStatus = findViewById(R.id.tvStatus);
        BleSensorPreferences.setConnected(this, false);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        btnScanConnect.setOnClickListener(v -> {
            if (checkPermissions()) {
                startScanAndConnect();
            }
        });
        // Automatic Connection
        if (checkPermissions()) {
            startScanAndConnect();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    private void startScanAndConnect() {
        if (bluetoothAdapter == null) {
            updateStatus("Status: Bluetooth unavailable", android.R.color.holo_red_dark);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        // Scan bonded Bluetooth devices
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (TARGET_DEVICE_NAME.equals(device.getName())) {
                esp32Device = device;
                connectToDevice();
                return;
            }
        }
        Log.d(TAG, "ESP32 not found in bonded devices!");
        updateStatus("Status: Device not found", android.R.color.holo_red_dark);
    }

    private void connectToDevice() {
        if (esp32Device == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Missing BLUETOOTH_CONNECT permission");
            return;
        }

        updateStatus("Status: Connecting...", android.R.color.holo_orange_dark);
        bluetoothGatt = esp32Device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {

                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {

                    Log.d(TAG,"Connected to ESP32");
                    reconnectDelay = 2000;
                    BleSensorPreferences.setConnected(MainActivity.this, true);

                    runOnUiThread(() -> updateStatus("Status: Connected", android.R.color.holo_green_dark));

                    gatt.discoverServices();

                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {

                    Log.d(TAG,"Disconnected from ESP32");
                    BleSensorPreferences.setConnected(MainActivity.this, false);

                    runOnUiThread(() -> updateStatus("Status: Not Connected", android.R.color.holo_red_dark));
                    if (shouldReconnect) {
                        reconnectDevice();
                    }
                }
            }

            @Override
            public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (gatt.getService(SERVICE_UUID) == null) {
                    Log.d(TAG, "BLE service not found");
                    runOnUiThread(() -> updateStatus("Status: Service not found", android.R.color.holo_red_dark));
                    return;
                }

                BluetoothGattCharacteristic characteristic = gatt
                        .getService(SERVICE_UUID)
                        .getCharacteristic(CHARACTERISTIC_UUID);
                if (characteristic == null) {
                    Log.d(TAG, "BLE characteristic not found");
                    runOnUiThread(() -> updateStatus("Status: Characteristic not found", android.R.color.holo_red_dark));
                    return;
                }

                // Enable Notifications
                gatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor cccd = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                if (cccd != null) {
                    cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(cccd);
                }
                Log.d(TAG, "Notifications enabled!");
            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                final String value = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                BleSensorData sensorData = BleSensorData.fromPayload(value);
                BleSensorPreferences.saveSensorData(MainActivity.this, sensorData);

                runOnUiThread(() -> updateStatus(buildConnectedStatus(sensorData), android.R.color.holo_green_dark));
                Log.d(TAG, "Value received: " + value);
            }
        });
    }


    private void reconnectDevice() {
        Log.d(TAG, "Reconnection attempt in " + reconnectDelay + " ms");
        reconnectHandler.postDelayed(() -> {

            if (esp32Device != null) {
                connectToDevice();
                reconnectDelay *= 2; // exponential backoff
            }

        }, reconnectDelay);
    }

    private String buildConnectedStatus(BleSensorData sensorData) {
        if (!sensorData.isFingerDetected()) {
            return "Status: Connected - waiting for finger";
        }

        int heartRate = sensorData.getAvgBpm() > 0 ? sensorData.getAvgBpm() : Math.round(sensorData.getBpm());
        return "Status: Connected - " + heartRate + " bpm";
    }

    private void updateStatus(String statusText, int colorRes) {
        tvStatus.setText(statusText);
        tvStatus.setTextColor(getResources().getColor(colorRes));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                startScanAndConnect();
            } else {
                Log.d(TAG, "BLE permissions denied!");
                updateStatus("Status: Permissions denied", android.R.color.holo_red_dark);
            }
        }
    }

    @Override
    protected void onDestroy() {
        shouldReconnect = false;
        reconnectHandler.removeCallbacksAndMessages(null);
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        super.onDestroy();
    }
}
