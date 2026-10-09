package com.example.studentlifeos;

import android.content.Context;

import java.util.List;

/**
 * Loads the student's skills and job data once and works out what the Life Score and the Home card
 * need: the skills readiness (0-100) and the single best-matching listing.
 * Never fails: if anything can't be loaded the result simply has no skills.
 */
public final class SkillsScoreLoader {

    public static final class Result {
        public final boolean hasSkills;
        /** 0-100, or null when the student has no skills yet (the Life Score counts that as 0). */
        public final Double readiness;
        /** The best non-flagged listing, or null. */
        public final MatchResult top;

        Result(boolean hasSkills, Double readiness, MatchResult top) {
            this.hasSkills = hasSkills;
            this.readiness = readiness;
            this.top = top;
        }
    }

    public interface Callback {
        void onResult(Result result);
    }

    private SkillsScoreLoader() {}

    public static void load(Context context, String uid, Callback cb) {
        Result none = new Result(false, null, null);
        JobRepository.load(context, new JobRepository.Callback() {
            @Override public void onLoaded(JobRepository repo) {
                SkillsProfileStore.load(uid, new SkillsProfileStore.LoadCallback() {
                    @Override public void onLoaded(SkillsProfile profile) {
                        if (profile.isEmpty()) {
                            cb.onResult(none);
                            return;
                        }
                        List<MatchResult> ranked = Matching.forRepo(repo).rank(profile);
                        MatchResult top = null;
                        for (MatchResult r : ranked) {
                            if (r.verdict != MatchResult.Verdict.AVOID) { top = r; break; }
                        }
                        cb.onResult(new Result(true, SkillsReadiness.compute(profile, ranked), top));
                    }
                    @Override public void onError(Exception e) { cb.onResult(none); }
                });
            }
            @Override public void onError(Exception e) { cb.onResult(none); }
        });
    }
}