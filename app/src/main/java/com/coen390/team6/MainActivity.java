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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLE_APP";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String TARGET_DEVICE_NAME = "esp32_bracelet_test";
    private static final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("abcd1234-1234-1234-1234-1234567890ab");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final FirestoreRepository firestoreRepository = new FirestoreRepository();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private Button btnScanConnect;
    private Button btnRefresh;
    private TextView tvStatus;
    private TextView tvScanStatus;
    private TextView tvDeviceName;
    private TextView tvSignalStrength;

    private BluetoothDevice esp32Device;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final Handler scanHandler     = new Handler(Looper.getMainLooper());
    private int     reconnectDelay  = 2000;
    private boolean shouldReconnect = true;
    private boolean isScanning      = false;
    private boolean hasOpenedDashboard = false; // renamed: clearer intent

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
                String matchedDeviceName = deviceName;
                int deviceRssi = result.getRssi();
                runOnUiThread(() -> updateDeviceCard(matchedDeviceName, deviceRssi));
                connectToDevice();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            isScanning = false;
            Log.d(TAG, "BLE scan failed: " + errorCode);
            runOnUiThread(() -> {
                updateStatus("Status: Scan failed", android.R.color.holo_red_dark);
                updateScanStatus("Scan failed");
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScanConnect = findViewById(R.id.btnScan);
        tvStatus       = findViewById(R.id.tvStatus);

        // Dashboard button → go to GPS/Dashboard WITHOUT finishing MainActivity
        Button btnDashboard = findViewById(R.id.btn_dashboard);
        btnDashboard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GpsNavigationActivity.class)));

        btnScanConnect = findViewById(R.id.btnScan);
        btnRefresh = findViewById(R.id.btnRefresh);
        tvStatus = findViewById(R.id.tvStatus);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvSignalStrength = findViewById(R.id.tvSignalStrength);
        BleSensorPreferences.setConnected(this, false);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        btnScanConnect.setOnClickListener(v -> {
            if (checkPermissions()) startScanAndConnect();
        });
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                if (checkPermissions()) {
                    startScanAndConnect();
                }
            });
        }
        // Automatic Connection
        if (checkPermissions()) {
            startScanAndConnect();
        }
    }

    private void startScanAndConnect() {
        if (isDeviceConnected()) {
            updateStatus("Status: Already connected", android.R.color.holo_green_dark);
            updateScanStatus("Device already connected");
            return;
        }

        if (bluetoothAdapter == null) {
            updateStatus("Status: Bluetooth unavailable", android.R.color.holo_red_dark);
            updateScanStatus("Bluetooth unavailable");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            updateStatus("Status: BLE scanner unavailable", android.R.color.holo_red_dark);
            updateScanStatus("Scanner unavailable");
            return;
        }

        if (isScanning) {
            stopBleScan();
        }

        updateStatus("Status: Scanning...", android.R.color.holo_orange_dark);
        updateScanStatus("Scanning...");
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
                updateScanStatus("Device not found");
            }
        }, 10000);
    }

    private void connectToDevice() {
        if (esp32Device == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Missing BLUETOOTH_CONNECT permission");
            return;
        }

        closeCurrentGatt();
        updateStatus("Connecting...", android.R.color.holo_orange_dark);

        bluetoothGatt = esp32Device.connectGatt(this, false, new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to ESP32");
                    reconnectDelay = 2000;
                    bluetoothGatt = gatt;
                    BleSensorPreferences.setConnected(MainActivity.this, true);

                    runOnUiThread(() -> {
                        updateStatus("Status: Connected", android.R.color.holo_green_dark);
                        updateScanStatus("Connected");
                        openGpsHome();
                    });

                    gatt.discoverServices();

                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from ESP32");
                    if (bluetoothGatt == gatt) {
                        closeCurrentGatt();
                    }
                    BleSensorPreferences.setConnected(MainActivity.this, false);

                    runOnUiThread(() -> {
                        updateStatus("Status: Not Connected", android.R.color.holo_red_dark);
                        updateScanStatus("Disconnected");
                    });
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
                    return;
                }
                BluetoothGattCharacteristic characteristic = gatt
                        .getService(SERVICE_UUID)
                        .getCharacteristic(CHARACTERISTIC_UUID);
                if (characteristic == null) {
                    Log.d(TAG, "BLE characteristic not found");
                    return;
                }
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
                if (characteristic == null) return;
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
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
                                                @NonNull BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                final String value = new String(characteristic.getValue(), StandardCharsets.UTF_8);

                // 1. Parse raw Arduino packet
                BleSensorData raw = BleSensorData.fromPayload(value);

                // 2. Use the device-reported driverState directly
                BleSensorData sensorData = raw;

                // 3. Save to SharedPrefs + Firestore
                BleSensorPreferences.saveSensorData(MainActivity.this, sensorData);
                firestoreRepository.saveSensorReading(sensorData);
                int fatigueScore = Math.round(DetailedAnalysisActivity.computeFatigueScore(
                        sensorData.getAvgBpm() > 0 ? sensorData.getAvgBpm() : Math.round(sensorData.getBpm()),
                        sensorData.getGsrFiltered(),
                        sensorData.getGsrBaseline()
                ));
                DriverAlertManager.evaluateAndNotify(
                        MainActivity.this,
                        sensorData.getAvgBpm() > 0 ? sensorData.getAvgBpm() : Math.round(sensorData.getBpm()),
                        fatigueScore,
                        sensorData.getDriverState()
                );

                Log.d(TAG, "Value received: " + value + " → state=" + sensorData.getDriverState());
            }
        });
    }

    private void stopBleScan() {
        if (bluetoothAdapter == null) { isScanning = false; return; }
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) scanner.stopScan(scanCallback);
        scanHandler.removeCallbacksAndMessages(null);
        isScanning = false;
    }

    private boolean isDeviceConnected() {
        return bluetoothGatt != null && BleSensorPreferences.isConnected(this);
    }

    private void closeCurrentGatt() {
        if (bluetoothGatt == null) {
            return;
        }
        try {
            bluetoothGatt.disconnect();
        } catch (Exception ignored) {
        }
        try {
            bluetoothGatt.close();
        } catch (Exception ignored) {
        }
        bluetoothGatt = null;
    }

    private void reconnectDevice() {
        Log.d(TAG, "Reconnect in " + reconnectDelay + " ms");
        reconnectHandler.postDelayed(() -> {
            if (esp32Device != null) {
                connectToDevice();
                reconnectDelay = Math.min(reconnectDelay * 2, 30000); // cap at 30s
            }
        }, reconnectDelay);
    }

    // Permissions

    private void updateScanStatus(String statusText) {
        if (tvScanStatus != null) {
            tvScanStatus.setText(statusText);
        }
    }

    private void updateDeviceCard(String deviceName, int rssi) {
        if (tvDeviceName != null && deviceName != null && !deviceName.isEmpty()) {
            tvDeviceName.setText(deviceName);
        }
        if (tvSignalStrength != null) {
            String strength;
            if (rssi >= -60) {
                strength = "Strong signal";
            } else if (rssi >= -75) {
                strength = "Good signal";
            } else {
                strength = "Weak signal";
            }
            tvSignalStrength.setText("\uD83D\uDCF6 " + strength);
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
            boolean missingPermission = false;
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    missingPermission = true;
                    break;
                }
            }
            if (missingPermission) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                return false;
            }
            return true;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void openGpsHome() {
        if (hasOpenedDashboard) {
            return;
        }
        hasOpenedDashboard = true;
        startActivity(new Intent(MainActivity.this, GpsNavigationActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) { granted = false; break; }
            if (granted) startScanAndConnect();
            else updateStatus("Permissions denied", android.R.color.holo_red_dark);
        }
    }

    // ── UI

    private void updateStatus(String text, int colorRes) {
        if (tvStatus != null)
            tvStatus.setTextColor(getResources().getColor(colorRes));
        if (tvStatus != null)
            tvStatus.setText(text);
    }

    // ── Lifecycle

    @Override
    protected void onDestroy() {
        shouldReconnect = false;
        stopBleScan();
        reconnectHandler.removeCallbacksAndMessages(null);
        closeCurrentGatt();
        super.onDestroy();
    }
}
