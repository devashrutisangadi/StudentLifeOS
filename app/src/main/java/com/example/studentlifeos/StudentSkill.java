package com.example.studentlifeos;

/** One skill the student has, with a self-rated level (1 = Beginner, 2 = Intermediate, 3 = Advanced). */
public class StudentSkill {

    public static final int BEGINNER = 1;
    public static final int INTERMEDIATE = 2;
    public static final int ADVANCED = 3;

    public final String name;
    public int level;

    public StudentSkill(String name, int level) {
        this.name = name;
        this.level = clamp(level);
    }

    public static int clamp(int level) {
        return Math.max(BEGINNER, Math.min(ADVANCED, level));
    }

    public String levelLabel() {
        switch (level) {
            case ADVANCED: return "Advanced";
            case INTERMEDIATE: return "Intermediate";
            default: return "Beginner";
        }
    }

    /** "●○○ Beginner", "●●○ Intermediate", "●●● Advanced". */
    public String levelDots() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= ADVANCED; i++) sb.append(i <= level ? '●' : '○');
        return sb + " " + levelLabel();
    }

    /** How much this skill counts when matching against a listing (used by the matching engine). */
    public double weight() {
        switch (level) {
            case ADVANCED: return 1.0;
            case INTERMEDIATE: return 0.8;
            default: return 0.5;
        }
    }

    /** Beginner -> Intermediate -> Advanced -> Beginner. */
    public void cycleLevel() {
        level = level >= ADVANCED ? BEGINNER : level + 1;
    }
}