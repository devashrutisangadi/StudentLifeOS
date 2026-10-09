package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Skills component of the Life Score: how well the student's skills fit the internships that suit
 * them best, i.e. the average fit of their top 5 listings (flagged "Avoid" listings are ignored).
 * Pure Java.
 */
public final class SkillsReadiness {

    public static final int TOP_N = 5;

    private SkillsReadiness() {}

    /** 0-100, or null when there is nothing to score (no skills, or no usable listings). */
    public static Double compute(SkillsProfile profile, List<MatchResult> results) {
        if (profile == null || profile.isEmpty() || results == null) return null;
        List<Integer> scores = new ArrayList<>();
        for (MatchResult r : results) {
            if (r.verdict != MatchResult.Verdict.AVOID) scores.add(r.score);
        }
        if (scores.isEmpty()) return null;
        Collections.sort(scores, Collections.reverseOrder());
        int n = Math.min(TOP_N, scores.size());
        double sum = 0;
        for (int i = 0; i < n; i++) sum += scores.get(i);
        return sum / n;
    }
}