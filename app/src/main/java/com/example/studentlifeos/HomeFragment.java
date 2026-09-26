package com.example.studentlifeos;

import android.content.Intent;
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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private LinearLayout timelineContainer;
    private View rootView;

    private static class ClassItem {
        String time, subject, professorRoom;
        boolean isCurrent;

        ClassItem(String time, String subject, String professorRoom, boolean isCurrent) {
            this.time = time;
            this.subject = subject;
            this.professorRoom = professorRoom;
            this.isCurrent = isCurrent;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        timelineContainer = rootView.findViewById(R.id.timelineContainer);

        loadStudentData();
        loadTodaysClasses();

        rootView.findViewById(R.id.tvSeeAll).setOnClickListener(v ->
                startActivity(new Intent(getContext(), TimetableActivity.class)));

        rootView.findViewById(R.id.timetableEmptyPrompt).setOnClickListener(v ->
                startActivity(new Intent(getContext(), TimetableActivity.class)));

        rootView.findViewById(R.id.cardSubjects).setOnClickListener(v -> {
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottomNav);
                bottomNav.setSelectedItemId(R.id.nav_subjects);
            }
        });

        rootView.findViewById(R.id.cardPyqPapers).setOnClickListener(v -> {
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottomNav);
                bottomNav.setSelectedItemId(R.id.nav_papers);
            }
        });
        rootView.findViewById(R.id.cardNotesRepo).setOnClickListener(v -> {
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottomNav);
                bottomNav.setSelectedItemId(R.id.nav_subjects);
            }
        });
        rootView.findViewById(R.id.cardAttendance).setOnClickListener(v ->
                startActivity(new android.content.Intent(getContext(), AttendanceOverviewActivity.class))
        );

        // Flashcards (like Notes) only exists per-unit, so — same as cardNotesRepo above —
        // this switches to the Subjects tab rather than opening a flashcards screen directly.
        // The student picks a unit there, then taps the Flashcards pill from that unit's
        // Notes screen.
        rootView.findViewById(R.id.cardFlashcards).setOnClickListener(v -> {
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottomNav);
                bottomNav.setSelectedItemId(R.id.nav_subjects);
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh in case the student just came back from uploading/editing their timetable.
        if (rootView != null) loadTodaysClasses();
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

    /** Queries timetable_entries for today's dayIndex and renders them in the timeline,
     *  replacing the old static placeholder list. */
    private void loadTodaysClasses() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || rootView == null) {
            showEmptyTimetableState();
            return;
        }

        int todayIndex = TimeFormatUtil.todayDayIndex();

        FirebaseFirestore.getInstance().collection("timetable_entries")
                .whereEqualTo("studentId", uid)
                .whereEqualTo("dayIndex", todayIndex)
                .get()
                .addOnSuccessListener(this::bindTodaysClasses)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Couldn't load timetable", Toast.LENGTH_SHORT).show();
                    showEmptyTimetableState();
                });
    }

    private void bindTodaysClasses(QuerySnapshot snapshot) {
        if (!isAdded() || rootView == null) return;

        List<TimetableEntry> entries = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            TimetableEntry entry = new TimetableEntry();
            entry.startTime = doc.getString("startTime");
            entry.endTime = doc.getString("endTime");
            entry.subjectName = doc.getString("subjectName");
            entry.professorName = doc.getString("professorName");
            entry.room = doc.getString("room");
            entries.add(entry);
        }

        if (entries.isEmpty()) {
            showEmptyTimetableState();
            return;
        }

        entries.sort((a, b) -> a.startTime.compareTo(b.startTime));

        String now = TimeFormatUtil.nowAsStorageTime();
        List<ClassItem> classes = new ArrayList<>();
        for (TimetableEntry e : entries) {
            boolean isCurrent = e.startTime != null && e.endTime != null
                    && now.compareTo(e.startTime) >= 0 && now.compareTo(e.endTime) < 0;

            String meta = "";
            if (e.professorName != null && !e.professorName.trim().isEmpty()) meta += e.professorName.trim();
            if (e.room != null && !e.room.trim().isEmpty()) {
                meta += meta.isEmpty() ? e.room.trim() : " · " + e.room.trim();
            }

            classes.add(new ClassItem(TimeFormatUtil.formatRange(e.startTime, e.endTime),
                    e.subjectName != null ? e.subjectName : "", meta, isCurrent));
        }

        timelineContainer.removeAllViews();
        timelineContainer.setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.timetableEmptyPrompt).setVisibility(View.GONE);

        for (int i = 0; i < classes.size(); i++) {
            addTimelineRow(classes.get(i), i == classes.size() - 1);
        }
    }

    private void showEmptyTimetableState() {
        if (rootView == null) return;
        timelineContainer.removeAllViews();
        timelineContainer.setVisibility(View.GONE);
        rootView.findViewById(R.id.timetableEmptyPrompt).setVisibility(View.VISIBLE);
    }

    private void addTimelineRow(ClassItem item, boolean isLast) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View row = inflater.inflate(R.layout.item_timeline_class, timelineContainer, false);

        View dot = row.findViewById(R.id.timelineDot);
        View line = row.findViewById(R.id.timelineLine);
        TextView tvTime = row.findViewById(R.id.tvClassTime);
        TextView tvSubject = row.findViewById(R.id.tvClassSubject);
        TextView tvProfRoom = row.findViewById(R.id.tvClassProfRoom);
        TextView tvNowBadge = row.findViewById(R.id.tvNewBadge); // repurposed to show "NOW" for the current class

        dot.setBackgroundResource(item.isCurrent ? R.drawable.dot_filled_purple : R.drawable.dot_outline_purple);
        line.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        tvTime.setText(item.time);
        tvSubject.setText(item.subject);
        tvProfRoom.setText(item.professorRoom);
        tvNowBadge.setText("NOW");
        tvNowBadge.setVisibility(item.isCurrent ? View.VISIBLE : View.GONE);

        timelineContainer.addView(row);
    }
}
