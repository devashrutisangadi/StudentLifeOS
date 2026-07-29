package com.example.studentlifeos;

import android.text.TextUtils;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddSubjectActivity extends AppCompatActivity {

    private LinearLayout unitsContainer;
    private final List<View> unitRows = new ArrayList<>();
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_subject);

        unitsContainer = findViewById(R.id.unitsContainer);
        progress = findViewById(R.id.saveSubjectProgress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        addUnitRow(); // start with one empty unit field

        findViewById(R.id.tvAddUnit).setOnClickListener(v -> addUnitRow());

        findViewById(R.id.btnSaveSubject).setOnClickListener(v -> saveSubject());
    }

    private void addUnitRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.item_unit_input, unitsContainer, false);
        ImageView ivRemove = row.findViewById(R.id.ivRemoveUnit);
        ivRemove.setOnClickListener(v -> {
            unitsContainer.removeView(row);
            unitRows.remove(row);
        });
        unitsContainer.addView(row);
        unitRows.add(row);
    }

    private void saveSubject() {
        EditText etName = findViewById(R.id.etSubjectName);
        EditText etCode = findViewById(R.id.etSubjectCode);
        EditText etFaculty = findViewById(R.id.etSubjectFaculty);

        String name = etName.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String faculty = etFaculty.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Subject name is required");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        progress.setVisibility(View.VISIBLE);
        findViewById(R.id.btnSaveSubject).setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> subject = new HashMap<>();
        subject.put("studentId", uid);
        subject.put("name", name);
        subject.put("code", code);
        subject.put("faculty", faculty);
        subject.put("progress", 0);

        db.collection("subjects").add(subject)
                .addOnSuccessListener(subjectRef -> {
                    String subjectId = subjectRef.getId();

                    // Collect non-empty unit titles typed in
                    List<String> unitTitles = new ArrayList<>();
                    for (View row : unitRows) {
                        EditText etUnit = row.findViewById(R.id.etUnitName);
                        String title = etUnit.getText().toString().trim();
                        if (!TextUtils.isEmpty(title)) unitTitles.add(title);
                    }

                    if (unitTitles.isEmpty()) {
                        finishSuccessfully();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (String title : unitTitles) {
                        Map<String, Object> unit = new HashMap<>();
                        unit.put("subjectId", subjectId);
                        unit.put("studentId", uid);
                        unit.put("title", title);
                        unit.put("completed", false);
                        batch.set(db.collection("units").document(), unit);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> finishSuccessfully())
                            .addOnFailureListener(e -> {
                                // Subject itself saved fine; units failed — don't lose the whole thing silently
                                progress.setVisibility(View.GONE);
                                findViewById(R.id.btnSaveSubject).setEnabled(true);
                                Toast.makeText(this,
                                        "Subject saved, but couldn't save units: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    findViewById(R.id.btnSaveSubject).setEnabled(true);
                    Toast.makeText(this, "Couldn't save subject: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void finishSuccessfully() {
        Toast.makeText(this, "Subject added", Toast.LENGTH_SHORT).show();
        finish();
    }
}