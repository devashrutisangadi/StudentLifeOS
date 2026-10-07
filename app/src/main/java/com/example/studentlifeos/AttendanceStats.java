package com.example.studentlifeos;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashSet;
import java.util.Set;

/**
 * Single source of truth for overall attendance %, computed from the same
 * "attendance" log entries the student marks by hand, so the Home stat, the
 * Life Score and the Attendance screens always agree.
 * (metrics.overallAttendance on the student document is never updated by
 * manual logging, so it must not be used for display.)
 */
public final class AttendanceStats {

    public interface Callback {
        /** percent is 0-100, or null if no lectures have been logged yet (or loading failed). */
        void onResult(Double percent);
    }

    private AttendanceStats() {}

    public static void loadOverall(String uid, Callback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Task<QuerySnapshot> subjectsTask = db.collection("subjects").whereEqualTo("studentId", uid).get();
        Task<QuerySnapshot> logsTask = db.collection("attendance").whereEqualTo("studentId", uid).get();

        Tasks.whenAllSuccess(subjectsTask, logsTask)
                .addOnSuccessListener(results -> {
                    // Only count logs for subjects that still exist (deleting a subject
                    // doesn't delete its logs).
                    Set<String> subjectIds = new HashSet<>();
                    for (DocumentSnapshot s : subjectsTask.getResult().getDocuments()) {
                        subjectIds.add(s.getId());
                    }

                    int held = 0, attended = 0;
                    for (DocumentSnapshot log : logsTask.getResult().getDocuments()) {
                        if (!subjectIds.contains(log.getString("subjectId"))) continue;
                        held++;
                        if (Boolean.TRUE.equals(log.getBoolean("present"))) attended++;
                    }

                    cb.onResult(held == 0 ? null : 100.0 * attended / held);
                })
                .addOnFailureListener(e -> cb.onResult(null));
    }
}