package com.coen390.team6;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverLogActivity extends AppCompatActivity {

    private static final int SELECTED_STROKE_WIDTH_DP = 2;
    private static final int UNSELECTED_STROKE_WIDTH_DP = 2;

    private View navDashboardItem;
    private View navAlertsItem;
    private View navLogItem;
    private View navSettingsItem;
    private TextView tabHistory;
    private MaterialCardView cardScheduledRest;
    private MaterialCardView cardUnscheduledStop;
    private MaterialCardView cardIncident;
    private MaterialCardView cardRoadClosure;
    private MaterialCardView cardAttachments;
    private RadioButton radioScheduledRest;
    private RadioButton radioUnscheduledStop;
    private RadioButton radioIncident;
    private RadioButton radioRoadClosure;
    private ImageView iconScheduledRest;
    private ImageView iconUnscheduledStop;
    private ImageView iconIncident;
    private ImageView iconRoadClosure;
    private TextView textAttachmentLabel;
    private TextInputEditText editTextNotes;
    private Button buttonSaveLog;
    private final List<String> selectedAttachmentUris = new ArrayList<>();

    private final ActivityResultLauncher<Intent> attachmentPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }

                Intent data = result.getData();
                List<Uri> selectedUris = new ArrayList<>();

                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        selectedUris.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null) {
                    selectedUris.add(data.getData());
                }

                if (selectedUris.isEmpty()) {
                    return;
                }

                if (selectedUris.size() == 1) {
                    textAttachmentLabel.setText("1 file selected");
                } else {
                    textAttachmentLabel.setText(selectedUris.size() + " files selected");
                }

                selectedAttachmentUris.clear();
                for (Uri uri : selectedUris) {
                    selectedAttachmentUris.add(uri.toString());
                }

                Toast.makeText(this, "Attachment selected", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_log);

        bindViews();
        bindNavigation();
        bindLogTypeSelection();
        bindAttachmentPicker();
        bindActions();
        selectLogType(radioScheduledRest);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        navDashboardItem = findViewById(R.id.navDashboardItem);
        navAlertsItem = findViewById(R.id.navAlertsItem);
        navLogItem = findViewById(R.id.navLogItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
        tabHistory = findViewById(R.id.tabHistory);
        cardScheduledRest = findViewById(R.id.cardScheduledRest);
        cardUnscheduledStop = findViewById(R.id.cardUnscheduledStop);
        cardIncident = findViewById(R.id.cardIncident);
        cardRoadClosure = findViewById(R.id.cardRoadClosure);
        cardAttachments = findViewById(R.id.cardAttachments);
        radioScheduledRest = findViewById(R.id.radioScheduledRest);
        radioUnscheduledStop = findViewById(R.id.radioUnscheduledStop);
        radioIncident = findViewById(R.id.radioIncident);
        radioRoadClosure = findViewById(R.id.radioRoadClosure);
        iconScheduledRest = findViewById(R.id.iconScheduledRest);
        iconUnscheduledStop = findViewById(R.id.iconUnscheduledStop);
        iconIncident = findViewById(R.id.iconIncident);
        iconRoadClosure = findViewById(R.id.iconRoadClosure);
        textAttachmentLabel = findViewById(R.id.textAttachmentLabel);
        editTextNotes = findViewById(R.id.editTextNotes);
        buttonSaveLog = findViewById(R.id.buttonSaveLog);
    }

    private void bindNavigation() {
        navDashboardItem.setOnClickListener(v -> {
            Intent intent = new Intent(DriverLogActivity.this, GpsNavigationActivity.class);
            startActivity(intent);
            finish();
        });

        navLogItem.setOnClickListener(v -> {
            // Already on the log tab.
        });

        navAlertsItem.setOnClickListener(v -> {
            Intent intent = new Intent(DriverLogActivity.this, AlertsActivity.class);
            startActivity(intent);
            finish();
        });

        navSettingsItem.setOnClickListener(v -> {
            Intent intent = new Intent(DriverLogActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });

        tabHistory.setOnClickListener(v -> {
            Intent intent = new Intent(DriverLogActivity.this, DriverLogHistoryActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void bindLogTypeSelection() {
        cardScheduledRest.setOnClickListener(v -> selectLogType(radioScheduledRest));
        cardUnscheduledStop.setOnClickListener(v -> selectLogType(radioUnscheduledStop));
        cardIncident.setOnClickListener(v -> selectLogType(radioIncident));
        cardRoadClosure.setOnClickListener(v -> selectLogType(radioRoadClosure));

        radioScheduledRest.setOnClickListener(v -> selectLogType(radioScheduledRest));
        radioUnscheduledStop.setOnClickListener(v -> selectLogType(radioUnscheduledStop));
        radioIncident.setOnClickListener(v -> selectLogType(radioIncident));
        radioRoadClosure.setOnClickListener(v -> selectLogType(radioRoadClosure));
    }

    private void bindAttachmentPicker() {
        cardAttachments.setOnClickListener(v -> showAttachmentPickerSheet());
    }

    private void bindActions() {
        buttonSaveLog.setOnClickListener(v -> saveLog());
    }

    private void showAttachmentPickerSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        LinearLayout sheetLayout = new LinearLayout(this);
        sheetLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        sheetLayout.setPadding(padding, padding, padding, padding);

        TextView titleView = new TextView(this);
        titleView.setText("Add Attachment");
        titleView.setTextSize(18f);
        titleView.setTextColor(ContextCompat.getColor(this,
                isNightMode() ? R.color.log_bottom_sheet_text_dark : R.color.log_bottom_sheet_text_light));
        titleView.setPadding(0, 0, 0, dpToPx(12));
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);

        Button photoButton = new Button(this);
        photoButton.setText("Choose Photo");
        photoButton.setAllCaps(false);
        photoButton.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openAttachmentPicker(new String[]{"image/*"});
        });

        Button fileButton = new Button(this);
        fileButton.setText("Choose File");
        fileButton.setAllCaps(false);
        fileButton.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openAttachmentPicker(new String[]{"application/pdf", "text/plain", "*/*"});
        });

        sheetLayout.addView(titleView);
        sheetLayout.addView(photoButton);
        sheetLayout.addView(fileButton);

        bottomSheetDialog.setContentView(sheetLayout);
        bottomSheetDialog.show();
    }

    private void openAttachmentPicker(String[] mimeTypes) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        attachmentPickerLauncher.launch(intent);
    }

    private void saveLog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to save logs", Toast.LENGTH_SHORT).show();
            return;
        }

        String notes = "";
        if (editTextNotes.getText() != null) {
            notes = editTextNotes.getText().toString().trim();
        }

        String logType = getSelectedLogType();
        if ("Accident / Incident".equals(logType) && notes.isEmpty()) {
            editTextNotes.setError("Notes are required for incidents");
            editTextNotes.requestFocus();
            return;
        }

        setButtonsEnabled(false);

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("driverId", currentUser.getUid());
        logEntry.put("logType", logType);
        logEntry.put("notes", notes);
        logEntry.put("status", "submitted");
        logEntry.put("attachmentCount", selectedAttachmentUris.size());
        logEntry.put("attachmentUris", new ArrayList<>(selectedAttachmentUris));
        logEntry.put("createdAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(currentUser.getUid())
                .collection("driver_logs")
                .add(logEntry)
                .addOnSuccessListener(documentReference -> {
                    setButtonsEnabled(true);
                    Toast.makeText(this, "Log saved and sent to manager", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setButtonsEnabled(true);
                    Toast.makeText(this, "Could not save log", Toast.LENGTH_SHORT).show();
                });
    }

    private void selectLogType(RadioButton selectedRadio) {
        boolean isDarkTheme = isNightMode();

        updateLogTypeCard(cardScheduledRest, radioScheduledRest, iconScheduledRest,
                selectedRadio == radioScheduledRest, isDarkTheme);
        updateLogTypeCard(cardUnscheduledStop, radioUnscheduledStop, iconUnscheduledStop,
                selectedRadio == radioUnscheduledStop, isDarkTheme);
        updateLogTypeCard(cardIncident, radioIncident, iconIncident,
                selectedRadio == radioIncident, isDarkTheme);
        updateLogTypeCard(cardRoadClosure, radioRoadClosure, iconRoadClosure,
                selectedRadio == radioRoadClosure, isDarkTheme);
    }

    private String getSelectedLogType() {
        if (radioScheduledRest.isChecked()) {
            return "Scheduled Rest";
        }
        if (radioUnscheduledStop.isChecked()) {
            return "Unscheduled Stop";
        }
        if (radioIncident.isChecked()) {
            return "Accident / Incident";
        }
        return "Road Closure";
    }

    private void updateLogTypeCard(MaterialCardView card, RadioButton radioButton, ImageView icon,
                                   boolean selected, boolean isDarkTheme) {
        int primary = ContextCompat.getColor(this, R.color.log_primary);
        int selectedBackground = ContextCompat.getColor(this, isDarkTheme
                ? R.color.log_dark_selected_background
                : R.color.log_light_selected_background);
        int unselectedBackground = ContextCompat.getColor(this, isDarkTheme
                ? R.color.log_dark_unselected_background
                : R.color.log_light_unselected_background);
        int selectedStroke = ContextCompat.getColor(this, isDarkTheme
                ? R.color.log_dark_selected_stroke
                : R.color.log_light_selected_stroke);
        int unselectedStroke = ContextCompat.getColor(this, isDarkTheme
                ? R.color.log_dark_unselected_stroke
                : R.color.log_light_unselected_stroke);
        int textColor = ContextCompat.getColor(this, isDarkTheme
                ? R.color.log_dark_text
                : R.color.log_light_text);
        int muted = ContextCompat.getColor(this, R.color.log_muted);

        radioButton.setChecked(selected);
        radioButton.setButtonDrawable(null);
        radioButton.setTextColor(textColor);
        icon.setColorFilter(selected ? primary : muted);
        card.setCardBackgroundColor(selected ? selectedBackground : unselectedBackground);
        card.setStrokeColor(selected ? selectedStroke : unselectedStroke);
        card.setStrokeWidth(dpToPx(selected ? SELECTED_STROKE_WIDTH_DP : UNSELECTED_STROKE_WIDTH_DP));
    }

    private boolean isNightMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setButtonsEnabled(boolean enabled) {
        buttonSaveLog.setEnabled(enabled);
    }
}
