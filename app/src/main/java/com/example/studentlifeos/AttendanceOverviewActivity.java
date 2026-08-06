package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AttendanceOverviewActivity extends AppCompatActivity {

    private AttendanceOverviewAdapter adapter;
    private final List<AttendanceOverviewAdapter.SubjectSummary> summaries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_overview);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recyclerAttendanceOverview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AttendanceOverviewAdapter(summaries, subject -> {
            Intent intent = new Intent(this, AttendanceDetailActivity.class);
            intent.putExtra("subjectId", subject.subjectId);
            intent.putExtra("subjectName", subject.subjectName);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadSubjectsAndAttendance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reload if we're returning to a non-empty list already shown once;
        // a full reload here also keeps things fresh after logging attendance
        // on the detail screen and backing out.
        if (!summaries.isEmpty()) {
            summaries.clear();
            adapter.notifyDataSetChanged();
            loadSubjectsAndAttendance();
        }
    }

    private void loadSubjectsAndAttendance() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("subjects")
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(subjectDocs -> {
                    if (subjectDocs.isEmpty()) {
                        findViewById(R.id.tvNoSubjects).setVisibility(View.VISIBLE);
                        return;
                    }
                    findViewById(R.id.tvNoSubjects).setVisibility(View.GONE);

                    int total = subjectDocs.size();
                    int[] remaining = {total};

                    subjectDocs.forEach(subjectDoc -> {
                        AttendanceOverviewAdapter.SubjectSummary summary = new AttendanceOverviewAdapter.SubjectSummary();
                        summary.subjectId = subjectDoc.getId();
                        summary.subjectName = subjectDoc.getString("name");
                        summaries.add(summary);
                        int index = summaries.size() - 1;

                        db.collection("attendance")
                                .whereEqualTo("subjectId", summary.subjectId)
                                .whereEqualTo("studentId", uid)
                                .get()
                                .addOnSuccessListener(logs -> {
                                    int held = logs.size();
                                    int attended = 0;
                                    for (com.google.firebase.firestore.DocumentSnapshot doc : logs.getDocuments()) {
                                        Boolean present = doc.getBoolean("present");
                                        if (Boolean.TRUE.equals(present)) attended++;
                                    }
                                    summary.held = held;
                                    summary.attended = attended;
                                    summary.hasLogs = held > 0;

                                    adapter.notifyItemChangedFor(index);
                                    remaining[0]--;
                                    if (remaining[0] == 0) adapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                });
                    });

                    adapter.notifyDataSetChanged(); // show subject names immediately, percentages fill in as they arrive
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}