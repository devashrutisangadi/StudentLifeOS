package com.example.studentlifeos;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceDetailActivity extends AppCompatActivity {

    private static final double THRESHOLD = 0.75; // 75%, per project spec

    private String subjectId, subjectName;
    private Long totalLecturesPlanned;
    private LinearLayout logsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_detail);

        subjectId = getIntent().getStringExtra("subjectId");
        subjectName = getIntent().getStringExtra("subjectName");

        ((TextView) findViewById(R.id.tvSubjectTitle)).setText(subjectName != null ? subjectName : "Attendance");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        logsContainer = findViewById(R.id.logsContainer);

        findViewById(R.id.btnMarkPresent).setOnClickListener(v -> pickDateAndLog(true));
        findViewById(R.id.btnMarkAbsent).setOnClickListener(v -> pickDateAndLog(false));

        findViewById(R.id.btnSaveTotalLectures).setOnClickListener(v -> saveTotalLectures());

        loadSubjectAndLogs();
    }

    private void loadSubjectAndLogs() {
        FirebaseFirestore.getInstance().collection("subjects").document(subjectId).get()
                .addOnSuccessListener(doc -> {
                    Object total = doc.get("totalLecturesPlanned");
                    totalLecturesPlanned = total != null ? ((Number) total).longValue() : null;
                    loadLogs();
                });
    }

    private void loadLogs() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("attendance")
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("studentId", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(this::bindLogs)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bindLogs(QuerySnapshot snapshot) {
        logsContainer.removeAllViews();
        int attended = 0, held = 0;
        SimpleDateFormat displayFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

        List<DocumentSnapshot> docs = snapshot.getDocuments();
        findViewById(R.id.tvNoLogs).setVisibility(docs.isEmpty() ? View.VISIBLE : View.GONE);

        for (DocumentSnapshot doc : docs) {
            boolean present = Boolean.TRUE.equals(doc.getBoolean("present"));
            Timestamp date = doc.getTimestamp("date");
            held++;
            if (present) attended++;

            View row = LayoutInflater.from(this).inflate(R.layout.item_attendance_log, logsContainer, false);
            TextView tvDate = row.findViewById(R.id.tvLogDate);
            TextView tvStatus = row.findViewById(R.id.tvLogStatus);
            tvDate.setText(date != null ? displayFormat.format(date.toDate()) : "—");
            tvStatus.setText(present ? "Present" : "Absent");
            tvStatus.setTextColor(getResources().getColor(present ? R.color.checkbox_tint : android.R.color.holo_red_light, getTheme()));
            logsContainer.addView(row);
        }

        updateStatsAndPrediction(attended, held);
    }

    private void updateStatsAndPrediction(int attended, int held) {
        TextView tvCurrentAttendance = findViewById(R.id.tvCurrentAttendance);
        TextView tvLecturesCount = findViewById(R.id.tvLecturesCount);
        TextView tvPredictionText = findViewById(R.id.tvPredictionText);
        LinearLayout rowSetTotalLectures = findViewById(R.id.rowSetTotalLectures);

        double currentPercent = held == 0 ? 0 : (100.0 * attended / held);
        tvCurrentAttendance.setText(String.format(Locale.getDefault(), "%.0f%%", currentPercent));
        tvLecturesCount.setText(attended + " / " + held);

        if (totalLecturesPlanned == null) {
            tvPredictionText.setText("Prediction unavailable — set total lectures planned for this semester below.");
            rowSetTotalLectures.setVisibility(View.VISIBLE);
            return;
        }
        rowSetTotalLectures.setVisibility(View.GONE);

        long remainingLectures = totalLecturesPlanned - held;
        if (remainingLectures <= 0) {
            tvPredictionText.setText(currentPercent >= THRESHOLD * 100
                    ? "Semester complete — you met the 75% requirement."
                    : "Semester complete — attendance fell short of the 75% requirement.");
            return;
        }

        // Formula: required future attendance = ((threshold × total future lectures) − current attended) / remaining lectures
        double requiredFutureRate = ((THRESHOLD * totalLecturesPlanned) - attended) / remainingLectures;

        if (requiredFutureRate <= 0) {
            // Already safely above threshold even attending zero more — compute safe leaves
            int safeLeaves = 0;
            int simulatedAttended = attended;
            int simulatedHeld = held;
            while (simulatedHeld < totalLecturesPlanned) {
                simulatedHeld++; // a future lecture, missed
                double projected = 100.0 * simulatedAttended / simulatedHeld;
                if (projected < THRESHOLD * 100) break;
                safeLeaves++;
            }
            tvPredictionText.setText("On track. You can safely miss " + safeLeaves
                    + " more lecture" + (safeLeaves == 1 ? "" : "s") + " and still meet 75%.");
        } else if (requiredFutureRate > 1) {
            tvPredictionText.setText("Shortage alert: even attending every remaining lecture, you can't reach 75% this semester.");
        } else {
            long lecturesNeeded = Math.round(requiredFutureRate * remainingLectures);
            tvPredictionText.setText("You need to attend at least " + lecturesNeeded + " of the remaining "
                    + remainingLectures + " lectures to stay above 75%.");
        }
    }

    private void pickDateAndLog(boolean present) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day, 0, 0, 0);
            logLecture(picked.getTime(), present);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void logLecture(java.util.Date date, boolean present) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> log = new HashMap<>();
        log.put("studentId", uid);
        log.put("subjectId", subjectId);
        log.put("date", new Timestamp(date));
        log.put("present", present);
        log.put("source", "self");

        FirebaseFirestore.getInstance().collection("attendance")
                .add(log)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Logged", Toast.LENGTH_SHORT).show();
                    loadLogs();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't log: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveTotalLectures() {
        EditText etTotal = findViewById(R.id.etSetTotalLectures);
        String text = etTotal.getText().toString().trim();
        if (text.isEmpty()) return;

        long total = Long.parseLong(text);
        FirebaseFirestore.getInstance().collection("subjects").document(subjectId)
                .set(java.util.Collections.singletonMap("totalLecturesPlanned", total), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    totalLecturesPlanned = total;
                    loadLogs();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}