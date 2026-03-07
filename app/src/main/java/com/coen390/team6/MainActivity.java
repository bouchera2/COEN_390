package com.coen390.team6;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLE_APP";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private Button btnScanConnect;
    private TextView tvStatus;

    // UUID of our ESP32
    private final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private final UUID CHARACTERISTIC_UUID = UUID.fromString("abcd1234-1234-1234-1234-1234567890ab");

    private BluetoothDevice esp32Device;

    private int reconnectDelay = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScanConnect = findViewById(R.id.btnScan);
        tvStatus = findViewById(R.id.tvStatus);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

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
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        // Scan bonded Bluetooth devices
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if ("esp32_bracelet_test".equals(device.getName())) {
                esp32Device = device;
                connectToDevice();
                return;
            }
        }
        Log.d(TAG, "ESP32 not found in bonded devices!");
    }

    private void connectToDevice() {
        if (esp32Device == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Missing BLUETOOTH_CONNECT permission");
            return;
        }

        bluetoothGatt = esp32Device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {

                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {

                    Log.d(TAG,"Connected to ESP32");

                    runOnUiThread(() -> {
                        tvStatus.setText("Status: Connected");
                        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    });

                    gatt.discoverServices();

                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {

                    Log.d(TAG,"Disconnected from ESP32");

                    runOnUiThread(() -> {
                        tvStatus.setText("Status: Not Connected");
                        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    });
                }
            }

            @Override
            public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                BluetoothGattCharacteristic characteristic = gatt
                        .getService(SERVICE_UUID)
                        .getCharacteristic(CHARACTERISTIC_UUID);

                // Enable Notifications
                gatt.setCharacteristicNotification(characteristic, true);
                Log.d(TAG, "Notifications enabled!");            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                final String value = new String(characteristic.getValue());

                //we receive data but we don't display it
                Log.d(TAG, "Value received: " + value);            }
        });
    }


    private void reconnectDevice() {

        Log.d(TAG, "Reconnection attempt in " + reconnectDelay + " ms");
        new android.os.Handler().postDelayed(() -> {

            if (esp32Device != null) {
                connectToDevice();
                reconnectDelay *= 2; // exponential backoff
            }

        }, reconnectDelay);
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
                Log.d(TAG, "BLE permissions denied!");            }
        }
    }
}