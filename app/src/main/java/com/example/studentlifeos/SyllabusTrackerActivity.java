package com.example.studentlifeos;

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
    private ProgressBar progressBarFill; // if using an Android ProgressBar; otherwise a View resized programmatically

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syllabus_tracker);

        String subjectId = getIntent().getStringExtra("subjectId");
        String subjectName = getIntent().getStringExtra("subjectName");

        ((TextView) findViewById(R.id.tvSubjectTitle)).setText(subjectName != null ? subjectName : "Syllabus");
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBarFill = findViewById(R.id.progressBarFill);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recyclerUnits);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UnitAdapter(new ArrayList<>(), (unit, isChecked) -> toggleUnit(unit, isChecked));
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
        updateProgressSummary(units);
    }

    private void toggleUnit(UnitAdapter.Unit unit, boolean isChecked) {
        Map<String, Object> update = new HashMap<>();
        update.put("completed", isChecked);
        update.put("studentId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        FirebaseFirestore.getInstance().collection("units").document(unit.id)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    unit.completed = isChecked;
                    updateProgressSummary(adapter.getUnits());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateProgressSummary(List<UnitAdapter.Unit> units) {
        int total = units.size();
        int completed = 0;
        for (UnitAdapter.Unit u : units) {
            if (u.completed) completed++;
        }
        int percent = total == 0 ? 0 : (completed * 100) / total;

        tvProgressLabel.setText(completed + " of " + total + " units complete");
        tvProgressPercent.setText(percent + "%");
        progressBarFill.setProgress(percent);
    }
}