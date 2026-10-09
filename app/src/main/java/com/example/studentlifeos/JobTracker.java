package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All of a student's saved and applied listings, keyed by job id. Plain Java (no Android) so it can be
 * unit-tested; {@link TrackerStore} saves and loads it.
 */
public final class JobTracker {

    private final Map<String, TrackedJob> items = new LinkedHashMap<>();

    public TrackedJob get(String jobId) { return items.get(jobId); }

    public boolean isTracked(String jobId) { return items.containsKey(jobId); }

    public int size() { return items.size(); }

    public void put(TrackedJob job) { items.put(job.jobId, job); }

    public TrackedJob remove(String jobId) { return items.remove(jobId); }

    /** Saves a listing. If it is already tracked it is left exactly as it was. */
    public TrackedJob save(String jobId, long now) {
        TrackedJob existing = items.get(jobId);
        if (existing != null) return existing;
        TrackedJob job = TrackedJob.saved(jobId, now);
        items.put(jobId, job);
        return job;
    }

    /** Sets the status, tracking the listing first if it wasn't (e.g. "mark applied" without saving). */
    public TrackedJob setStatus(String jobId, TrackedJob.Status status, long now) {
        TrackedJob current = items.get(jobId);
        if (current == null) current = TrackedJob.saved(jobId, now);
        TrackedJob next = current.withStatus(status, now);
        items.put(jobId, next);
        return next;
    }

    /** Most recently changed first. */
    public List<TrackedJob> all() {
        List<TrackedJob> out = new ArrayList<>(items.values());
        out.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return out;
    }

    /** Most recently changed first; pass null for every status. */
    public List<TrackedJob> withStatus(TrackedJob.Status status) {
        if (status == null) return all();
        List<TrackedJob> out = new ArrayList<>();
        for (TrackedJob j : all()) if (j.status == status) out.add(j);
        return out;
    }

    public int count(TrackedJob.Status status) {
        int n = 0;
        for (TrackedJob j : items.values()) if (j.status == status) n++;
        return n;
    }

    /** From the map stored at students/{uid}.tracker ({jobId: {...}}). Anything unreadable is skipped. */
    public static JobTracker fromMap(Map<?, ?> raw) {
        JobTracker t = new JobTracker();
        if (raw == null) return t;
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            if (e.getKey() == null || !(e.getValue() instanceof Map)) continue;
            t.put(TrackedJob.fromMap(e.getKey().toString(), (Map<?, ?>) e.getValue()));
        }
        return t;
    }
}