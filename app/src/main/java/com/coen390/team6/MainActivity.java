package com.coen390.team6;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLE_APP";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;
    private static final String TARGET_DEVICE_NAME = "esp32_bracelet_test";
    private static final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("abcd1234-1234-1234-1234-1234567890ab");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final FirestoreRepository firestoreRepository = new FirestoreRepository();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private Button btnScanConnect;
    private TextView tvStatus;

    private BluetoothDevice esp32Device;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private int reconnectDelay = 2000;
    private boolean shouldReconnect = true;
    private boolean isScanning = false;
    private boolean hasOpenedGps;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String deviceName = null;
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                deviceName = device.getName();
            }

            if (TARGET_DEVICE_NAME.equals(deviceName)) {
                stopBleScan();
                esp32Device = device;
                connectToDevice();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            isScanning = false;
            Log.d(TAG, "BLE scan failed: " + errorCode);
            runOnUiThread(() -> updateStatus("Status: Scan failed", android.R.color.holo_red_dark));
        }
    };

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
                Intent intent = new Intent(MainActivity.this, GpsNavigationActivity.class);
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
        requestNotificationPermissionIfNeeded();

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST_CODE
        );
    }

    private void startScanAndConnect() {
        if (bluetoothAdapter == null) {
            updateStatus("Status: Bluetooth unavailable", android.R.color.holo_red_dark);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            updateStatus("Status: BLE scanner unavailable", android.R.color.holo_red_dark);
            return;
        }

        if (isScanning) {
            stopBleScan();
        }

        updateStatus("Status: Scanning...", android.R.color.holo_orange_dark);
        isScanning = true;
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);

        scanHandler.postDelayed(() -> {
            if (isScanning) {
                stopBleScan();
                updateStatus("Status: Device not found", android.R.color.holo_red_dark);
            }
        }, 10000);
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

                    runOnUiThread(() -> {
                        updateStatus("Status: Connected", android.R.color.holo_green_dark);
                        openGpsHome();
                    });

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
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.requestMtu(185);
                }
                BluetoothGattDescriptor cccd = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                if (cccd != null) {
                    cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(cccd);
                }
                Log.d(TAG, "Notifications enabled!");
            }
            @Override
            public void onMtuChanged(@NonNull BluetoothGatt gatt, int mtu, int status) {
                Log.d(TAG, "MTU = " + mtu);
                BluetoothGattCharacteristic characteristic = gatt
                        .getService(SERVICE_UUID)
                        .getCharacteristic(CHARACTERISTIC_UUID);
                BluetoothGattDescriptor cccd = characteristic
                        .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                if (cccd != null) {
                    cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.writeDescriptor(cccd);
                        Log.d(TAG, "Notifications enabled after MTU!");
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                final String value = new String(characteristic.getValue(), StandardCharsets.UTF_8);

                // 1. Parse le paquet brut de l'Arduino
                BleSensorData raw = BleSensorData.fromPayload(value);

                // 2. Reclassifie le driverState avec les thresholds réglés dans Settings
                //    (ignore le "ds=..." envoyé par l'Arduino)
                boolean baselineReady = raw.getGsrBaseline() > 0.01f;
                String appState = ThresholdPreferences.classifyDriverState(
                        MainActivity.this,
                        raw.getAvgBpm(),
                        raw.getGsrFiltered(),
                        raw.getGsrBaseline(),
                        baselineReady
                );

                // 3. Crée une copie avec le driverState de l'app
                BleSensorData sensorData = raw.withDriverState(appState);

                // 4. Sauvegarde normalement — même pipeline qu'avant
                BleSensorPreferences.saveSensorData(MainActivity.this, sensorData);
                AlertMonitor.processSensorState(MainActivity.this, sensorData);
                firestoreRepository.saveSensorReading(sensorData);

                runOnUiThread(() -> updateStatus(buildConnectedStatus(sensorData), android.R.color.holo_green_dark));
                Log.d(TAG, "Value received: " + value + " → state=" + appState);
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

    private void stopBleScan() {
        if (bluetoothAdapter == null) {
            isScanning = false;
            return;
        }

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) {
            scanner.stopScan(scanCallback);
        }
        scanHandler.removeCallbacksAndMessages(null);
        isScanning = false;
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

    private void openGpsHome() {
        if (hasOpenedGps) {
            return;
        }

        hasOpenedGps = true;
        Intent intent = new Intent(this, GpsNavigationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        stopBleScan();
        reconnectHandler.removeCallbacksAndMessages(null);
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        super.onDestroy();
    }
}
