package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the skills mentioned in the text of a CV. Runs entirely on the device and only ever returns
 * skills that exist in the job data, so everything it finds can affect the student's matches.
 *
 * Three kinds of match:
 *  - a skill's canonical name as a whole word ("Python", "Data Structures");
 *  - an alias from the job data ("reactjs" -> React, "node" -> Node.js);
 *  - one- and two-letter names ("C", "R", "Go"), which only count when they sit in a delimited list
 *    ("Languages: C, Java, Go") so that "Grade C" or "Go to market" don't match.
 *
 * Plain Java (no Android) so it can be unit-tested.
 */
public final class CvSkillExtractor {

    private CvSkillExtractor() {}

    /** A skill found in the CV. */
    public static final class Found {
        public final String name;
        /** How many times the CV mentions it (aliases and the canonical name are added together). */
        public final int mentions;
        /** Starting level: Beginner for one or two mentions, Intermediate for three or more. */
        public final int suggestedLevel;

        Found(String name, int mentions) {
            this.name = name;
            this.mentions = mentions;
            this.suggestedLevel = mentions >= 3 ? StudentSkill.INTERMEDIATE : StudentSkill.BEGINNER;
        }
    }

    public static final class Result {
        /** Skills found that the student does not have yet, most-mentioned first. */
        public final List<Found> fresh;
        /** How many found skills the student already had (not listed again). */
        public final int alreadyHad;

        Result(List<Found> fresh, int alreadyHad) {
            this.fresh = fresh;
            this.alreadyHad = alreadyHad;
        }
    }

    /**
     * Words that are also ordinary English or very generic. Matched only with their exact
     * capitalisation, so "Express.js"/"Spark" can match but "express interest" or "spark" cannot.
     */
    private static final Set<String> CASE_SENSITIVE_NAMES = new HashSet<>();
    static {
        for (String s : new String[]{"express", "swift", "spark", "unity", "processing", "dart", "ruby",
                "rust", "flask", "excel", "word", "office", "design", "testing", "networking", "react",
                "angular", "bootstrap", "go", "r", "c"}) {
            CASE_SENSITIVE_NAMES.add(s);
        }
    }

    /** Characters that can sit before / after an item in a comma- or bullet-separated skills list. */
    private static final String LIST_BEFORE = ",;/|•·●▪■◦*:(\\-–—";
    private static final String LIST_AFTER = ",;/|)•·●▪■◦*";

    /**
     * @param text       the CV text
     * @param catalog    canonical technical skill names, most common first (the rank breaks ties)
     * @param aliases    lower-case alias -> canonical names, as held by {@link SkillNormalizer}
     * @param ownedKeys  {@link SkillNormalizer#key} of skills the student already has
     */
    public static Result extract(String text, List<String> catalog,
                                 Map<String, List<String>> aliases, Set<String> ownedKeys) {
        if (text == null || text.trim().isEmpty()) return new Result(new ArrayList<>(), 0);

        Map<String, String> nameByKey = new HashMap<>();
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < catalog.size(); i++) {
            String k = SkillNormalizer.key(catalog.get(i));
            nameByKey.put(k, catalog.get(i));
            rank.put(k, i);
        }

        Map<String, Integer> hits = new LinkedHashMap<>();

        // 1. canonical names
        for (String name : catalog) {
            int n = count(text, name, false);
            if (n > 0) hits.merge(SkillNormalizer.key(name), n, Integer::sum);
        }

        // 2. aliases (skipped when the alias is just the canonical name again)
        if (aliases != null) {
            for (Map.Entry<String, List<String>> e : aliases.entrySet()) {
                String alias = e.getKey();
                if (nameByKey.containsKey(SkillNormalizer.key(alias))) continue;
                int n = count(text, alias, true);
                if (n == 0) continue;
                for (String canonical : e.getValue()) {
                    String k = SkillNormalizer.key(canonical);
                    if (nameByKey.containsKey(k)) hits.merge(k, n, Integer::sum);
                }
            }
        }

        List<String> keys = new ArrayList<>(hits.keySet());
        keys.sort((a, b) -> {
            int byHits = Integer.compare(hits.get(b), hits.get(a));
            return byHits != 0 ? byHits : Integer.compare(rank.get(a), rank.get(b));
        });

        List<Found> fresh = new ArrayList<>();
        int had = 0;
        for (String k : keys) {
            if (ownedKeys.contains(k)) had++;
            else fresh.add(new Found(nameByKey.get(k), hits.get(k)));
        }
        return new Result(fresh, had);
    }

    /**
     * Whole-word occurrences of one skill name or alias in the text.
     *
     * @param isAlias true for an alias (written in any case in a CV), false for a canonical name
     */
    static int count(String text, String name, boolean isAlias) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return 0;
        boolean ambiguous = CASE_SENSITIVE_NAMES.contains(SkillNormalizer.key(trimmed));

        // spaces in the name match any run of whitespace, so a PDF line break inside "Data\nStructures" still counts
        StringBuilder body = new StringBuilder();
        for (String part : trimmed.split("\\s+")) {
            if (body.length() > 0) body.append("\\s+");
            body.append(Pattern.quote(part));
        }

        Pattern p;
        if (trimmed.length() <= 2 || (isAlias && ambiguous)) {
            // "C", "R", "Go", or an everyday word used as an alias ("express"): only inside a delimited list
            int flags = Pattern.MULTILINE | (isAlias ? Pattern.CASE_INSENSITIVE : 0);
            p = Pattern.compile("(?:^|[" + LIST_BEFORE + "])[ \\t]*" + body + "[ \\t]*(?=$|[" + LIST_AFTER + "])", flags);
        } else {
            // "SQL", "Git", "AWS" and everyday words ("Swift", "Spark") must be written with that exact case
            boolean exactCase = ambiguous || trimmed.length() <= 3;
            p = Pattern.compile("(?<![A-Za-z0-9+#.])" + body + "(?![A-Za-z0-9+#])",
                    exactCase && !isAlias ? 0 : Pattern.CASE_INSENSITIVE);
        }
        Matcher m = p.matcher(text);
        int n = 0;
        while (m.find()) n++;
        return n;
    }
}