package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which missing skill would help the most? For every technical skill the student lacks, pretends they
 * learned it (at Intermediate) and counts the listings whose verdict would move up a tier.
 * Pure Java; reuses {@link JobMatcher}.
 */
public final class SkillGapAnalyzer {

    private SkillGapAnalyzer() {}

    public static final class Gap {
        public final String skill;
        /** Listings that would move up a tier (Skip -> Wait/Apply, Wait -> Apply). */
        public final int unlocks;
        /** Of those, how many would become Apply. */
        public final int becomeApply;
        /** Listings that ask for this skill at all. */
        public final int asking;

        Gap(String skill, int unlocks, int becomeApply, int asking) {
            this.skill = skill;
            this.unlocks = unlocks;
            this.becomeApply = becomeApply;
            this.asking = asking;
        }
    }

    private static int tier(MatchResult.Verdict v) {
        switch (v) {
            case APPLY: return 2;
            case WAIT: return 1;
            default: return 0;
        }
    }

    public static List<Gap> topGaps(SkillsProfile profile, JobMatcher matcher, Collection<Job> jobs,
                                    Map<String, String> kindByKey, int limit) {
        List<Gap> out = new ArrayList<>();
        if (profile.isEmpty()) return out;

        // technical skill -> display name + the listings asking for it
        Map<String, String> display = new LinkedHashMap<>();
        Map<String, List<Job>> asking = new HashMap<>();
        for (Job j : jobs) {
            for (String s : j.skills) {
                String k = SkillNormalizer.key(s);
                String kind = kindByKey.get(k);
                if (kind != null && !"technical".equals(kind)) continue;
                display.putIfAbsent(k, s);
                asking.computeIfAbsent(k, x -> new ArrayList<>()).add(j);
            }
        }

        Map<Job, MatchResult.Verdict> before = new HashMap<>();
        for (Job j : jobs) before.put(j, matcher.match(profile, j).verdict);

        java.util.Set<String> owned = profile.keys();
        for (Map.Entry<String, String> e : display.entrySet()) {
            if (owned.contains(e.getKey())) continue;
            SkillsProfile with = new SkillsProfile();
            with.replaceWith(profile);
            with.add(e.getValue(), StudentSkill.INTERMEDIATE);

            int unlocks = 0, becomeApply = 0;
            List<Job> list = asking.get(e.getKey());
            for (Job j : list) {
                MatchResult.Verdict was = before.get(j);
                if (was == MatchResult.Verdict.AVOID) continue;
                MatchResult.Verdict now = matcher.match(with, j).verdict;
                if (tier(now) > tier(was)) {
                    unlocks++;
                    if (now == MatchResult.Verdict.APPLY) becomeApply++;
                }
            }
            out.add(new Gap(e.getValue(), unlocks, becomeApply, list.size()));
        }

        out.sort((a, b) -> {
            if (a.unlocks != b.unlocks) return Integer.compare(b.unlocks, a.unlocks);
            if (a.becomeApply != b.becomeApply) return Integer.compare(b.becomeApply, a.becomeApply);
            if (a.asking != b.asking) return Integer.compare(b.asking, a.asking);
            return a.skill.compareToIgnoreCase(b.skill);
        });
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }
}