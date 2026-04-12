package com.coen390.team6;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertsActivity extends AppCompatActivity {

    private View navDashboardItem;
    private View navAlertsItem;
    private View navLogItem;
    private View navSettingsItem;
    private View emptyHistoryCard;
    private LinearLayout historyListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alerts);

        bindViews();
        bindNavigation();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderAlerts();
    }

    private void bindViews() {
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navAlertsItem = findViewById(R.id.navAlertsItem);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
        emptyHistoryCard = findViewById(R.id.emptyHistoryCard);
        historyListContainer = findViewById(R.id.historyListContainer);
    }

    private void bindNavigation() {
        if (navDashboardItem != null) {
            navDashboardItem.setOnClickListener(v -> {
                startActivity(NavigationIntentFactory.createGpsIntent(this));
                finish();
            });
        }

        if (navAlertsItem != null) {
            navAlertsItem.setOnClickListener(v -> {
                // Already here.
            });
        }

        if (navLogItem != null) {
            navLogItem.setOnClickListener(v -> {
                startActivity(new Intent(this, DriverLogActivity.class));
                finish();
            });
        }

        if (navSettingsItem != null) {
            navSettingsItem.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
            });
        }
    }

    private void renderAlerts() {
        historyListContainer.removeAllViews();

        JSONArray history = AlertHistoryPreferences.getHistoryAlerts(this);
        if (history.length() == 0) {
            emptyHistoryCard.setVisibility(View.VISIBLE);
        } else {
            emptyHistoryCard.setVisibility(View.GONE);
            for (int i = 0; i < history.length(); i++) {
                JSONObject item = history.optJSONObject(i);
                if (item != null) {
                    historyListContainer.addView(createAlertView(item, false));
                }
            }
        }
    }

    private View createAlertView(JSONObject alert, boolean isActive) {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_alert_entry, historyListContainer, false);

        FrameLayout iconContainer = itemView.findViewById(R.id.iconContainer);
        TextView iconText = itemView.findViewById(R.id.iconText);
        TextView titleText = itemView.findViewById(R.id.titleText);
        TextView subtitleText = itemView.findViewById(R.id.subtitleText);
        TextView statusChip = itemView.findViewById(R.id.statusChip);
        TextView messageText = itemView.findViewById(R.id.messageText);

        titleText.setText(alert.optString("title", "Fatigue Alert"));
        subtitleText.setText(formatAlertSubtitle(alert, isActive));

        if (isActive) {
            statusChip.setText("Active");
            statusChip.setTextColor(Color.parseColor("#EF4444"));
            statusChip.setBackgroundColor(Color.parseColor("#1AEF4444"));
            iconContainer.setBackgroundColor(Color.parseColor("#1AEF4444"));
            iconText.setTextColor(Color.parseColor("#EF4444"));
            iconText.setText("⚠");
            String message = alert.optString("message",
                    "Critical fatigue detected. Open the alert to guide the driver to a safe stop.");
            int score = alert.optInt("score", 0);
            int heartRate = alert.optInt("heartRate", 0);
            messageText.setText(message + " Score: " + score + "/100" + (heartRate > 0 ? " • HR: " + heartRate + " BPM" : ""));
        } else {
            String status = alert.optString("status", "Resolved");
            statusChip.setText(status);
            if ("Dismissed".equalsIgnoreCase(status)) {
                statusChip.setTextColor(Color.parseColor("#94A3B8"));
                statusChip.setBackgroundColor(Color.parseColor("#1A94A3B8"));
                iconContainer.setBackgroundColor(Color.parseColor("#0F172A"));
                iconText.setTextColor(Color.parseColor("#94A3B8"));
                iconText.setText("✓");
            } else {
                statusChip.setTextColor(Color.parseColor("#22C55E"));
                statusChip.setBackgroundColor(Color.parseColor("#1A22C55E"));
                iconContainer.setBackgroundColor(Color.parseColor("#1A135BEC"));
                iconText.setTextColor(Color.parseColor("#135BEC"));
                iconText.setText("⚠");
            }
            String resolutionMessage = alert.optString("resolutionMessage",
                    "Driver acknowledged the alert and the event was archived.");
            messageText.setText(resolutionMessage);
        }

        return itemView;
    }

    private String formatTimestamp(long timestampMs) {
        return new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(new Date(timestampMs));
    }

    private String formatAlertSubtitle(JSONObject alert, boolean isActive) {
        long triggeredAtMs = alert.optLong("timestampMs", 0L);
        if (triggeredAtMs <= 0L) {
            return isActive ? "ACTIVE ALERT" : "ALERT HISTORY";
        }

        String triggeredText = "Triggered " + formatTimestamp(triggeredAtMs);
        if (isActive) {
            return triggeredText;
        }

        long resolvedAtMs = alert.optLong("resolvedAtMs", 0L);
        if (resolvedAtMs > 0L) {
            return triggeredText + "  •  Resolved " + formatTimestamp(resolvedAtMs);
        }
        return triggeredText;
    }
}
