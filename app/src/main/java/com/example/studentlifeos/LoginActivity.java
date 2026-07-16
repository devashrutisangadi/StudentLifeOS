package com.example.studentlifeos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_SAVED_EMAIL = "saved_email";

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private boolean isPasswordVisible = false;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task =
                        GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account);
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-in cancelled or failed", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        ImageView ivTogglePassword = findViewById(R.id.ivTogglePassword);
        CheckBox cbRememberMe = findViewById(R.id.cbRememberMe);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ProgressBar progress = findViewById(R.id.loginProgress);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String savedEmail = prefs.getString(KEY_SAVED_EMAIL, null);
        if (savedEmail != null) {
            etEmail.setText(savedEmail);
            cbRememberMe.setChecked(true);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_eye_open);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_eye_closed);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }
            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Couldn't send reset email: " +
                                    (task.getException() != null ? task.getException().getMessage() : ""),
                            Toast.LENGTH_LONG).show();
                }
            });
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            progress.setVisibility(View.VISIBLE);
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progress.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            if (cbRememberMe.isChecked()) {
                                prefs.edit().putString(KEY_SAVED_EMAIL, email).apply();
                            } else {
                                prefs.edit().remove(KEY_SAVED_EMAIL).apply();
                            }
                            proceedAfterLogin();
                        } else {
                            String msg = task.getException() != null
                                    ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent())
        );

        findViewById(R.id.tvGoToSignup).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class))
        );
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                proceedAfterLogin();
            } else {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Checks whether this account already has a students/{uid} document.
     * If not (e.g. an account created before the schema migration, or a
     * first-time Google sign-in), auto-provisions one so nothing breaks.
     */
    private void proceedAfterLogin() {
        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        String email = auth.getCurrentUser().getEmail() != null
                                ? auth.getCurrentUser().getEmail() : "";
                        String name = auth.getCurrentUser().getDisplayName() != null
                                ? auth.getCurrentUser().getDisplayName() : "";
                        createStudentDocument(uid, name, email, this::goToDashboard);
                    } else {
                        goToDashboard();
                    }
                })
                .addOnFailureListener(e -> goToDashboard()); // fail-open rather than stranding the user
    }

    /** Same shape as SignUpActivity's version — see that file for the
     *  field-by-field reasoning; kept duplicated here for now. */
    private void createStudentDocument(String uid, String fullName, String email, Runnable onComplete) {
        String[] nameParts = fullName.trim().split("\\s+", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Map<String, Object> personal = new HashMap<>();
        personal.put("firstName", firstName);
        personal.put("lastName", lastName);
        personal.put("phone", "");
        personal.put("dob", "");
        personal.put("bloodGroup", "");
        personal.put("avatarUrl", "");

        Map<String, Object> academic = new HashMap<>();
        academic.put("university", "");
        academic.put("college", "");
        academic.put("degree", "");
        academic.put("branch", "");
        academic.put("semester", 1);
        academic.put("rollNumber", "");
        academic.put("enrollmentNumber", "");

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpi", 0.0);
        metrics.put("spiHistory", new ArrayList<Double>());
        metrics.put("totalCreditsEarned", 0);
        metrics.put("backlogs", 0);
        metrics.put("overallAttendance", 0.0);

        Map<String, Object> student = new HashMap<>();
        student.put("personal", personal);
        student.put("academic", academic);
        student.put("metrics", metrics);
        student.put("email", email);

        FirebaseFirestore.getInstance().collection("students").document(uid)
                .set(student)
                .addOnSuccessListener(unused -> onComplete.run())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't create profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Still proceed rather than stranding the user on a blank screen —
                    // but now you'll actually SEE why it failed, via the Toast above.
                    onComplete.run();
                });
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}