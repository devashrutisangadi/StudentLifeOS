package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task =
                        GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account);
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-up cancelled or failed", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        EditText etName = findViewById(R.id.etName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        ProgressBar progress = findViewById(R.id.signupProgress);

        findViewById(R.id.btnSignUp).setOnClickListener(v -> {
            String fullName = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (fullName.isEmpty() || email.isEmpty() || password.length() < 6) {
                Toast.makeText(this, "Fill all fields (password min 6 chars)", Toast.LENGTH_SHORT).show();
                return;
            }

            progress.setVisibility(View.VISIBLE);
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = auth.getCurrentUser().getUid();
                            createStudentDocument(uid, fullName, email, () -> {
                                progress.setVisibility(View.GONE);
                                startActivity(new Intent(this, DashboardActivity.class));
                                finish();
                            });
                        } else {
                            progress.setVisibility(View.GONE);
                            String msg = task.getException() != null
                                    ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, "Signup failed: " + msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        findViewById(R.id.btnGoogleSignUp).setOnClickListener(v ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent())
        );

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.tvLoginLink).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                String uid = user.getUid();
                String name = user.getDisplayName() != null ? user.getDisplayName() : "";
                String email = user.getEmail() != null ? user.getEmail() : "";

                FirebaseFirestore.getInstance().collection("students").document(uid).get()
                        .addOnSuccessListener(snapshot -> {
                            if (!snapshot.exists()) {
                                createStudentDocument(uid, name, email, () -> {
                                    startActivity(new Intent(this, DashboardActivity.class));
                                    finish();
                                });
                            } else {
                                startActivity(new Intent(this, DashboardActivity.class));
                                finish();
                            }
                        });
            } else {
                Toast.makeText(this, "Google sign-up failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Creates a students/{uid} document matching the new dataset's nested
     * shape (personal/academic/metrics), with sensible empty/zero defaults
     * for fields the sign-up form doesn't collect yet (roll number, CGPA,
     * attendance, etc. — those need a proper "complete your profile" or
     * admin-entry flow later).
     */
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
        // email is stored one level up for easy lookup/rules use, matching
        // where your Auth account's email lives — not nested, unlike the
        // synthetic dataset's login_credentials (which we deliberately don't
        // replicate, since credentials belong in Auth, not Firestore).
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
}