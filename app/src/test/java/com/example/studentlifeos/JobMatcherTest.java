package com.example.studentlifeos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobMatcherTest {

    private static Job job(String id, String role, String... skills) {
        Job j = new Job();
        j.id = id; j.title = id; j.company = "Co"; j.role = role; j.description = "";
        j.skills = new ArrayList<>(Arrays.asList(skills));
        return j;
    }

    private static Map<String, String> kinds() {
        Map<String, String> m = new HashMap<>();
        m.put("effective communication", "soft");
        m.put("english proficiency (spoken)", "language");
        return m;
    }

    private static SkillsProfile profile(Object... nameLevel) {
        SkillsProfile p = new SkillsProfile();
        for (int i = 0; i < nameLevel.length; i += 2) p.add((String) nameLevel[i], (Integer) nameLevel[i + 1]);
        return p;
    }

    private static MatchResult match(SkillsProfile p, Job j) {
        return new JobMatcher(Collections.singletonList(j), kinds()).match(p, j);
    }

    @Test public void fullAdvancedMatchScoresHundredAndApply() {
        MatchResult r = match(profile("Python", 3, "SQL", 3, "Git", 3), job("a", "Backend", "Python", "SQL", "Git"));
        assertEquals(100, r.score);
        assertEquals(MatchResult.Verdict.APPLY, r.verdict);
        assertTrue(r.missing.isEmpty());
    }

    @Test public void levelsChangeTheScore() {
        Job j = job("a", "Backend", "Python", "SQL", "Git");
        assertEquals(50, match(profile("Python", 1, "SQL", 1, "Git", 1), j).score); // all Beginner
        assertEquals(80, match(profile("Python", 2, "SQL", 2, "Git", 2), j).score); // all Intermediate
    }

    @Test public void oneSkillListingCannotReachTheTop() {
        MatchResult r = match(profile("Python", 3), job("a", "Backend", "Python"));
        assertEquals(33, r.score);
        assertEquals(MatchResult.Verdict.SKIP, r.verdict);
    }

    @Test public void softAndLanguageSkillsAreIgnored() {
        Job j = job("a", "Backend", "Python", "SQL", "Git", "Effective Communication", "English Proficiency (Spoken)");
        MatchResult r = match(profile("Python", 3, "SQL", 3, "Git", 3), j);
        assertEquals(100, r.score);
        assertTrue(r.missing.isEmpty());
    }

    @Test public void listingWithOnlySoftSkillsFallsBackToAllSkills() {
        MatchResult r = match(profile("Effective Communication", 3), job("a", "Business & Other", "Effective Communication"));
        assertEquals(33, r.score);
    }

    @Test public void targetRoleAddsTenAndIsCapped() {
        Job j = job("a", "Backend", "Python", "SQL", "Git", "Java");
        SkillsProfile p = profile("Python", 2, "SQL", 2); // 1.6/4 = 40
        assertEquals(40, match(p, j).score);
        p.setTargetRole("Backend");
        MatchResult r = match(p, j);
        assertEquals(50, r.score);
        assertTrue(r.roleMatch);
        SkillsProfile full = profile("Python", 3, "SQL", 3, "Git", 3, "Java", 3);
        full.setTargetRole("Backend");
        assertEquals(100, match(full, j).score);
    }

    @Test public void relatedSqlSkillGetsHalfCredit() {
        // knows MySQL (Advanced) -> PostgreSQL listing counts 0.5 of 3 skills
        Job j = job("a", "Backend", "PostgreSQL", "Python", "Git");
        MatchResult r = match(profile("MySQL", 3, "Python", 3, "Git", 3), j);
        assertEquals(83, r.score); // (0.5 + 1 + 1) / 3
        assertEquals(Collections.singletonList("PostgreSQL"), r.related);
        assertTrue(r.missing.contains("PostgreSQL"));
    }

    @Test public void emptyProfileScoresZero() {
        assertEquals(0, match(new SkillsProfile(), job("a", "Backend", "Python")).score);
    }

    @Test public void listingsThatAskForMoneyAreAvoided() {
        Job j = job("a", "Backend", "Python", "SQL", "Git");
        j.description = "A small REGISTRATION FEE of Rs 500 is required.";
        MatchResult r = match(profile("Python", 3, "SQL", 3, "Git", 3), j);
        assertEquals(MatchResult.Verdict.AVOID, r.verdict);
        assertNotNull(r.warning);
    }

    @Test public void titleSkillMismatchIsFlaggedOnlyWhenTheRoleIsBigEnough() {
        List<Job> all = new ArrayList<>();
        // 20 backend listings sharing 12 common skills, so those 12 are what "typical for Backend" means
        for (int i = 0; i < 20; i++) all.add(job("b" + i, "Backend", "Python", "SQL", "Git", "Java", "Docker", "Linux",
                "Spring", "Django", "Flask", "Redis", "AWS", "Kafka"));
        Job odd = job("odd", "Backend", "SEO", "Canva", "Blogging");
        all.add(odd);
        JobMatcher m = new JobMatcher(all, kinds());
        assertNotNull(m.listingWarning(odd));
        assertNull(m.listingWarning(all.get(0)));

        JobMatcher small = new JobMatcher(Collections.singletonList(odd), kinds()); // role too small to judge
        assertNull(small.listingWarning(odd));
    }

    @Test public void rankPutsBestFirstAndFlaggedLast() {
        Job great = job("great", "Backend", "Python", "SQL", "Git");
        Job ok = job("ok", "Backend", "Python", "Java", "Docker");
        Job scam = job("scam", "Backend", "Python", "SQL", "Git");
        scam.description = "security deposit needed";
        JobMatcher m = new JobMatcher(Arrays.asList(ok, scam, great), kinds());
        List<MatchResult> ranked = m.rank(profile("Python", 3, "SQL", 3, "Git", 3));
        assertEquals("great", ranked.get(0).job.id);
        assertEquals("ok", ranked.get(1).job.id);
        assertEquals("scam", ranked.get(2).job.id);
    }
}