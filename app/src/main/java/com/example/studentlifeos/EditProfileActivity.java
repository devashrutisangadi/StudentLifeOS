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

        db.collection("students").document(user.getUid()).get()
                .addOnSuccessListener(this::bindExistingValues)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load existing profile", Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void bindExistingValues(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;

        Map<String, Object> personal = (Map<String, Object>) doc.get("personal");
        Map<String, Object> academic = (Map<String, Object>) doc.get("academic");
        Map<String, Object> metrics = (Map<String, Object>) doc.get("metrics");

        if (TextUtils.isEmpty(etName.getText()) && personal != null) {
            String first = personal.get("firstName") != null ? personal.get("firstName").toString() : "";
            String last = personal.get("lastName") != null ? personal.get("lastName").toString() : "";
            String fullName = (first + " " + last).trim();
            if (!fullName.isEmpty()) etName.setText(fullName);
        }

        if (academic != null) {
            Object branch = academic.get("branch");
            if (branch != null) etBranch.setText(branch.toString());

            Object semester = academic.get("semester");
            if (semester != null) etSemester.setText(String.valueOf(semester));

            Object rollNumber = academic.get("rollNumber");
            if (rollNumber != null) etRollNumber.setText(rollNumber.toString());

            Object university = academic.get("university");
            if (university != null) etUniversity.setText(university.toString());
        }

        if (metrics != null) {
            Object cgpa = metrics.get("cpi");
            if (cgpa != null) etCgpa.setText(String.valueOf(cgpa));
        }

        if (personal != null) {
            Object contact = personal.get("phone");
            if (contact != null) etContact.setText(contact.toString());
        }
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

        String[] nameParts = name.split("\\s+", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Dot-path keys so this only touches the fields this form edits,
        // leaving college/degree/dob/bloodGroup/enrollmentNumber etc. (and
        // metrics like spiHistory/backlogs/overallAttendance) untouched.
        Map<String, Object> updates = new HashMap<>();
        updates.put("personal.firstName", firstName);
        updates.put("personal.lastName", lastName);
        updates.put("personal.phone", etContact.getText().toString().trim());
        updates.put("academic.branch", etBranch.getText().toString().trim());
        updates.put("academic.semester", semester);
        updates.put("academic.rollNumber", etRollNumber.getText().toString().trim());
        updates.put("academic.university", etUniversity.getText().toString().trim());
        updates.put("metrics.cpi", cgpa);

        db.collection("students").document(user.getUid())
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