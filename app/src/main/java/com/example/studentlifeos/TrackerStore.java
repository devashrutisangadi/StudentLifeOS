package com.example.studentlifeos;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.Map;

/**
 * Reads and writes the student's saved and applied internships at students/{uid}.tracker, a map of
 * jobId -> {status, savedAt, appliedAt, updatedAt}. Same document and approach as {@link SkillsProfileStore}.
 */
public final class TrackerStore {

    private TrackerStore() {}

    public interface LoadCallback {
        void onLoaded(JobTracker tracker);
        void onError(Exception e);
    }

    public interface DoneCallback {
        void onDone();
        void onError(Exception e);
    }

    /** Gives an empty tracker (not an error) when the student hasn't saved anything yet. */
    public static void load(String uid, LoadCallback cb) {
        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    Object raw = doc.exists() ? doc.get("tracker") : null;
                    cb.onLoaded(raw instanceof Map ? JobTracker.fromMap((Map<?, ?>) raw) : new JobTracker());
                })
                .addOnFailureListener(cb::onError);
    }

    /** Adds or replaces one entry. Merge-set, so the other entries and the rest of the document are untouched. */
    public static void put(String uid, TrackedJob job, DoneCallback cb) {
        Map<String, Object> entry = Collections.singletonMap(job.jobId, job.toMap());
        FirebaseFirestore.getInstance().collection("students").document(uid)
                .set(Collections.singletonMap("tracker", entry), SetOptions.merge())
                .addOnSuccessListener(v -> { if (cb != null) cb.onDone(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }

    /** Removes one entry. FieldPath keeps ids containing dots or slashes from being read as nested paths. */
    public static void remove(String uid, String jobId, DoneCallback cb) {
        FirebaseFirestore.getInstance().collection("students").document(uid)
                .update(FieldPath.of("tracker", jobId), FieldValue.delete())
                .addOnSuccessListener(v -> { if (cb != null) cb.onDone(); })
                .addOnFailureListener(e -> { if (cb != null) cb.onError(e); });
    }
}