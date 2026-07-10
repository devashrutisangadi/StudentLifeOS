package com.example.studentlifeos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private LinearLayout timelineContainer;
    private View rootView;

    private static class ClassItem {
        String time, subject, professorRoom;
        boolean isCurrent;
        boolean isNew;

        ClassItem(String time, String subject, String professorRoom, boolean isCurrent, boolean isNew) {
            this.time = time;
            this.subject = subject;
            this.professorRoom = professorRoom;
            this.isCurrent = isCurrent;
            this.isNew = isNew;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        timelineContainer = rootView.findViewById(R.id.timelineContainer);

        loadProfileHeader();
        loadCreditsTotal();

        // TODO: replace this static list with real timetable data from Firestore later
        List<ClassItem> classes = new ArrayList<>();
        classes.add(new ClassItem("9:00 - 10:00 AM", "Data Structures", "Prof. Name · Room", true, false));
        classes.add(new ClassItem("11:00 - 12:00 AM", "Data Structure", "Prof. Name · Room", false, false));
        classes.add(new ClassItem("11:00 - 12:00 PM", "DBMS Lab", "Prof. Name · Room", false, true));
        classes.add(new ClassItem("11:00 - 12:00 PM", "DBMS Lab", "Prof. Name · Room", false, false));

        for (int i = 0; i < classes.size(); i++) {
            boolean isLast = (i == classes.size() - 1);
            addTimelineRow(classes.get(i), isLast);
        }

        TextView tvSeeAll = rootView.findViewById(R.id.tvSeeAll);
        tvSeeAll.setOnClickListener(v ->
                Toast.makeText(getContext(), "See all classes - hook this up later", Toast.LENGTH_SHORT).show());

        LinearLayout cardNotesRepo = rootView.findViewById(R.id.cardNotesRepo);
        cardNotesRepo.setOnClickListener(v ->
                Toast.makeText(getContext(), "Open Notes Repo", Toast.LENGTH_SHORT).show());

        LinearLayout cardPyqPapers = rootView.findViewById(R.id.cardPyqPapers);
        cardPyqPapers.setOnClickListener(v ->
                Toast.makeText(getContext(), "Open PYQ Papers", Toast.LENGTH_SHORT).show());

        return rootView;
    }

    /** Pulls displayName, semester, and cgpa from the signed-in user's Firestore doc. */
    private void loadProfileHeader() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || rootView == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(this::bindProfileHeader)
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Couldn't load dashboard data", Toast.LENGTH_SHORT).show());
    }

    private void bindProfileHeader(DocumentSnapshot doc) {
        if (!isAdded() || rootView == null) return;

        String name = doc.getString("displayName");
        Long semester = doc.getLong("semester");
        Double cgpa = doc.getDouble("cgpa");

        TextView tvGreeting = rootView.findViewById(R.id.tvGreeting);
        TextView tvSemester = rootView.findViewById(R.id.tvSemester);
        TextView tvCgpaValue = rootView.findViewById(R.id.tvCgpaValue);
        TextView tvAvatarInitials = rootView.findViewById(R.id.tvAvatarInitials);

        // First name only for the greeting, to match "Hi, Carlitos" style
        String firstName = (name != null && !name.trim().isEmpty())
                ? name.trim().split("\\s+")[0] : "there";
        tvGreeting.setText("Hi, " + firstName);

        tvSemester.setText(semester != null ? ("Semester " + semester) : "—");
        tvCgpaValue.setText(cgpa != null ? String.valueOf(cgpa) : "—");

        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            String initials = parts.length > 1
                    ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
                    : parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            tvAvatarInitials.setText(initials);
        }
    }

    /**
     * Sums the `credits` field across every subject this student is enrolled in.
     * NOTE: Attendance has no equivalent — there is no attendance-tracking
     * collection in the current data model, so tvAttendanceValue is left as "—"
     * until that feature exists.
     */
    private void loadCreditsTotal() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || rootView == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("enrollments")
                .whereEqualTo("studentUid", uid)
                .get()
                .addOnSuccessListener(enrollmentDocs -> {
                    List<String> subjectIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : enrollmentDocs) {
                        String subjectId = doc.getString("subjectId");
                        if (subjectId != null) subjectIds.add(subjectId);
                    }

                    if (subjectIds.isEmpty()) {
                        setCreditsText(0);
                        return;
                    }

                    final int[] creditsSum = {0};
                    final int[] remaining = {subjectIds.size()};

                    for (String subjectId : subjectIds) {
                        db.collection("subjects").document(subjectId).get()
                                .addOnSuccessListener(subjectDoc -> {
                                    Long credits = subjectDoc.getLong("credits");
                                    if (credits != null) creditsSum[0] += credits;
                                    remaining[0]--;
                                    if (remaining[0] == 0) setCreditsText(creditsSum[0]);
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) setCreditsText(creditsSum[0]);
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Couldn't load credits", Toast.LENGTH_SHORT).show());
    }

    private void setCreditsText(int total) {
        if (!isAdded() || rootView == null) return;
        TextView tvCreditsValue = rootView.findViewById(R.id.tvCreditsValue);
        tvCreditsValue.setText(String.valueOf(total));
    }

    private void addTimelineRow(ClassItem item, boolean isLast) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View row = inflater.inflate(R.layout.item_timeline_class, timelineContainer, false);

        View dot = row.findViewById(R.id.timelineDot);
        View line = row.findViewById(R.id.timelineLine);
        TextView tvTime = row.findViewById(R.id.tvClassTime);
        TextView tvSubject = row.findViewById(R.id.tvClassSubject);
        TextView tvProfRoom = row.findViewById(R.id.tvClassProfRoom);
        TextView tvNewBadge = row.findViewById(R.id.tvNewBadge);

        dot.setBackgroundResource(item.isCurrent ? R.drawable.dot_filled_purple : R.drawable.dot_outline_purple);
        line.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        tvTime.setText(item.time);
        tvSubject.setText(item.subject);
        tvProfRoom.setText(item.professorRoom);
        tvNewBadge.setVisibility(item.isNew ? View.VISIBLE : View.GONE);

        timelineContainer.addView(row);
    }
}