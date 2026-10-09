package com.example.studentlifeos;

/**
 * Computes the 0–100 weighted "Student Life Score" from data already tracked elsewhere in
 * the app: attendance %, syllabus completion %, CGPA, and skills readiness (how well the student's
 * skills fit the internships in the Jobs tab). Pure logic, no Android/Firestore dependencies, so it's
 * easy to reuse from both HomeFragment (the dashboard card) and LifeScoreActivity (the breakdown screen).
 *
 * Weights, ranked by priority: Attendance > Syllabus completion > CGPA > Skills.
 */
public class LifeScoreCalculator {

    public static final int ATTENDANCE_WEIGHT = 40;
    public static final int SYLLABUS_WEIGHT = 30;
    public static final int CGPA_WEIGHT = 20;
    public static final int SKILLS_WEIGHT = 10;
    // Sum = 100.

    public static final double CGPA_SCALE_MAX = 10.0; // standard 0–10 CGPA scale

    public static class Breakdown {
        public final int score;
        public final double attendancePercent;
        public final double syllabusPercent;
        public final double cgpaAsPercent; // CGPA normalized to 0–100, for display alongside the others
        public final double skillsPercent; // skills readiness, 0–100

        Breakdown(int score, double attendancePercent, double syllabusPercent,
                  double cgpaAsPercent, double skillsPercent) {
            this.score = score;
            this.attendancePercent = attendancePercent;
            this.syllabusPercent = syllabusPercent;
            this.cgpaAsPercent = cgpaAsPercent;
            this.skillsPercent = skillsPercent;
        }
    }

    /**
     * @param attendancePercent 0–100, or null if unknown (treated as 0)
     * @param syllabusPercent   0–100, average completion across the student's subjects, or null (treated as 0)
     * @param cgpaOn10Scale     0–10, or null (treated as 0)
     * @param skillsReadiness   0–100 from {@link SkillsReadiness}, or null if the student has no skills yet (treated as 0)
     */
    public static Breakdown compute(Double attendancePercent, Double syllabusPercent,
                                    Double cgpaOn10Scale, Double skillsReadiness) {
        double attendance = clamp(nullToZero(attendancePercent), 0, 100);
        double syllabus = clamp(nullToZero(syllabusPercent), 0, 100);
        double cgpaPercent = clamp((nullToZero(cgpaOn10Scale) / CGPA_SCALE_MAX) * 100.0, 0, 100);
        double skills = clamp(nullToZero(skillsReadiness), 0, 100);

        double weighted = attendance * (ATTENDANCE_WEIGHT / 100.0)
                + syllabus * (SYLLABUS_WEIGHT / 100.0)
                + cgpaPercent * (CGPA_WEIGHT / 100.0)
                + skills * (SKILLS_WEIGHT / 100.0);

        int score = (int) Math.round(clamp(weighted, 0, 100));
        return new Breakdown(score, attendance, syllabus, cgpaPercent, skills);
    }

    /** Short human label for the score, used on both the Home card and the breakdown screen. */
    public static String label(int score) {
        if (score >= 85) return "Excellent";
        if (score >= 70) return "Great";
        if (score >= 50) return "Good";
        if (score >= 30) return "Needs attention";
        return "At risk";
    }

    private static double nullToZero(Double v) {
        return v != null ? v : 0.0;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}