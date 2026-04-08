package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DriverLogHistoryActivity extends AppCompatActivity {

    private View navDashboardItem;
    private View navAlertsItem;
    private View navLogItem;
    private View navSettingsItem;
    private TextView tabLogEvent;
    private MaterialCardView emptyHistoryCard;
    private LinearLayout historyListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_log_history);

        bindViews();
        bindNavigation();
        loadHistory();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void bindViews() {
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navAlertsItem = findViewById(R.id.navAlertsItem);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
        tabLogEvent = findViewById(R.id.tabLogEvent);
        emptyHistoryCard = findViewById(R.id.emptyHistoryCard);
        historyListContainer = findViewById(R.id.historyListContainer);
    }

    private void bindNavigation() {
        tabLogEvent.setOnClickListener(v -> {
            Intent intent = new Intent(DriverLogHistoryActivity.this, DriverLogActivity.class);
            startActivity(intent);
            finish();
        });

        navDashboardItem.setOnClickListener(v -> {
            startActivity(NavigationIntentFactory.createGpsIntent(this));
            finish();
        });

        navLogItem.setOnClickListener(v -> {
            // Already inside the log section.
        });

        if (navAlertsItem != null) {
            navAlertsItem.setOnClickListener(v ->
                    startActivity(new Intent(this, AlertsActivity.class)));
        }

        navSettingsItem.setOnClickListener(v -> {
            Intent intent = new Intent(DriverLogHistoryActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadHistory() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showEmptyState();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(currentUser.getUid())
                .collection("driver_logs")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyListContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    emptyHistoryCard.setVisibility(View.GONE);
                    LayoutInflater inflater = LayoutInflater.from(this);
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        View itemView = inflater.inflate(R.layout.item_driver_log_history, historyListContainer, false);
                        bindHistoryItem(itemView, document);
                        historyListContainer.addView(itemView);
                    }
                })
                .addOnFailureListener(e -> {
                    showEmptyState();
                    Toast.makeText(this, "Could not load log history", Toast.LENGTH_SHORT).show();
                });
    }

    private void bindHistoryItem(View itemView, QueryDocumentSnapshot document) {
        TextView historyDate = itemView.findViewById(R.id.historyDate);
        TextView historyStatus = itemView.findViewById(R.id.historyStatus);
        TextView historyLogType = itemView.findViewById(R.id.historyLogType);
        TextView historyAttachmentCount = itemView.findViewById(R.id.historyAttachmentCount);
        TextView historyNotePreview = itemView.findViewById(R.id.historyNotePreview);
        Button historyViewButton = itemView.findViewById(R.id.historyViewButton);

        String status = valueOrFallback(document.getString("status"), "saved");
        String logType = valueOrFallback(document.getString("logType"), "Unknown Event");
        String notes = valueOrFallback(document.getString("notes"), "No notes");
        Long attachmentCount = document.getLong("attachmentCount");
        Timestamp createdAt = document.getTimestamp("createdAt");

        historyDate.setText(formatTimestamp(createdAt));
        historyStatus.setText(status.toUpperCase(Locale.US));
        historyLogType.setText(logType);
        historyAttachmentCount.setText(formatAttachmentCount(attachmentCount));
        historyNotePreview.setText(notes.isBlank() ? "No notes" : notes);
        historyViewButton.setOnClickListener(v ->
                Toast.makeText(this, "Detailed log view can be added next", Toast.LENGTH_SHORT).show());
    }

    private void showEmptyState() {
        emptyHistoryCard.setVisibility(View.VISIBLE);
        historyListContainer.removeAllViews();
    }

    private String formatTimestamp(Timestamp timestamp) {
        Date date = timestamp != null ? timestamp.toDate() : new Date();
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(date);
    }

    private String formatAttachmentCount(Long attachmentCount) {
        long count = attachmentCount != null ? attachmentCount : 0L;
        return count == 1 ? "1 file" : count + " files";
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
