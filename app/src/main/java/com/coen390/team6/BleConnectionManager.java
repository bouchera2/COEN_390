package com.coen390.team6;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Set;
import java.util.UUID;

public class BleConnectionManager {
    public interface ConnectionListener {
        void onConnecting();
        void onConnected();
        void onDisconnected();
        void onDeviceUnavailable();
        void onPermissionsMissing();
    }

    public static final int PERMISSION_REQUEST_CODE = 1002;
    private static final String TARGET_DEVICE_NAME = "esp32_bracelet_test";
    private static final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("abcd1234-1234-1234-1234-1234567890ab");

    private final Context appContext;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice targetDevice;

    public BleConnectionManager(Context context) {
        appContext = context.getApplicationContext();
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }

    public boolean hasRequiredPermissions(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }

        boolean hasConnect = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasScan = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED;
        return hasConnect && hasScan;
    }

    public String[] getRequiredPermissions() {
        return new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
    }

    public boolean prepareTargetDevice(Context context) {
        if (!hasRequiredPermissions(context) || bluetoothAdapter == null) {
            targetDevice = null;
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        targetDevice = null;
        for (BluetoothDevice device : bondedDevices) {
            if (TARGET_DEVICE_NAME.equals(device.getName())) {
                targetDevice = device;
                break;
            }
        }
        return targetDevice != null;
    }

    public void connect(Context context, ConnectionListener listener) {
        if (!hasRequiredPermissions(context)) {
            listener.onPermissionsMissing();
            return;
        }

        if (targetDevice == null && !prepareTargetDevice(context)) {
            listener.onDeviceUnavailable();
            return;
        }

        listener.onConnecting();
        bluetoothGatt = targetDevice.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    listener.onConnected();
                    gatt.discoverServices();
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    listener.onDisconnected();
                }
            }

            @Override
            public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (gatt.getService(SERVICE_UUID) == null) {
                    return;
                }
                BluetoothGattCharacteristic characteristic = gatt
                        .getService(SERVICE_UUID)
                        .getCharacteristic(CHARACTERISTIC_UUID);
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true);
                }
            }
        });
    }

    public void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
