package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * The dedicated Timetable screen, reached from Home's "See all". Shows day tabs (Monday
 * through Sunday, defaulting to today) with that day's classes underneath, and lets the
 * student add/edit/delete entries individually or replace the whole week via a PDF upload
 * (UploadTimetableActivity).
 */
public class TimetableActivity extends AppCompatActivity {

    private final List<TimetableEntry> allEntries = new ArrayList<>();
    private int selectedDayIndex;
    private LinearLayout dayTabsContainer;
    private TimetableEntryAdapter adapter;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        selectedDayIndex = TimeFormatUtil.todayDayIndex();

        dayTabsContainer = findViewById(R.id.dayTabsContainer);
        tvEmptyState = findViewById(R.id.tvTimetableEmptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnUploadTimetable).setOnClickListener(v ->
                startActivity(new Intent(this, UploadTimetableActivity.class)));
        findViewById(R.id.btnAddEntry).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTimetableEntryActivity.class);
            intent.putExtra("dayIndex", selectedDayIndex);
            startActivity(intent);
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerTimetableEntries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableEntryAdapter(new ArrayList<>(), this::openEditEntry, this::confirmDelete);
        recyclerView.setAdapter(adapter);

        buildDayTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTimetable();
    }

    private void buildDayTabs() {
        dayTabsContainer.removeAllViews();
        for (int i = 1; i <= 7; i++) {
            int dayIndex = i;
            TextView tab = new TextView(this);
            String shortLabel = TimetableEntry.dayNameFor(dayIndex).substring(0, 3);
            tab.setText(shortLabel);
            tab.setTextSize(12);
            tab.setPadding(dp(14), dp(7), dp(14), dp(7));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(6));
            tab.setLayoutParams(params);
            tab.setOnClickListener(v -> {
                selectedDayIndex = dayIndex;
                buildDayTabs();
                bindEntriesForSelectedDay();
            });
            styleTab(tab, dayIndex == selectedDayIndex);
            dayTabsContainer.addView(tab);
        }
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_pill_purple : R.drawable.bg_card_white_rounded);
        tab.setTextColor(getResources().getColor(selected ? R.color.white : R.color.text_gray));
        tab.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void loadTimetable() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("timetable_entries")
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(this::bindAllEntries)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load timetable: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bindAllEntries(QuerySnapshot snapshot) {
        allEntries.clear();
        for (QueryDocumentSnapshot doc : snapshot) {
            TimetableEntry entry = new TimetableEntry();
            entry.id = doc.getId();
            entry.dayIndex = doc.getLong("dayIndex") != null ? doc.getLong("dayIndex").intValue() : 0;
            entry.dayName = doc.getString("dayName");
            entry.startTime = doc.getString("startTime");
            entry.endTime = doc.getString("endTime");
            entry.subjectName = doc.getString("subjectName");
            entry.professorName = doc.getString("professorName");
            entry.room = doc.getString("room");
            allEntries.add(entry);
        }
        bindEntriesForSelectedDay();
    }

    private void bindEntriesForSelectedDay() {
        List<TimetableEntry> dayEntries = new ArrayList<>();
        for (TimetableEntry e : allEntries) {
            if (e.dayIndex == selectedDayIndex) dayEntries.add(e);
        }
        dayEntries.sort((a, b) -> a.startTime.compareTo(b.startTime));

        adapter.updateData(dayEntries);
        tvEmptyState.setVisibility(dayEntries.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.recyclerTimetableEntries).setVisibility(dayEntries.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openEditEntry(TimetableEntry entry) {
        Intent intent = new Intent(this, AddTimetableEntryActivity.class);
        intent.putExtra("entryId", entry.id);
        intent.putExtra("dayIndex", entry.dayIndex);
        intent.putExtra("startTime", entry.startTime);
        intent.putExtra("endTime", entry.endTime);
        intent.putExtra("subjectName", entry.subjectName);
        intent.putExtra("professorName", entry.professorName);
        intent.putExtra("room", entry.room);
        startActivity(intent);
    }

    private void confirmDelete(TimetableEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this class?")
                .setMessage(entry.subjectName + " · " + TimeFormatUtil.formatRange(entry.startTime, entry.endTime))
                .setPositiveButton("Delete", (dialog, which) ->
                        FirebaseFirestore.getInstance().collection("timetable_entries").document(entry.id)
                                .delete()
                                .addOnSuccessListener(unused -> loadTimetable())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Couldn't delete: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
