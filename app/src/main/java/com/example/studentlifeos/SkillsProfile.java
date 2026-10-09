package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The student's skills and target role. Plain Java so it can be unit-tested; it converts to and
 * from the map stored under students/{uid}.skills in Firestore.
 */
public class SkillsProfile {

    private final List<StudentSkill> skills = new ArrayList<>();
    /** One of the role buckets in jobs.json (e.g. "Backend"), or null when the student hasn't picked one. */
    private String targetRole;

    public List<StudentSkill> getSkills() { return skills; }

    public String getTargetRole() { return targetRole; }

    public void setTargetRole(String role) {
        targetRole = (role == null || role.trim().isEmpty()) ? null : role.trim();
    }

    public boolean isEmpty() { return skills.isEmpty(); }

    public StudentSkill find(String name) {
        String k = SkillNormalizer.key(name);
        for (StudentSkill s : skills) if (SkillNormalizer.key(s.name).equals(k)) return s;
        return null;
    }

    public boolean has(String name) { return find(name) != null; }

    /** Adds a skill. Returns false (and changes nothing) if it is blank or already there. */
    public boolean add(String name, int level) {
        if (name == null || name.trim().isEmpty() || has(name)) return false;
        skills.add(new StudentSkill(name.trim(), level));
        return true;
    }

    public boolean remove(String name) {
        StudentSkill s = find(name);
        return s != null && skills.remove(s);
    }

    public void replaceWith(SkillsProfile other) {
        skills.clear();
        skills.addAll(other.skills);
        targetRole = other.targetRole;
    }

    /** Lower-cased keys of every skill, for "already added" checks. */
    public Set<String> keys() {
        Set<String> out = new LinkedHashSet<>();
        for (StudentSkill s : skills) out.add(SkillNormalizer.key(s.name));
        return out;
    }

    // ------------------------------------------------------------ Firestore shape

    /** {items:[{name,level}], targetRole, updatedAt}. Nested maps only, so merge-set keeps it intact. */
    public Map<String, Object> toMap() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (StudentSkill s : skills) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", s.name);
            m.put("level", s.level);
            items.add(m);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        out.put("targetRole", targetRole);
        out.put("updatedAt", System.currentTimeMillis());
        return out;
    }

    /** Tolerant of missing or odd values: anything unreadable is skipped. */
    public static SkillsProfile fromMap(Map<String, Object> map) {
        SkillsProfile p = new SkillsProfile();
        if (map == null) return p;
        Object items = map.get("items");
        if (items instanceof List) {
            for (Object o : (List<?>) items) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                Object name = m.get("name");
                Object level = m.get("level");
                if (name == null) continue;
                p.add(name.toString(), level instanceof Number ? ((Number) level).intValue() : StudentSkill.BEGINNER);
            }
        }
        Object role = map.get("targetRole");
        if (role != null) p.setTargetRole(role.toString());
        return p;
    }
}