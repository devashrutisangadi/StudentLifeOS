package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyllabusTrackerActivity extends AppCompatActivity {

    private UnitAdapter adapter;
    private TextView tvProgressLabel, tvProgressPercent;
    private ProgressBar progressBarFill;
    private String subjectId, subjectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syllabus_tracker);

        subjectId = getIntent().getStringExtra("subjectId");
        subjectName = getIntent().getStringExtra("subjectName");

        ((TextView) findViewById(R.id.tvSubjectTitle)).setText(subjectName != null ? subjectName : "Syllabus");
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBarFill = findViewById(R.id.progressBarFill);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recyclerUnits);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UnitAdapter(
                new ArrayList<>(),
                (unit, isChecked) -> toggleUnit(unit, isChecked),
                unit -> {
                    Intent intent = new Intent(this, NotesActivity.class);
                    intent.putExtra("unitId", unit.id);
                    intent.putExtra("unitTitle", unit.title);
                    startActivity(intent);
                }
        );
        recyclerView.setAdapter(adapter);

        loadUnits(subjectId);
    }

    private void loadUnits(String subjectId) {
        if (subjectId == null) return;

        FirebaseFirestore.getInstance().collection("units")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .addOnSuccessListener(this::bindUnits)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load syllabus: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bindUnits(QuerySnapshot snapshot) {
        List<UnitAdapter.Unit> units = new ArrayList<>();
        snapshot.forEach(doc -> {
            UnitAdapter.Unit u = new UnitAdapter.Unit();
            u.id = doc.getId();
            u.title = doc.getString("title");
            Boolean completed = doc.getBoolean("completed");
            u.completed = completed != null && completed;
            units.add(u);
        });
        adapter.updateData(units);
        updateProgressSummary(units); // visual only — no Firestore write on initial load
    }

    private void toggleUnit(UnitAdapter.Unit unit, boolean isChecked) {
        Map<String, Object> update = new HashMap<>();
        update.put("completed", isChecked);
        update.put("studentId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        FirebaseFirestore.getInstance().collection("units").document(unit.id)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    unit.completed = isChecked;
                    int percent = updateProgressSummary(adapter.getUnits());
                    syncSubjectProgress(percent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Updates the local progress bar/label and returns the computed percent. */
    private int updateProgressSummary(List<UnitAdapter.Unit> units) {
        int total = units.size();
        int completed = 0;
        for (UnitAdapter.Unit u : units) {
            if (u.completed) completed++;
        }
        int percent = total == 0 ? 0 : (completed * 100) / total;

        tvProgressLabel.setText(completed + " of " + total + " units complete");
        tvProgressPercent.setText(percent + "%");
        progressBarFill.setProgress(percent);
        return percent;
    }

    /** Writes the freshly computed completion percentage back to the parent
     *  subject document, so the Subjects list reflects real progress instead
     *  of the dataset's original static value. Only called on an actual
     *  toggle, not on initial screen load. */
    private void syncSubjectProgress(int percent) {
        if (subjectId == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("progress", percent);

        FirebaseFirestore.getInstance().collection("subjects").document(subjectId)
                .set(update, SetOptions.merge());
        // Fire-and-forget: the Syllabus Tracker screen already reflects the
        // change locally; Subjects will pick it up on its next onResume().
    }
}