package com.coen390.team6;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private Button btnONOFF;
    private static final String TAG = "MainActivity";

    private final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private final UUID CHARACTERISTIC_UUID = UUID.fromString("abcd1234-1234-1234-1234-1234567890ab");
    private final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, "Bluetooth OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(TAG, "Bluetooth turning OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(TAG, "Bluetooth ON");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, "Bluetooth turning ON");
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(btReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btnONOFF = findViewById(R.id.btnONOFF);

        btnONOFF.setOnClickListener(v -> {
            Log.d(TAG, "Clicked: toggle bluetooth");
            toggleBluetooth();
        });
    }

    private void toggleBluetooth() {
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Device does not support Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        } else {
            bluetoothAdapter.disable();
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btReceiver, filter);
    }
}