package com.example.studentlifeos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillNormalizerTest {

    private SkillNormalizer sample() {
        Map<String, List<String>> aliases = new HashMap<>();
        aliases.put("html&css", Arrays.asList("HTML", "CSS"));
        aliases.put("reactjs", Collections.singletonList("React"));
        aliases.put("node", Collections.singletonList("Node.js"));
        return new SkillNormalizer(aliases,
                Arrays.asList("HTML", "CSS", "React", "Node.js", "Python", "MySQL"));
    }

    @Test public void aliasMapsToCanonicalName() {
        assertEquals(Collections.singletonList("React"), sample().normalize("ReactJS"));
        assertEquals(Collections.singletonList("Node.js"), sample().normalize("  node "));
    }

    @Test public void combinedSkillSplitsIntoTwo() {
        assertEquals(Arrays.asList("HTML", "CSS"), sample().normalize("HTML&CSS"));
    }

    @Test public void caseInsensitiveMatchUsesCanonicalSpelling() {
        assertEquals(Collections.singletonList("Python"), sample().normalize("python"));
        assertEquals(Collections.singletonList("MySQL"), sample().normalize("MYSQL"));
    }

    @Test public void unknownSkillIsKeptAsTyped() {
        assertEquals(Collections.singletonList("Kotlin Coroutines"), sample().normalize(" Kotlin   Coroutines "));
    }

    @Test public void blankInputGivesNothing() {
        assertTrue(sample().normalize("   ").isEmpty());
        assertTrue(sample().normalize(null).isEmpty());
    }

    @Test public void normalizeAllRemovesDuplicatesAndKeepsOrder() {
        List<String> out = sample().normalizeAll(Arrays.asList("html", "HTML&CSS", "python", "Python"));
        assertEquals(Arrays.asList("HTML", "CSS", "Python"), out);
    }

    @Test public void isKnownOnlyForSkillsInTheJobData() {
        assertTrue(sample().isKnown("reactjs"));
        assertFalse(sample().isKnown("Kotlin Coroutines"));
    }
}