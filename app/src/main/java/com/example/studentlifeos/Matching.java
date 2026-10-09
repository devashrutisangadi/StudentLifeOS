package com.example.studentlifeos;

import java.util.HashMap;
import java.util.Map;

/** Builds the {@link JobMatcher} for the loaded job data once and reuses it. */
public final class Matching {

    private static JobRepository cachedFor;
    private static JobMatcher cached;

    private Matching() {}

    public static synchronized JobMatcher forRepo(JobRepository repo) {
        if (cached == null || cachedFor != repo) {
            Map<String, String> kinds = new HashMap<>();
            for (JobRepository.SkillInfo s : repo.getSkillCatalog()) {
                kinds.put(SkillNormalizer.key(s.name), s.kind);
            }
            cached = new JobMatcher(repo.getJobs(), kinds);
            cachedFor = repo;
        }
        return cached;
    }
}