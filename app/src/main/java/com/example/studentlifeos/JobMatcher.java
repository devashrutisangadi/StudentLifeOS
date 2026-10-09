package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * On-device matching: scores every listing against a student's skills. Pure Java, no Android.
 *
 * Score (0-100):
 *   coverage = sum of weights of the listing's core skills the student has / max(skills asked, 3)
 *              (weight: Beginner 0.5, Intermediate 0.8, Advanced 1.0; a close relative of the skill
 *               counts half of that)
 *   score    = coverage * 100, plus 10 if the listing's role is the student's target role, capped at 100
 * "Core" skills are the technical ones; soft and language skills ("Effective Communication",
 * "English Proficiency") don't count for or against anyone. Listings asking for fewer than 3 skills are
 * treated as asking for 3, so a one-skill listing can't top the ranking on a single match.
 *
 * Verdict: Apply >= 70, Wait >= 45, otherwise Skip. Avoid overrides all of these when the listing
 * itself looks unreliable: it asks for money, or none of its skills fit its title (compared with the
 * typical skills of that role in the data, and with the title's own words).
 */
public final class JobMatcher {

    public static final int APPLY_AT = 70;
    public static final int WAIT_AT = 45;
    static final int MIN_SKILLS_ASKED = 3;
    static final int ROLE_BONUS = 10;
    static final double RELATED_CREDIT = 0.5;

    /** Skills close enough that knowing one is partial credit for another. */
    private static final List<List<String>> FAMILIES = Arrays.asList(
            Arrays.asList("SQL", "MySQL", "PostgreSQL", "MS SQL Server"),
            Arrays.asList("Firebase", "Cloud Firestore"));

    private static final String[] SCAM_PHRASES = {
            "registration fee", "security deposit", "joining fee", "training fee", "pay to join",
            "pay a fee", "investment required", "refundable deposit"};

    // title-vs-skills check
    private static final int MIN_ROLE_SIZE = 15;   // smaller roles have no reliable "typical skills"
    private static final int TYPICAL_TOP_N = 12;   // a role's typical skills = its 12 most-requested
    /** Developer roles share typical skills ("Full Stack" listings legitimately ask for Java/Spring). */
    private static final List<String> DEV_ROLES =
            Arrays.asList("Web", "Frontend", "Backend", "Full Stack", "Software");

    private final Map<String, String> kindByKey;
    private final Map<String, Integer> roleSize = new HashMap<>();
    private final Map<String, java.util.Set<String>> typicalSkills = new HashMap<>();
    private final List<Job> jobs;

    /**
     * @param jobs      every listing (also used to learn which skills are typical for each role)
     * @param kindByKey {@link SkillNormalizer#key} of a skill -> "technical" | "soft" | "language"
     */
    public JobMatcher(Collection<Job> jobs, Map<String, String> kindByKey) {
        this.jobs = new ArrayList<>(jobs);
        this.kindByKey = kindByKey;
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        for (Job j : jobs) {
            roleSize.merge(j.role, 1, Integer::sum);
            Map<String, Integer> c = counts.computeIfAbsent(j.role, r -> new HashMap<>());
            for (String s : j.skills) c.merge(SkillNormalizer.key(s), 1, Integer::sum);
        }
        Map<String, java.util.Set<String>> top = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : counts.entrySet()) {
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(e.getValue().entrySet());
            sorted.sort((a, b) -> {
                int byCount = Integer.compare(b.getValue(), a.getValue());
                return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
            });
            java.util.Set<String> set = new java.util.HashSet<>();
            for (int i = 0; i < Math.min(TYPICAL_TOP_N, sorted.size()); i++) set.add(sorted.get(i).getKey());
            top.put(e.getKey(), set);
        }
        for (String role : roleSize.keySet()) {
            java.util.Set<String> typical = new java.util.HashSet<>(top.get(role));
            if (DEV_ROLES.contains(role)) {
                for (String dev : DEV_ROLES) if (top.containsKey(dev)) typical.addAll(top.get(dev));
            }
            typicalSkills.put(role, typical);
        }
    }

    /** Every listing scored and sorted best first (ties: more matched skills, then higher stipend). */
    public List<MatchResult> rank(SkillsProfile profile) {
        List<MatchResult> out = new ArrayList<>(jobs.size());
        for (Job j : jobs) out.add(match(profile, j));
        out.sort((a, b) -> {
            int byAvoid = Boolean.compare(a.verdict == MatchResult.Verdict.AVOID, b.verdict == MatchResult.Verdict.AVOID);
            if (byAvoid != 0) return byAvoid;                       // flagged listings sink
            int byScore = Integer.compare(b.score, a.score);
            if (byScore != 0) return byScore;
            int byMatched = Integer.compare(b.matched.size(), a.matched.size());
            if (byMatched != 0) return byMatched;
            return Integer.compare(stipend(b.job), stipend(a.job));
        });
        return out;
    }

    public MatchResult match(SkillsProfile profile, Job job) {
        Map<String, StudentSkill> owned = new HashMap<>();
        for (StudentSkill s : profile.getSkills()) owned.put(SkillNormalizer.key(s.name), s);

        List<String> core = coreSkills(job);

        List<String> matched = new ArrayList<>(), related = new ArrayList<>(), missing = new ArrayList<>();
        double credit = 0;
        for (String skill : core) {
            StudentSkill have = owned.get(SkillNormalizer.key(skill));
            if (have != null) {
                credit += have.weight();
                matched.add(skill);
                continue;
            }
            StudentSkill relative = closestRelative(skill, owned);
            if (relative != null) {
                credit += relative.weight() * RELATED_CREDIT;
                related.add(skill);
            } else {
                missing.add(skill);
            }
        }
        // a related skill is still something to learn, so it is listed under missing as well
        missing.addAll(0, related);

        double coverage = credit / Math.max(core.size(), MIN_SKILLS_ASKED);
        boolean roleMatch = profile.getTargetRole() != null && profile.getTargetRole().equals(job.role);
        int score = (int) Math.round(Math.min(100.0, coverage * 100 + (roleMatch ? ROLE_BONUS : 0)));
        if (profile.isEmpty()) score = 0;

        String warning = listingWarning(job);
        MatchResult.Verdict verdict;
        if (warning != null) verdict = MatchResult.Verdict.AVOID;
        else if (score >= APPLY_AT) verdict = MatchResult.Verdict.APPLY;
        else if (score >= WAIT_AT) verdict = MatchResult.Verdict.WAIT;
        else verdict = MatchResult.Verdict.SKIP;

        MatchResult r = new MatchResult(job, score, verdict, roleMatch, warning);
        r.matched.addAll(matched);
        r.related.addAll(related);
        r.missing.addAll(missing);
        return r;
    }

    /** Null when the listing looks fine, otherwise a short reason to be careful. */
    public String listingWarning(Job job) {
        String text = ((job.title == null ? "" : job.title) + " " + (job.description == null ? "" : job.description))
                .toLowerCase(Locale.ROOT);
        for (String phrase : SCAM_PHRASES) {
            if (text.contains(phrase)) return "Asks for money (\"" + phrase + "\"). Genuine internships don't charge you.";
        }
        Integer size = roleSize.get(job.role);
        if (size != null && size >= MIN_ROLE_SIZE) {
            List<String> core = coreSkills(job);
            if (core.size() >= MIN_SKILLS_ASKED) {
                java.util.Set<String> typical = typicalSkills.get(job.role);
                String title = job.title == null ? "" : job.title.toLowerCase(Locale.ROOT);
                boolean fits = false;
                for (String skill : core) {
                    if (typical.contains(SkillNormalizer.key(skill)) || title.contains(SkillNormalizer.key(skill))) {
                        fits = true;
                        break;
                    }
                }
                if (!fits) {
                    return "None of the listed skills fit the \"" + job.title + "\" title, so check the original listing.";
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- helpers

    /** Technical skills of a listing; everything if it has none. */
    private List<String> coreSkills(Job job) {
        List<String> core = new ArrayList<>();
        for (String s : job.skills) if ("technical".equals(kindOf(s))) core.add(s);
        if (core.isEmpty()) core.addAll(job.skills);
        return core;
    }

    private String kindOf(String skill) {
        String k = kindByKey.get(SkillNormalizer.key(skill));
        return k == null ? "technical" : k;
    }

    /** The student's strongest skill from the same family as {@code skill}, or null. */
    private static StudentSkill closestRelative(String skill, Map<String, StudentSkill> owned) {
        String key = SkillNormalizer.key(skill);
        for (List<String> family : FAMILIES) {
            boolean inFamily = false;
            for (String f : family) if (SkillNormalizer.key(f).equals(key)) inFamily = true;
            if (!inFamily) continue;
            StudentSkill best = null;
            for (String f : family) {
                StudentSkill s = owned.get(SkillNormalizer.key(f));
                if (s != null && (best == null || s.level > best.level)) best = s;
            }
            return best;
        }
        return null;
    }

    private static int stipend(Job j) {
        return j.stipendMax == null ? 0 : j.stipendMax;
    }
}