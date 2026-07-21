package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class SubjectsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SubjectAdapter adapter;
    private TextView subtitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subjects, container, false);

        recyclerView = view.findViewById(R.id.recyclerSubjects);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SubjectAdapter(new ArrayList<>(), subject -> {
            Intent intent = new Intent(getContext(), SyllabusTrackerActivity.class);
            intent.putExtra("subjectId", subject.id);
            intent.putExtra("subjectName", subject.name);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        subtitle = view.findViewById(R.id.subjectsSubtitle);

        RecyclerView chipRecycler = view.findViewById(R.id.semesterChipRecycler);
        chipRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        List<String> semesters = Arrays.asList("Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6");
        SemesterChipAdapter chipAdapter = new SemesterChipAdapter(getContext(), semesters, semester -> {
            // TODO: once semester filtering is wired into Firestore, re-query here instead of just updating the label
            subtitle.setText(semester + " · " + adapter.getItemCount() + " subjects");
        });
        chipRecycler.setAdapter(chipAdapter);

        loadSubjects();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSubjects(); // refresh progress %s in case a syllabus was updated
    }

    private void loadSubjects() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("subjects")
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(this::bindSubjects)
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Couldn't load subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bindSubjects(QuerySnapshot snapshot) {
        if (!isAdded()) return;
        List<SubjectAdapter.Subject> subjects = new ArrayList<>();
        snapshot.forEach(doc -> {
            SubjectAdapter.Subject s = new SubjectAdapter.Subject();
            s.id = doc.getId();
            s.name = doc.getString("name");
            s.code = doc.getString("code");
            s.faculty = doc.getString("faculty");
            Object progress = doc.get("progress");
            s.progress = progress != null ? ((Number) progress).intValue() : 0;
            subjects.add(s);
        });
        adapter.updateData(subjects);
        if (subtitle != null) {
            subtitle.setText("Semester 5 · " + subjects.size() + " subjects"); // TODO: replace hardcoded "Semester 5" once user's actual semester is available
        }
    }
}