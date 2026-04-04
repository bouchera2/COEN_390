package com.coen390.team6;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertsActivity extends AppCompatActivity {
    public static final String EXTRA_DEFAULT_TAB = "default_tab";
    public static final String TAB_ACTIVE = "active";
    public static final String TAB_HISTORY = "history";

    private View navDashboardItem;
    private View navAlertsItem;
    private View navLogItem;
    private View navSettingsItem;
    private TextView tabActive;
    private TextView tabHistory;
    private View activeContent;
    private View historyContent;
    private MaterialCardView emptyActiveCard;
    private MaterialCardView emptyHistoryCard;
    private LinearLayout activeListContainer;
    private LinearLayout historyListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alerts);

        bindViews();
        bindNavigation();
        bindTabs();

        String defaultTab = getIntent().getStringExtra(EXTRA_DEFAULT_TAB);
        showTab(TAB_HISTORY.equals(defaultTab) ? TAB_HISTORY : TAB_ACTIVE);

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
        tabActive = findViewById(R.id.tabActive);
        tabHistory = findViewById(R.id.tabHistory);
        activeContent = findViewById(R.id.activeContent);
        historyContent = findViewById(R.id.historyContent);
        emptyActiveCard = findViewById(R.id.emptyActiveCard);
        emptyHistoryCard = findViewById(R.id.emptyHistoryCard);
        activeListContainer = findViewById(R.id.activeListContainer);
        historyListContainer = findViewById(R.id.historyListContainer);
    }

    private void bindNavigation() {
        navDashboardItem.setOnClickListener(v -> {
            startActivity(new Intent(this, GpsNavigationActivity.class));
            finish();
        });
        navAlertsItem.setOnClickListener(v -> { });
        navLogItem.setOnClickListener(v -> {
            startActivity(new Intent(this, DriverLogActivity.class));
            finish();
        });
        navSettingsItem.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        });
    }

    private void bindTabs() {
        tabActive.setOnClickListener(v -> showTab(TAB_ACTIVE));
        tabHistory.setOnClickListener(v -> showTab(TAB_HISTORY));
    }

    private void showTab(String tab) {
        boolean showActive = TAB_ACTIVE.equals(tab);
        activeContent.setVisibility(showActive ? View.VISIBLE : View.GONE);
        historyContent.setVisibility(showActive ? View.GONE : View.VISIBLE);

        tabActive.setSelected(showActive);
        tabHistory.setSelected(!showActive);
        tabActive.setBackgroundResource(showActive ? R.drawable.bg_alert_tab_selected : android.R.color.transparent);
        tabHistory.setBackgroundResource(showActive ? android.R.color.transparent : R.drawable.bg_alert_tab_selected);
        tabActive.setTextColor(Color.parseColor(showActive ? "#135BEC" : "#64748B"));
        tabHistory.setTextColor(Color.parseColor(showActive ? "#64748B" : "#135BEC"));
    }

    private void renderAlerts() {
        renderActiveAlerts(AlertRepository.getActiveAlerts(this));
        renderHistoryAlerts(AlertRepository.getResolvedAlerts(this));
    }

    private void renderActiveAlerts(List<AlertItem> alerts) {
        activeListContainer.removeAllViews();
        emptyActiveCard.setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);
        if (alerts.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (AlertItem item : alerts) {
            View itemView = inflater.inflate(R.layout.item_alert_active, activeListContainer, false);
            bindActiveItem(itemView, item);
            activeListContainer.addView(itemView);
        }
    }

    private void renderHistoryAlerts(List<AlertItem> alerts) {
        historyListContainer.removeAllViews();
        emptyHistoryCard.setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);
        if (alerts.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (AlertItem item : alerts) {
            View itemView = inflater.inflate(R.layout.item_alert_history, historyListContainer, false);
            bindHistoryItem(itemView, item);
            historyListContainer.addView(itemView);
        }
    }

    private void bindActiveItem(View itemView, AlertItem item) {
        TextView title = itemView.findViewById(R.id.alertTitle);
        TextView timestamp = itemView.findViewById(R.id.alertTimestamp);
        TextView message = itemView.findViewById(R.id.alertMessage);
        Button falseAlarmButton = itemView.findViewById(R.id.buttonFalseAlarm);
        Button resolveButton = itemView.findViewById(R.id.buttonResolve);

        title.setText(item.getTitle());
        timestamp.setText(formatTimestamp(item.getTimestamp()));
        message.setText(item.getMessage());

        falseAlarmButton.setOnClickListener(v -> {
            AlertRepository.resolveAlert(this, item.getId(), "Driver marked this alert as a false alarm.");
            renderAlerts();
            showTab(TAB_HISTORY);
        });

        resolveButton.setOnClickListener(v -> {
            AlertRepository.resolveAlert(this, item.getId(), "Driver acknowledged the alert and took a safety break.");
            renderAlerts();
            showTab(TAB_HISTORY);
        });
    }

    private void bindHistoryItem(View itemView, AlertItem item) {
        TextView title = itemView.findViewById(R.id.historyAlertTitle);
        TextView timestamp = itemView.findViewById(R.id.historyAlertTimestamp);
        TextView status = itemView.findViewById(R.id.historyAlertStatus);
        TextView message = itemView.findViewById(R.id.historyAlertMessage);
        TextView resolution = itemView.findViewById(R.id.historyResolutionNote);

        title.setText(item.getTitle());
        timestamp.setText(formatTimestamp(item.getTimestamp()));
        status.setText(item.getStatusLabel().toUpperCase(Locale.US));
        message.setText(item.getMessage());
        resolution.setText(item.getResolutionNote().isEmpty()
                ? "Alert closed without an additional note."
                : item.getResolutionNote());
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.US).format(new Date(timestamp));
    }
}
