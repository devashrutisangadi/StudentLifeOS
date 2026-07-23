package com.example.studentlifeos;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PapersListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_papers_list);

        String subjectName = getIntent().getStringExtra("subjectName");
        ((TextView) findViewById(R.id.tvSubjectTitle)).setText(subjectName != null ? subjectName : "Papers");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recyclerPapers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // NOTE: placeholder data — no real previous-year papers dataset exists yet.
        // Replace this with a real Firestore query once actual papers are collected.
        List<PaperAdapter.Paper> papers = generatePlaceholderPapers();
        PaperAdapter adapter = new PaperAdapter(papers, paper ->
                Toast.makeText(this, "Sample placeholder — real papers not uploaded yet", Toast.LENGTH_SHORT).show());
        recyclerView.setAdapter(adapter);
    }

    private List<PaperAdapter.Paper> generatePlaceholderPapers() {
        List<PaperAdapter.Paper> papers = new ArrayList<>();
        String[] examTypes = {"Mid-Semester", "End-Semester"};
        int[] years = {2025, 2024, 2023};
        for (int year : years) {
            for (String examType : examTypes) {
                PaperAdapter.Paper p = new PaperAdapter.Paper();
                p.title = examType + " " + year;
                p.examType = examType;
                p.year = year;
                papers.add(p);
            }
        }
        return papers;
    }
}