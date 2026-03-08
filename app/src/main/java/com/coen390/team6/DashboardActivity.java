package com.coen390.team6;

import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Color;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DashboardActivity extends AppCompatActivity {

    private TextView heartRateText, fatigueText, bluetoothText, batteryText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        bindText();
        bindData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void bindText() {
        heartRateText = findViewById(R.id.heartRateText);
        fatigueText = findViewById(R.id.fatigueText);
        bluetoothText = findViewById(R.id.bluetoothText);
        batteryText = findViewById(R.id.batteryText);
    }

    public void bindData() {
        heartRateText.setText(getString(R.string.heartrate_value_placeholder));
        fatigueText.setText(getString(R.string.fatigue_value_placeholder));
        bluetoothText.setText(getString(R.string.bt_status_placeholder));
        batteryText.setText(getString(R.string.battery_value_placeholder));
        fatigueText.setTextColor(Color.parseColor("#4CAF50"));
    }
}