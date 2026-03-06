package com.coen390.team6;

import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Color;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private TextView heartrateText, fatigueText, bluetoothText, batteryText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bindViews();
        bindData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void bindViews() {
        heartrateText = findViewById(R.id.heartrateText);
        fatigueText = findViewById(R.id.fatigueText);
        bluetoothText = findViewById(R.id.bluetoothText);
        batteryText = findViewById(R.id.batteryText);
    }

    public void bindData() {
        heartrateText.setText(toString(R.string.heartrate_value));
        fatigueText.setText(toString(R.string.fatigue_value));
        bluetoothText.setText(toString(R.string.bluetooth_value));
        batteryText.setText(toString(R.string.battery_value));
        fatigueText.setTextColor(Color.parseColor("#4CAF50"));
    }
}