package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        EditText etName = findViewById(R.id.etName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        ProgressBar progress = findViewById(R.id.signupProgress);

        findViewById(R.id.btnSignUp).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.length() < 6) {
                Toast.makeText(this, "Fill all fields (password min 6 chars)", Toast.LENGTH_SHORT).show();
                return;
            }

            progress.setVisibility(View.VISIBLE);
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = auth.getCurrentUser().getUid();
                            Map<String, Object> profile = new HashMap<>();
                            profile.put("uid", uid);
                            profile.put("displayName", name);
                            profile.put("email", email);
                            profile.put("role", "student");

                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .set(profile)
                                    .addOnCompleteListener(t -> {
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
    }
}