package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class DriverProfileSetupActivity extends AppCompatActivity {
    public static final String EXTRA_EDIT_MODE = "edit_mode";

    private EditText etFullName, etAge, etWeight, etHeight, etRestingHR, etEmergencyContact;
    private FirebaseFirestore db;
    private String uid;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile_setup);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        isEditMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);

        etFullName = findViewById(R.id.etFullName);
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etRestingHR = findViewById(R.id.etRestingHR);
        etEmergencyContact = findViewById(R.id.etEmergencyContact);

        View btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvProfileSetupTitle);
        TextView tvSubtitle = findViewById(R.id.tvProfileSetupSubtitle);
        View progressSection = findViewById(R.id.progressSection);
        View progressBar = findViewById(R.id.progressBarProfile);
        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnSkip = findViewById(R.id.btnSkip);

        btnBack.setOnClickListener(v -> finish());

        if (isEditMode) {
            tvTitle.setText("Edit Personal Information");
            tvSubtitle.setText("Update the driver profile used for personalized monitoring.");
            progressSection.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            btnSave.setText("Save Changes");
            btnSkip.setText("Cancel");
            loadExistingProfile();
        }

        btnSave.setOnClickListener(v -> saveProfile());
        btnSkip.setOnClickListener(v -> {
            if (isEditMode) {
                finish();
            } else {
                goToMain();
            }
        });
    }

    private void saveProfile() {
        // Validate all fields
        if (!InputValidator.validateNotEmpty(etFullName, "Full Name")) return;
        if (!InputValidator.validateNumber(etAge, "Age", 16, 80)) return;
        if (!InputValidator.validateNumber(etWeight, "Weight", 30, 300)) return;
        if (!InputValidator.validateNumber(etHeight, "Height", 100, 250)) return;
        if (!InputValidator.validatePhone(etEmergencyContact)) return;

        // Optional resting HR validation (if not empty)
        String restingHRStr = etRestingHR.getText().toString().trim();
        if (!restingHRStr.isEmpty()) {
            if (!InputValidator.validateNumber(etRestingHR, "Resting HR", 40, 120)) return;
        }
        String name = etFullName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String emergencyPhone = etEmergencyContact.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            etFullName.setError("Name is required");
            etFullName.requestFocus();
            return;
        }
        if (ageStr.isEmpty()) {
            etAge.setError("Age is required");
            etAge.requestFocus();
            return;
        }
        if (weightStr.isEmpty()) {
            etWeight.setError("Weight is required");
            etWeight.requestFocus();
            return;
        }
        if (heightStr.isEmpty()) {
            etHeight.setError("Height is required");
            etHeight.requestFocus();
            return;
        }

        int age = Integer.parseInt(ageStr);
        double weight = Double.parseDouble(weightStr);
        double height = Double.parseDouble(heightStr);
        int restingHR = restingHRStr.isEmpty() ? 70 : Integer.parseInt(restingHRStr);

        // Calculate heart rate zones using Karvonen formula
        // Max HR = 220 - age
        int maxHR = 220 - age;
        int hrReserve = maxHR - restingHR;

        // Normal driving zone (40-60% of HR reserve)
        int normalHRLow = (int) (restingHR + 0.4 * hrReserve);
        int normalHRHigh = (int) (restingHR + 0.6 * hrReserve);

        // Fatigue warning zone (below resting + 10% reserve = drowsy/falling asleep)
        int fatigueHRThreshold = (int) (restingHR + 0.1 * hrReserve);

        // Stress/alert zone (above 70% = stressed or panicking)
        int stressHRThreshold = (int) (restingHR + 0.7 * hrReserve);

        DriverProfilePreferences.saveThresholds(
                this,
                restingHR,
                normalHRLow,
                normalHRHigh,
                fatigueHRThreshold,
                stressHRThreshold
        );

        // Build Firestore document
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("age", age);
        profile.put("weight", weight);
        profile.put("height", height);
        profile.put("restingHeartRate", restingHR);
        profile.put("maxHeartRate", maxHR);
        profile.put("normalHRLow", normalHRLow);
        profile.put("normalHRHigh", normalHRHigh);
        profile.put("fatigueHRThreshold", fatigueHRThreshold);
        profile.put("stressHRThreshold", stressHRThreshold);
        profile.put("profileComplete", true);

        // Emergency contact
        Map<String, Object> emergency = new HashMap<>();
        emergency.put("phone", emergencyPhone);
        profile.put("emergencyContact", emergency);

        db.collection("drivers").document(uid).update(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(
                            this,
                            isEditMode
                                    ? "Profile updated."
                                    : "Profile saved! Max HR: " + maxHR + " | Normal zone: "
                                    + normalHRLow + "-" + normalHRHigh + " BPM",
                            Toast.LENGTH_LONG
                    ).show();
                    if (isEditMode) {
                        finish();
                    } else {
                        goToMain();
                    }
                })
                .addOnFailureListener(e -> {
                    // If document doesn't exist yet, use set instead of update
                    profile.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
                    profile.put("driverId", uid);
                    profile.put("isActive", true);
                    profile.put("createdAt", com.google.firebase.Timestamp.now());

                    Map<String, Object> settings = new HashMap<>();
                    settings.put("fatigueWarningThreshold", 70);
                    settings.put("fatigueCriticalThreshold", 85);
                    settings.put("alertSound", "alarm_default");
                    settings.put("vibrationEnabled", true);
                    settings.put("darkModeEnabled", true);
                    profile.put("settings", settings);

                    db.collection("drivers").document(uid).set(profile)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(
                                        this,
                                        isEditMode ? "Profile updated." : "Profile created!",
                                        Toast.LENGTH_SHORT
                                ).show();
                                if (isEditMode) {
                                    finish();
                                } else {
                                    goToMain();
                                }
                            })
                            .addOnFailureListener(err ->
                                    Toast.makeText(this, "Error: " + err.getMessage(), Toast.LENGTH_LONG).show());
                });
    }

    private void loadExistingProfile() {
        db.collection("drivers").document(uid).get()
                .addOnSuccessListener(this::populateFields)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Unable to load profile.", Toast.LENGTH_SHORT).show());
    }

    private void populateFields(DocumentSnapshot doc) {
        if (!doc.exists()) {
            return;
        }

        String name = doc.getString("name");
        Long age = doc.getLong("age");
        Double weight = doc.getDouble("weight");
        Double height = doc.getDouble("height");
        Long restingHr = doc.getLong("restingHeartRate");

        Object emergency = doc.get("emergencyContact");
        String emergencyPhone = "";
        if (emergency instanceof Map) {
            Object phone = ((Map<?, ?>) emergency).get("phone");
            if (phone != null) {
                emergencyPhone = String.valueOf(phone);
            }
        }

        etFullName.setText(name != null ? name : "");
        etAge.setText(age != null ? String.valueOf(age) : "");
        etWeight.setText(weight != null ? formatDecimal(weight) : "");
        etHeight.setText(height != null ? formatDecimal(height) : "");
        etRestingHR.setText(restingHr != null ? String.valueOf(restingHr) : "");
        etEmergencyContact.setText(emergencyPhone);
    }

    private String formatDecimal(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
