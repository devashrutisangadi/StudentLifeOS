package com.example.studentlifeos;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.Map;

/** Reads and writes the student's skills profile at students/{uid}.skills. */
public final class SkillsProfileStore {

    private SkillsProfileStore() {}

    public interface LoadCallback {
        void onLoaded(SkillsProfile profile);
        void onError(Exception e);
    }

    public interface SaveCallback {
        void onSaved();
        void onError(Exception e);
    }

    /** Gives an empty profile (not an error) when the student hasn't saved any skills yet. */
    @SuppressWarnings("unchecked")
    public static void load(String uid, LoadCallback cb) {
        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    Object raw = doc.exists() ? doc.get("skills") : null;
                    cb.onLoaded(raw instanceof Map
                            ? SkillsProfile.fromMap((Map<String, Object>) raw)
                            : new SkillsProfile());
                })
                .addOnFailureListener(cb::onError);
    }

    /** Merge-set with a nested map, so the rest of the student document is untouched. */
    public static void save(String uid, SkillsProfile profile, SaveCallback cb) {
        FirebaseFirestore.getInstance().collection("students").document(uid)
                .set(Collections.singletonMap("skills", profile.toMap()), SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSaved())
                .addOnFailureListener(cb::onError);
    }
}