package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Suggests skills a student probably already has, from the text of their subject names and syllabus
 * units. Two sources: a skill name appearing as a whole word ("Python", "Data Structures"), and a
 * small phrase table for topics that imply skills ("database" -> SQL, DBMS). Only skills that exist
 * in the job data are ever suggested.
 */
public final class SkillSuggester {

    private SkillSuggester() {}

    /** lower-case phrase found in the text -> skills it implies (silently dropped if not in the catalog). */
    private static final Map<String, String[]> PHRASES = new LinkedHashMap<>();
    static {
        PHRASES.put("database", new String[]{"SQL", "DBMS"});
        PHRASES.put("dbms", new String[]{"DBMS", "SQL"});
        PHRASES.put("web technolog", new String[]{"HTML", "CSS", "JavaScript"});
        PHRASES.put("web development", new String[]{"HTML", "CSS", "JavaScript"});
        PHRASES.put("web programming", new String[]{"HTML", "CSS", "JavaScript"});
        PHRASES.put("data structure", new String[]{"Data Structures", "Algorithms"});
        PHRASES.put("algorithm", new String[]{"Algorithms"});
        PHRASES.put("machine learning", new String[]{"Machine Learning", "Python"});
        PHRASES.put("deep learning", new String[]{"Deep Learning", "Neural Networks"});
        PHRASES.put("neural network", new String[]{"Neural Networks"});
        PHRASES.put("artificial intelligence", new String[]{"Artificial Intelligence"});
        PHRASES.put("natural language", new String[]{"NLP"});
        PHRASES.put("data mining", new String[]{"Data Analytics"});
        PHRASES.put("data warehouse", new String[]{"Data Analytics", "SQL"});
        PHRASES.put("data science", new String[]{"Data Science", "Python"});
        PHRASES.put("big data", new String[]{"Hadoop", "Data Analytics"});
        PHRASES.put("cloud computing", new String[]{"Cloud Computing"});
        PHRASES.put("computer network", new String[]{"Computer Networking"});
        PHRASES.put("operating system", new String[]{"Linux"});
        PHRASES.put("software testing", new String[]{"Software Testing"});
        PHRASES.put("software engineering", new String[]{"Software Testing"});
        PHRASES.put("advanced java", new String[]{"Java", "J2EE"});
        PHRASES.put("object oriented", new String[]{"Java"});
        PHRASES.put("c programming", new String[]{"C"});
        PHRASES.put("programming in c", new String[]{"C"});
        PHRASES.put("mobile application", new String[]{"Android"});
        PHRASES.put("internet of things", new String[]{"IoT"});
        PHRASES.put("statistics", new String[]{"Statistics"});
        PHRASES.put("probability", new String[]{"Statistics"});
    }

    /**
     * @param texts       subject names and unit titles
     * @param catalog     candidate skill names, most common first (pass technical skills only)
     * @param ownedKeys   {@link SkillNormalizer#key} of skills the student already has
     * @param max         most suggestions to return
     */
    public static List<String> suggest(Collection<String> texts, List<String> catalog,
                                       Set<String> ownedKeys, int max) {
        StringBuilder all = new StringBuilder();
        for (String t : texts) if (t != null) all.append(t).append('\n');
        String text = all.toString();
        String lower = text.toLowerCase(Locale.ROOT);

        Map<String, String> catalogByKey = new HashMap<>();
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < catalog.size(); i++) {
            String k = SkillNormalizer.key(catalog.get(i));
            catalogByKey.put(k, catalog.get(i));
            rank.put(k, i);
        }

        Map<String, Integer> hits = new LinkedHashMap<>();

        // 1. the skill's own name appears as a whole word
        for (String name : catalog) {
            if (name.length() <= 2) continue; // "C", "R", "Go": too ambiguous, handled by phrases
            boolean shortName = name.length() <= 3; // "SQL", "SEM", "Git": must match case exactly, so "Sem 5" isn't SEM
            Pattern p = Pattern.compile("(?<![A-Za-z0-9+#.])" + Pattern.quote(name) + "(?![A-Za-z0-9+#])",
                    shortName ? 0 : Pattern.CASE_INSENSITIVE);
            int n = 0;
            java.util.regex.Matcher m = p.matcher(text);
            while (m.find()) n++;
            if (n > 0) hits.merge(SkillNormalizer.key(name), n, Integer::sum);
        }

        // 2. topic phrases
        for (Map.Entry<String, String[]> e : PHRASES.entrySet()) {
            int idx = 0, n = 0;
            while ((idx = lower.indexOf(e.getKey(), idx)) >= 0) { n++; idx += e.getKey().length(); }
            if (n == 0) continue;
            for (String skill : e.getValue()) {
                String k = SkillNormalizer.key(skill);
                if (catalogByKey.containsKey(k)) hits.merge(k, n, Integer::sum);
            }
        }

        List<String> keys = new ArrayList<>(hits.keySet());
        keys.removeIf(ownedKeys::contains);
        keys.sort((a, b) -> {
            int byHits = Integer.compare(hits.get(b), hits.get(a));
            return byHits != 0 ? byHits : Integer.compare(rank.get(a), rank.get(b));
        });

        List<String> out = new ArrayList<>();
        for (String k : keys) {
            if (out.size() >= max) break;
            out.add(catalogByKey.get(k));
        }
        return out;
    }
}