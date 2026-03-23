package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonSignUp, buttonGoogleSignIn;
    private Button tabSignIn, tabRegister;
    private ProgressBar progressBar;

    private boolean isSignInMode = true;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // UI
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn);
        tabSignIn = findViewById(R.id.tabSignIn);
        tabRegister = findViewById(R.id.tabRegister);
        progressBar = findViewById(R.id.progressBar);

        // Google Sign-In config
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Tab switching
        tabSignIn.setOnClickListener(v -> switchToSignIn());
        tabRegister.setOnClickListener(v -> switchToRegister());

        // Actions
        buttonLogin.setOnClickListener(v -> loginWithEmail());
        buttonSignUp.setOnClickListener(v -> signUpWithEmail());
        buttonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Start in Sign In mode
        switchToSignIn();
    }

    private void switchToSignIn() {
        isSignInMode = true;
        tabSignIn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3B4A6B));
        tabSignIn.setTextColor(0xFFFFFFFF);
        tabRegister.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x00000000));
        tabRegister.setTextColor(0xFF8896AB);
        buttonLogin.setVisibility(View.VISIBLE);
        buttonSignUp.setVisibility(View.GONE);
    }

    private void switchToRegister() {
        isSignInMode = false;
        tabRegister.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3B4A6B));
        tabRegister.setTextColor(0xFFFFFFFF);
        tabSignIn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x00000000));
        tabSignIn.setTextColor(0xFF8896AB);
        buttonLogin.setVisibility(View.GONE);
        buttonSignUp.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
        }
    }

    private void loginWithEmail() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty()) { editTextEmail.setError("Email is required"); editTextEmail.requestFocus(); return; }
        if (password.isEmpty()) { editTextPassword.setError("Password is required"); editTextPassword.requestFocus(); return; }
        if (password.length() < 6) { editTextPassword.setError("Min 6 characters"); editTextPassword.requestFocus(); return; }

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signUpWithEmail() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty()) { editTextEmail.setError("Email is required"); editTextEmail.requestFocus(); return; }
        if (password.isEmpty()) { editTextPassword.setError("Password is required"); editTextPassword.requestFocus(); return; }
        if (password.length() < 6) { editTextPassword.setError("Min 6 characters"); editTextPassword.requestFocus(); return; }

        showLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) createDriverProfile(user);
                        goToMain();
                    } else {
                        Toast.makeText(this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        showLoading(true);
        googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            showLoading(false);
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && task.getResult().getAdditionalUserInfo().isNewUser()) {
                            createDriverProfile(user);
                        }
                        goToMain();
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createDriverProfile(FirebaseUser user) {
        java.util.Map<String, Object> driver = new java.util.HashMap<>();
        driver.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
        driver.put("email", user.getEmail() != null ? user.getEmail() : "");
        driver.put("phone", "");
        driver.put("driverId", user.getUid());
        driver.put("fleetManagerId", "");
        driver.put("isActive", true);
        driver.put("createdAt", com.google.firebase.Timestamp.now());

        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("fatigueWarningThreshold", 70);
        settings.put("fatigueCriticalThreshold", 85);
        settings.put("alertSound", "alarm_default");
        settings.put("vibrationEnabled", true);
        settings.put("darkModeEnabled", true);
        driver.put("settings", settings);

        java.util.Map<String, Object> emergency = new java.util.HashMap<>();
        emergency.put("name", "");
        emergency.put("phone", "");
        driver.put("emergencyContact", emergency);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(user.getUid())
                .set(driver)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Driver profile created"))
                .addOnFailureListener(e -> Log.w(TAG, "Error creating profile", e));
    }

    private void goToMain() {
        String uid = mAuth.getCurrentUser().getUid();
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("drivers").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Intent intent;
                    if (doc.exists() && doc.getBoolean("profileComplete") != null
                            && doc.getBoolean("profileComplete")) {
                        // Profile complete — go to BLE connection screen first.
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    } else {
                        // Profile not complete — go to setup
                        intent = new Intent(LoginActivity.this, DriverProfileSetupActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Firestore error — go to profile setup as fallback
                    Intent intent = new Intent(LoginActivity.this, DriverProfileSetupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!isLoading);
        buttonSignUp.setEnabled(!isLoading);
        buttonGoogleSignIn.setEnabled(!isLoading);
    }
}
