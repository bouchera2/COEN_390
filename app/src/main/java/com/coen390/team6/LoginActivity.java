package com.coen390.team6;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI elements
    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonSignUp;
    private SignInButton buttonGoogleSignIn;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // Google Sign-In launcher
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn);
        progressBar = findViewById(R.id.progressBar);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Email/Password Login
        buttonLogin.setOnClickListener(v -> loginWithEmail());

        // Email/Password Sign Up
        buttonSignUp.setOnClickListener(v -> signUpWithEmail());

        // Google Sign-In
        buttonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If user is already logged in, go straight to MainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
        }
    }

    // ==========================================
    // EMAIL / PASSWORD LOGIN
    // ==========================================

    private void loginWithEmail() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail: success");
                        goToMain();
                    } else {
                        Log.w(TAG, "signInWithEmail: failure", task.getException());
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ==========================================
    // EMAIL / PASSWORD SIGN UP
    // ==========================================

    private void signUpWithEmail() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail: success");
                        // Create driver profile in Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            createDriverProfile(user);
                        }
                        goToMain();
                    } else {
                        Log.w(TAG, "createUserWithEmail: failure", task.getException());
                        Toast.makeText(LoginActivity.this,
                                "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ==========================================
    // GOOGLE SIGN-IN
    // ==========================================

    private void signInWithGoogle() {
        showLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Google sign in success, authenticating with Firebase...");
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            showLoading(false);
            Log.w(TAG, "Google sign in failed", e);
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential: success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Create profile if first time
                        if (user != null && task.getResult().getAdditionalUserInfo().isNewUser()) {
                            createDriverProfile(user);
                        }
                        goToMain();
                    } else {
                        Log.w(TAG, "signInWithCredential: failure", task.getException());
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==========================================
    // FIRESTORE — Create Driver Profile
    // ==========================================

    private void createDriverProfile(FirebaseUser user) {
        // This creates a driver document in Firestore when a new user signs up
        java.util.Map<String, Object> driver = new java.util.HashMap<>();
        driver.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
        driver.put("email", user.getEmail() != null ? user.getEmail() : "");
        driver.put("phone", "");
        driver.put("driverId", user.getUid());
        driver.put("fleetManagerId", "");
        driver.put("isActive", true);
        driver.put("createdAt", com.google.firebase.Timestamp.now());

        // Default settings
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("fatigueWarningThreshold", 70);
        settings.put("fatigueCriticalThreshold", 85);
        settings.put("alertSound", "alarm_default");
        settings.put("vibrationEnabled", true);
        settings.put("darkModeEnabled", true);
        driver.put("settings", settings);

        // Emergency contact (empty by default)
        java.util.Map<String, Object> emergency = new java.util.HashMap<>();
        emergency.put("name", "");
        emergency.put("phone", "");
        driver.put("emergencyContact", emergency);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(user.getUid())
                .set(driver)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Driver profile created successfully"))
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error creating driver profile", e));
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!isLoading);
        buttonSignUp.setEnabled(!isLoading);
        buttonGoogleSignIn.setEnabled(!isLoading);
    }
}
