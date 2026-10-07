package com.example.studentlifeos;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

/** Small shared helpers used by both HomeFragment (the Life Score card) and
 *  LifeScoreActivity (the breakdown screen), so the two stay consistent. */
public class LifeScoreUtil {

    /** Firestore numeric fields can come back as Long, Double, or Integer — normalize to Double. */
    public static Double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return null;
    }

    /** Average of the "progress" field (0–100) across a student's subjects. Subjects with no
     *  progress field are treated as 0% complete rather than excluded, since an un-started
     *  subject genuinely is 0% through its syllabus. Returns 0 if the student has no subjects
     *  at all (rather than null, so the score always has something to compute against). */
    public static double averageProgress(QuerySnapshot subjectsSnapshot) {
        if (subjectsSnapshot == null || subjectsSnapshot.isEmpty()) return 0.0;

        double total = 0;
        int count = 0;
        for (QueryDocumentSnapshot doc : subjectsSnapshot) {
            Double progress = toDouble(doc.get("progress"));
            total += (progress != null ? progress : 0.0);
            count++;
        }
        return count > 0 ? total / count : 0.0;
    }
}