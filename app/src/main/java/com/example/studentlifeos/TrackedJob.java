package com.example.studentlifeos;

import java.util.HashMap;
import java.util.Map;

/**
 * One internship the student has saved or applied to. Immutable: change it with {@link #withStatus},
 * which returns a new copy. Plain Java so it can be unit-tested; converts to and from the map stored in
 * Firestore under students/{uid}.tracker.{jobId}.
 */
public final class TrackedJob {

    /** Where the student is with a listing. Order is the order shown in the UI. */
    public enum Status {
        SAVED("Saved"),
        APPLIED("Applied"),
        INTERVIEWING("Interviewing"),
        OFFER("Offer"),
        REJECTED("Rejected");

        private final String label;

        Status(String label) { this.label = label; }

        public String label() { return label; }

        /** Unknown or missing names fall back to SAVED, so a bad value never hides a listing. */
        public static Status parse(String name) {
            if (name != null) {
                for (Status s : values()) if (s.name().equals(name)) return s;
            }
            return SAVED;
        }
    }

    public final String jobId;
    public final Status status;
    /** When the student first saved it (millis). */
    public final long savedAt;
    /** When they first marked it applied (millis), or 0 if they never have. */
    public final long appliedAt;
    /** Last change (millis); the tracker is sorted by this. */
    public final long updatedAt;

    public TrackedJob(String jobId, Status status, long savedAt, long appliedAt, long updatedAt) {
        this.jobId = jobId;
        this.status = status == null ? Status.SAVED : status;
        this.savedAt = savedAt;
        this.appliedAt = appliedAt;
        this.updatedAt = updatedAt;
    }

    /** A newly saved listing. */
    public static TrackedJob saved(String jobId, long now) {
        return new TrackedJob(jobId, Status.SAVED, now, 0, now);
    }

    /**
     * Moves to another status. The applied date is set the first time the student moves past Saved
     * and kept after that; going back to Saved clears it (they un-applied).
     */
    public TrackedJob withStatus(Status next, long now) {
        long applied;
        if (next == Status.SAVED) applied = 0;
        else applied = appliedAt != 0 ? appliedAt : now;
        return new TrackedJob(jobId, next, savedAt, applied, now);
    }

    /** True once the student has applied (any status after Saved). */
    public boolean hasApplied() { return status != Status.SAVED; }

    /** The date to show next to the status: when it was saved, or when they applied. */
    public long shownDate() { return hasApplied() && appliedAt != 0 ? appliedAt : savedAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", status.name());
        m.put("savedAt", savedAt);
        m.put("appliedAt", appliedAt);
        m.put("updatedAt", updatedAt);
        return m;
    }

    /** Tolerant of missing or odd values. */
    public static TrackedJob fromMap(String jobId, Map<?, ?> m) {
        Object status = m.get("status");
        long saved = num(m.get("savedAt"));
        long applied = num(m.get("appliedAt"));
        long updated = num(m.get("updatedAt"));
        return new TrackedJob(jobId, Status.parse(status == null ? null : status.toString()),
                saved, applied, updated != 0 ? updated : Math.max(saved, applied));
    }

    private static long num(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }
}