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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        loadStudentData();

        // TODO: replace this static list with real timetable data once that
        // feature/collection exists — the current dataset has no schedule.
        List<ClassItem> classes = new ArrayList<>();
        classes.add(new ClassItem("9:00 - 10:00 AM", "Data Structures", "Prof. Name · Room", true, false));
        classes.add(new ClassItem("11:00 - 12:00 AM", "Data Structure", "Prof. Name · Room", false, false));
        classes.add(new ClassItem("11:00 - 12:00 PM", "DBMS Lab", "Prof. Name · Room", false, true));
        classes.add(new ClassItem("11:00 - 12:00 PM", "DBMS Lab", "Prof. Name · Room", false, false));

        for (int i = 0; i < classes.size(); i++) {
            addTimelineRow(classes.get(i), i == classes.size() - 1);
        }

        rootView.findViewById(R.id.tvSeeAll).setOnClickListener(v ->
                Toast.makeText(getContext(), "See all classes - hook this up later", Toast.LENGTH_SHORT).show());

        rootView.findViewById(R.id.cardNotesRepo).setOnClickListener(v ->
                Toast.makeText(getContext(), "Open Notes Repo", Toast.LENGTH_SHORT).show());

        rootView.findViewById(R.id.cardPyqPapers).setOnClickListener(v ->
                Toast.makeText(getContext(), "Open PYQ Papers", Toast.LENGTH_SHORT).show());

        return rootView;
    }

    /** Pulls personal/academic/metrics straight from students/{uid} — no more
     *  fan-out through enrollments+subjects needed; totalCreditsEarned and
     *  overallAttendance are now real, direct fields. */
    private void loadStudentData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || rootView == null) return;

        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener(this::bindStudentData)
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Couldn't load dashboard data", Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void bindStudentData(DocumentSnapshot doc) {
        if (!isAdded() || rootView == null) return;

        Map<String, Object> personal = (Map<String, Object>) doc.get("personal");
        Map<String, Object> academic = (Map<String, Object>) doc.get("academic");
        Map<String, Object> metrics = (Map<String, Object>) doc.get("metrics");

        TextView tvGreeting = rootView.findViewById(R.id.tvGreeting);
        TextView tvSemester = rootView.findViewById(R.id.tvSemester);
        TextView tvCgpaValue = rootView.findViewById(R.id.tvCgpaValue);
        TextView tvAttendanceValue = rootView.findViewById(R.id.tvAttendanceValue);
        TextView tvCreditsValue = rootView.findViewById(R.id.tvCreditsValue);
        TextView tvAvatarInitials = rootView.findViewById(R.id.tvAvatarInitials);

        String firstName = personal != null && personal.get("firstName") != null
                ? personal.get("firstName").toString() : "there";
        tvGreeting.setText("Hi, " + firstName);

        Object semester = academic != null ? academic.get("semester") : null;
        tvSemester.setText(semester != null ? ("Semester " + semester) : "—");

        Object cpi = metrics != null ? metrics.get("cpi") : null;
        tvCgpaValue.setText(cpi != null ? String.valueOf(cpi) : "—");

        Object attendance = metrics != null ? metrics.get("overallAttendance") : null;
        tvAttendanceValue.setText(attendance != null ? (attendance + "%") : "—");

        Object credits = metrics != null ? metrics.get("totalCreditsEarned") : null;
        tvCreditsValue.setText(credits != null ? String.valueOf(credits) : "—");

        if (personal != null && personal.get("firstName") != null) {
            String first = personal.get("firstName").toString();
            String last = personal.get("lastName") != null ? personal.get("lastName").toString() : "";
            String initials = (first.isEmpty() ? "" : first.charAt(0) + "")
                    + (last.isEmpty() ? "" : last.charAt(0) + "");
            tvAvatarInitials.setText(initials.isEmpty() ? "?" : initials.toUpperCase());
        }
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