package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.List;

/** How well one listing fits one student, with the reasons. Produced by {@link JobMatcher}. */
public class MatchResult {

    public enum Verdict {
        APPLY("Apply"), WAIT("Wait"), SKIP("Skip"), AVOID("Avoid");

        private final String label;
        Verdict(String label) { this.label = label; }
        public String label() { return label; }
    }

    public final Job job;
    /** 0-100. */
    public final int score;
    public final Verdict verdict;
    /** Listing skills the student has, in the listing's order. */
    public final List<String> matched = new ArrayList<>();
    /** Skills the student lacks but has a close relative of (knows MySQL, listing wants PostgreSQL). They earn half credit and are also listed in {@code missing}. */
    public final List<String> related = new ArrayList<>();
    /** Core skills still to learn (includes the related ones above). */
    public final List<String> missing = new ArrayList<>();
    /** True when the listing's role equals the student's target role. */
    public final boolean roleMatch;
    /** Why the listing looks unreliable, or null. A non-null warning always means Verdict.AVOID. */
    public final String warning;

    MatchResult(Job job, int score, Verdict verdict, boolean roleMatch, String warning) {
        this.job = job;
        this.score = score;
        this.verdict = verdict;
        this.roleMatch = roleMatch;
        this.warning = warning;
    }
}