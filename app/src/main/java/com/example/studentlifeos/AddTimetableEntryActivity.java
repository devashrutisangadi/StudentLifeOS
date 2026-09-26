package com.example.studentlifeos;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddTimetableEntryActivity extends AppCompatActivity {

    private String existingEntryId;
    private Spinner spinnerDay;
    private TextView tvStartTime, tvEndTime;
    private EditText etSubject, etProfessor, etRoom;
    private Button btnSave;
    private String startTime, endTime; // "HH:mm", 24h

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_timetable_entry);

        existingEntryId = getIntent().getStringExtra("entryId"); // null if adding a new entry
        int prefillDayIndex = getIntent().getIntExtra("dayIndex", TimeFormatUtil.todayDayIndex());
        startTime = getIntent().getStringExtra("startTime");
        endTime = getIntent().getStringExtra("endTime");

        ((TextView) findViewById(R.id.tvAddEntryTitle))
                .setText(existingEntryId != null ? "Edit class" : "Add class");

        spinnerDay = findViewById(R.id.spinnerDay);
        android.widget.ArrayAdapter<String> dayAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, TimetableEntry.DAY_NAMES);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);
        spinnerDay.setSelection(Math.max(0, prefillDayIndex - 1));

        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        etSubject = findViewById(R.id.etSubjectName);
        etProfessor = findViewById(R.id.etProfessorName);
        etRoom = findViewById(R.id.etRoom);
        btnSave = findViewById(R.id.btnSaveEntry);

        if (startTime != null) tvStartTime.setText(TimeFormatUtil.toDisplay(startTime));
        if (endTime != null) tvEndTime.setText(TimeFormatUtil.toDisplay(endTime));

        String existingSubject = getIntent().getStringExtra("subjectName");
        String existingProfessor = getIntent().getStringExtra("professorName");
        String existingRoom = getIntent().getStringExtra("room");
        if (existingSubject != null) etSubject.setText(existingSubject);
        if (existingProfessor != null) etProfessor.setText(existingProfessor);
        if (existingRoom != null) etRoom.setText(existingRoom);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvStartTime.setOnClickListener(v -> pickTime(true));
        tvEndTime.setOnClickListener(v -> pickTime(false));
        btnSave.setOnClickListener(v -> saveEntry());
    }

    private void pickTime(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            String formatted = String.format(java.util.Locale.US, "%02d:%02d", selectedHour, selectedMinute);
            if (isStart) {
                startTime = formatted;
                tvStartTime.setText(TimeFormatUtil.toDisplay(formatted));
            } else {
                endTime = formatted;
                tvEndTime.setText(TimeFormatUtil.toDisplay(formatted));
            }
        }, hour, minute, false).show();
    }

    private void saveEntry() {
        String subject = etSubject.getText().toString().trim();
        String professor = etProfessor.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        int dayIndex = spinnerDay.getSelectedItemPosition() + 1;

        if (subject.isEmpty()) {
            Toast.makeText(this, "Add a subject name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startTime == null || endTime == null) {
            Toast.makeText(this, "Set both a start and end time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startTime.compareTo(endTime) >= 0) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        btnSave.setEnabled(false);

        Map<String, Object> entry = new HashMap<>();
        entry.put("studentId", uid);
        entry.put("dayIndex", dayIndex);
        entry.put("dayName", TimetableEntry.dayNameFor(dayIndex));
        entry.put("startTime", startTime);
        entry.put("endTime", endTime);
        entry.put("subjectName", subject);
        entry.put("professorName", professor);
        entry.put("room", room);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (existingEntryId != null) {
            db.collection("timetable_entries").document(existingEntryId)
                    .set(entry, SetOptions.merge())
                    .addOnSuccessListener(unused -> finishSuccessfully())
                    .addOnFailureListener(this::onSaveFailed);
        } else {
            db.collection("timetable_entries").add(entry)
                    .addOnSuccessListener(ref -> finishSuccessfully())
                    .addOnFailureListener(this::onSaveFailed);
        }
    }

    private void onSaveFailed(Exception e) {
        btnSave.setEnabled(true);
        Toast.makeText(this, "Couldn't save: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void finishSuccessfully() {
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
