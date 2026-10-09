package com.example.studentlifeos;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * Breakdown screen for the Student Life Score: the overall number plus each weighted
 * component (attendance, syllabus completion, CGPA, skills readiness) with its raw value and weight,
 * so the student can see exactly what's driving their score. Reached by tapping the Life Score
 * card on Home.
 */
public class LifeScoreActivity extends AppCompatActivity {

    private TextView tvScoreBig, tvScoreLabel;
    private TextView tvAttendanceValue, tvSyllabusValue, tvCgpaValue, tvSkillsValue, tvSkillsHint;
    private ProgressBar progressAttendance, progressSyllabus, progressCgpa, progressSkills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_life_score);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvScoreBig = findViewById(R.id.tvScoreBig);
        tvScoreLabel = findViewById(R.id.tvScoreLabel);
        tvAttendanceValue = findViewById(R.id.tvAttendanceValue);
        tvSyllabusValue = findViewById(R.id.tvSyllabusValue);
        tvCgpaValue = findViewById(R.id.tvCgpaValue);
        tvSkillsValue = findViewById(R.id.tvSkillsValue);
        tvSkillsHint = findViewById(R.id.tvSkillsHint);
        progressAttendance = findViewById(R.id.progressAttendance);
        progressSyllabus = findViewById(R.id.progressSyllabus);
        progressCgpa = findViewById(R.id.progressCgpa);
        progressSkills = findViewById(R.id.progressSkills);

        ((TextView) findViewById(R.id.tvAttendanceWeight)).setText(LifeScoreCalculator.ATTENDANCE_WEIGHT + "% of score");
        ((TextView) findViewById(R.id.tvSyllabusWeight)).setText(LifeScoreCalculator.SYLLABUS_WEIGHT + "% of score");
        ((TextView) findViewById(R.id.tvCgpaWeight)).setText(LifeScoreCalculator.CGPA_WEIGHT + "% of score");
        ((TextView) findViewById(R.id.tvSkillsWeight)).setText(LifeScoreCalculator.SKILLS_WEIGHT + "% of score");

        findViewById(R.id.rowSkills).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, SkillsProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndComputeScore(); // also refreshes after the student edits their skills
    }

    private void loadAndComputeScore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener(studentDoc -> loadSyllabusAndCompute(uid, studentDoc))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load your data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void loadSyllabusAndCompute(String uid, DocumentSnapshot studentDoc) {
        Map<String, Object> metrics = (Map<String, Object>) studentDoc.get("metrics");
        Double cgpa = metrics != null ? LifeScoreUtil.toDouble(metrics.get("cpi")) : null;

        AttendanceStats.loadOverall(uid, attendance ->
                FirebaseFirestore.getInstance().collection("subjects")
                        .whereEqualTo("studentId", uid)
                        .get()
                        .addOnSuccessListener(subjectsSnapshot -> {
                            double syllabusPercent = LifeScoreUtil.averageProgress(subjectsSnapshot);
                            withSkills(uid, attendance, syllabusPercent, cgpa);
                        })
                        .addOnFailureListener(e ->
                                // Still show a score from what we have rather than blocking entirely.
                                withSkills(uid, attendance, 0.0, cgpa)));
    }

    private void withSkills(String uid, Double attendance, double syllabusPercent, Double cgpa) {
        SkillsScoreLoader.load(this, uid, result -> {
            bindScore(LifeScoreCalculator.compute(attendance, syllabusPercent, cgpa, result.readiness));
            tvSkillsHint.setVisibility(result.hasSkills ? View.GONE : View.VISIBLE);
        });
    }

    private void bindScore(LifeScoreCalculator.Breakdown b) {
        tvScoreBig.setText(String.valueOf(b.score));
        tvScoreLabel.setText(LifeScoreCalculator.label(b.score));

        tvAttendanceValue.setText(Math.round(b.attendancePercent) + "%");
        tvSyllabusValue.setText(Math.round(b.syllabusPercent) + "%");
        tvCgpaValue.setText(Math.round(b.cgpaAsPercent) + "%");
        tvSkillsValue.setText(Math.round(b.skillsPercent) + "%");

        progressAttendance.setProgress((int) Math.round(b.attendancePercent));
        progressSyllabus.setProgress((int) Math.round(b.syllabusPercent));
        progressCgpa.setProgress((int) Math.round(b.cgpaAsPercent));
        progressSkills.setProgress((int) Math.round(b.skillsPercent));
    }
}