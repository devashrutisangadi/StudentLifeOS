package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etBranch, etSemester, etRollNumber, etCgpa, etContact, etUniversity;
    private ProgressBar progress;

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        etName = findViewById(R.id.etName);
        etBranch = findViewById(R.id.etBranch);
        etSemester = findViewById(R.id.etSemester);
        etRollNumber = findViewById(R.id.etRollNumber);
        etCgpa = findViewById(R.id.etCgpa);
        etContact = findViewById(R.id.etContact);
        etUniversity = findViewById(R.id.etUniversity);
        progress = findViewById(R.id.editProfileProgress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());

        prefillFields();
    }

    private void prefillFields() {
        if (user == null) return;

        etName.setText(user.getDisplayName());

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(this::bindExistingValues)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load existing profile", Toast.LENGTH_SHORT).show());
    }

    private void bindExistingValues(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;

        if (TextUtils.isEmpty(etName.getText())) {
            String name = doc.getString("displayName");
            if (name != null) etName.setText(name);
        }

        String branch = doc.getString("branch");
        if (branch != null) etBranch.setText(branch);

        Long semester = doc.getLong("semester");
        if (semester != null) etSemester.setText(String.valueOf(semester));

        String rollNumber = doc.getString("rollNumber");
        if (rollNumber != null) etRollNumber.setText(rollNumber);

        Double cgpa = doc.getDouble("cgpa");
        if (cgpa != null) etCgpa.setText(String.valueOf(cgpa));

        String contact = doc.getString("contact");
        if (contact != null) etContact.setText(contact);

        String university = doc.getString("university");
        if (university != null) etUniversity.setText(university);
    }

    private void saveProfile() {
        if (user == null) return;

        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        Long semester = parseLongOrNull(etSemester.getText().toString().trim());
        Double cgpa = parseDoubleOrNull(etCgpa.getText().toString().trim());

        setLoading(true);

        // Keep FirebaseAuth's displayName in sync too.
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(request);

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", name);
        updates.put("branch", etBranch.getText().toString().trim());
        updates.put("semester", semester);
        updates.put("rollNumber", etRollNumber.getText().toString().trim());
        updates.put("cgpa", cgpa);
        updates.put("contact", etContact.getText().toString().trim());
        updates.put("university", etUniversity.getText().toString().trim());

        db.collection("users").document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK, new Intent());
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Couldn't save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        findViewById(R.id.btnSaveProfile).setEnabled(!loading);
    }

    private Long parseLongOrNull(String text) {
        if (TextUtils.isEmpty(text)) return null;
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleOrNull(String text) {
        if (TextUtils.isEmpty(text)) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}