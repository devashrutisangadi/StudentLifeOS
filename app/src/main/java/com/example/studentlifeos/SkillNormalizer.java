package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns whatever a student types ("reactjs", "HTML & CSS", "node") into the same canonical
 * skill names the job listings use ("React", "HTML" + "CSS", "Node.js"), so matching compares
 * like with like. Pure Java: the alias table and canonical names come from assets/jobs.json.
 */
public final class SkillNormalizer {

    private final Map<String, List<String>> aliases = new HashMap<>();
    private final Map<String, String> canonicalByLower = new HashMap<>();

    /**
     * @param aliases   lower-case alias -> canonical names (as written in jobs.json)
     * @param canonical every canonical skill name known from the job data
     */
    public SkillNormalizer(Map<String, List<String>> aliases, Collection<String> canonical) {
        for (Map.Entry<String, List<String>> e : aliases.entrySet()) {
            this.aliases.put(key(e.getKey()), e.getValue());
        }
        for (String name : canonical) {
            canonicalByLower.put(key(name), name);
        }
    }

    /** Lower-cases and collapses whitespace. */
    public static String key(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Canonical names for one raw skill. Empty for blank input. Unknown skills are kept as typed. */
    public List<String> normalize(String raw) {
        List<String> out = new ArrayList<>();
        String k = key(raw);
        if (k.isEmpty()) return out;

        List<String> viaAlias = aliases.get(k);
        if (viaAlias != null) {
            out.addAll(viaAlias);
            return out;
        }
        String known = canonicalByLower.get(k);
        out.add(known != null ? known : raw.trim().replaceAll("\\s+", " "));
        return out;
    }

    /** Normalises many raw skills, removing duplicates (case-insensitively) and keeping order. */
    public List<String> normalizeAll(Collection<String> raws) {
        List<String> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (String raw : raws) {
            for (String name : normalize(raw)) {
                String k = key(name);
                if (!seen.contains(k)) {
                    seen.add(k);
                    out.add(name);
                }
            }
        }
        return out;
    }

    /** True if this text maps to a skill that appears in the job data. */
    public boolean isKnown(String raw) {
        for (String name : normalize(raw)) {
            if (!canonicalByLower.containsKey(key(name))) return false;
        }
        return !normalize(raw).isEmpty();
    }
}