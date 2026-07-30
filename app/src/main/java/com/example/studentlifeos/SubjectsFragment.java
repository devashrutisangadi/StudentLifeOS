package com.example.studentlifeos;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class SubjectsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SubjectAdapter adapter;
    private EditText etSearchSubject;
    private ImageView ivNotifications;
    private TextView tvNoResults;
    private List<SubjectAdapter.Subject> allSubjects = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subjects, container, false);

        recyclerView = view.findViewById(R.id.recyclerSubjects);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SubjectAdapter(
                new ArrayList<>(),
                subject -> {
                    Intent intent = new Intent(getContext(), SyllabusTrackerActivity.class);
                    intent.putExtra("subjectId", subject.id);
                    intent.putExtra("subjectName", subject.name);
                    startActivity(intent);
                },
                subject -> confirmAndDeleteSubject(subject)
        );
        recyclerView.setAdapter(adapter);

        tvNoResults = view.findViewById(R.id.tvNoResults);
        view.findViewById(R.id.fabAddSubject).setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddSubjectActivity.class))
        );

        etSearchSubject = view.findViewById(R.id.etSearchSubject);
        etSearchSubject.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSubjects(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivNotifications = view.findViewById(R.id.ivNotifications);
        ivNotifications.setOnClickListener(v ->
                startActivity(new Intent(getContext(), NotificationsActivity.class)));

        loadSubjects();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSubjects();
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
        allSubjects = subjects;
        filterSubjects(etSearchSubject.getText().toString());
    }

    private void filterSubjects(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.updateData(allSubjects);
            tvNoResults.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            return;
        }
        String q = query.toLowerCase();
        List<SubjectAdapter.Subject> filtered = new ArrayList<>();
        for (SubjectAdapter.Subject s : allSubjects) {
            if ((s.name != null && s.name.toLowerCase().contains(q))
                    || (s.code != null && s.code.toLowerCase().contains(q))) {
                filtered.add(s);
            }
        }
        adapter.updateData(filtered);

        if (filtered.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoResults.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void confirmAndDeleteSubject(SubjectAdapter.Subject subject) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete subject?")
                .setMessage("This will also delete all of \"" + subject.name + "\"'s syllabus units. This can't be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteSubjectAndUnits(subject))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSubjectAndUnits(SubjectAdapter.Subject subject) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("units")
                .whereEqualTo("subjectId", subject.id)
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(unitDocs -> {
                    WriteBatch batch = db.batch();
                    unitDocs.forEach(doc -> batch.delete(doc.getReference()));
                    batch.delete(db.collection("subjects").document(subject.id));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (!isAdded()) return;
                                Toast.makeText(getContext(), "Subject deleted", Toast.LENGTH_SHORT).show();
                                loadSubjects();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                Toast.makeText(getContext(), "Couldn't delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Couldn't delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}