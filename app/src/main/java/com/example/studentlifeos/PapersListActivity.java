package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class PapersListActivity extends AppCompatActivity {

    private String subjectId, subjectName;
    private PaperAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_papers_list);

        subjectId = getIntent().getStringExtra("subjectId");
        subjectName = getIntent().getStringExtra("subjectName");

        ((TextView) findViewById(R.id.tvSubjectTitle)).setText(subjectName != null ? subjectName : "Papers");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recyclerPapers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PaperAdapter(
                new ArrayList<>(),
                paper -> {
                    if (paper.fileUrl != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(paper.fileUrl));
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "No file attached to this paper", Toast.LENGTH_SHORT).show();
                    }
                },
                paper -> confirmAndDeletePaper(paper)
        );
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabAddPaper).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPaperActivity.class);
            intent.putExtra("subjectId", subjectId);
            intent.putExtra("subjectName", subjectName);
            startActivity(intent);
        });

        loadPapers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPapers();
    }

    private void loadPapers() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || subjectId == null) return;

        FirebaseFirestore.getInstance().collection("papers")
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(this::bindPapers)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load papers: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bindPapers(QuerySnapshot snapshot) {
        List<PaperAdapter.Paper> papers = new ArrayList<>();
        snapshot.forEach(doc -> {
            PaperAdapter.Paper p = new PaperAdapter.Paper();
            p.id = doc.getId();
            p.title = doc.getString("title");
            p.examType = doc.getString("examType");
            p.fileUrl = doc.getString("fileUrl");
            p.fileType = doc.getString("fileType");
            Object year = doc.get("year");
            p.year = year != null ? ((Number) year).intValue() : 0;
            papers.add(p);
        });
        adapter.updateData(papers);
    }

    private void confirmAndDeletePaper(PaperAdapter.Paper paper) {
        new AlertDialog.Builder(this)
                .setTitle("Delete paper?")
                .setMessage("Remove \"" + paper.title + "\"? This can't be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("papers").document(paper.id)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Paper deleted", Toast.LENGTH_SHORT).show();
                                loadPapers();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Couldn't delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}