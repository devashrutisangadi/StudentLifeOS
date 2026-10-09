package com.example.studentlifeos;

import java.util.List;

/** Builds the Groq prompt for a learning plan. Pure Java. */
public final class LearningPlanPrompt {

    public static final int MAX_SKILLS_TO_LEARN = 6;
    private static final int MAX_SKILLS_SHOWN = 12;

    private LearningPlanPrompt() {}

    /**
     * @param goal    what the student is aiming for, e.g. "the Backend Development Intern role at Acme"
     * @param have    skills the student already has
     * @param missing skills to learn (only the first {@link #MAX_SKILLS_TO_LEARN} are used)
     */
    public static String build(String goal, List<StudentSkill> have, List<String> missing) {
        StringBuilder haveText = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_SKILLS_SHOWN, have.size()); i++) {
            if (i > 0) haveText.append(", ");
            haveText.append(have.get(i).name).append(" (").append(have.get(i).levelLabel()).append(')');
        }
        if (haveText.length() == 0) haveText.append("none listed yet");

        StringBuilder learn = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_SKILLS_TO_LEARN, missing.size()); i++) {
            if (i > 0) learn.append(", ");
            learn.append(missing.get(i));
        }

        return "You are a practical career mentor for an Indian engineering student looking for internships.\n"
                + "Goal: " + goal + ".\n"
                + "Skills the student already has: " + haveText + ".\n"
                + "Skills to learn: " + learn + ".\n\n"
                + "Write a realistic learning plan. Put the skills in the order they should be learned, "
                + "building on what the student already knows. For each skill give: one sentence on why it "
                + "matters for this goal, exactly 3 concrete actions, an honest time estimate in days "
                + "(assume 2 hours a day alongside college), and the name of one free resource "
                + "(official documentation or a well-known free course). Do NOT write URLs. "
                + "Finish with one small portfolio project, buildable in a weekend, that uses several of these skills.\n\n"
                + "Respond with JSON in exactly this shape: "
                + "{\"overview\": \"one or two encouraging sentences\", "
                + "\"steps\": [{\"skill\": \"...\", \"why\": \"...\", \"days\": 3, "
                + "\"actions\": [\"...\", \"...\", \"...\"], \"resource\": \"...\"}], "
                + "\"project\": \"...\"}. No extra commentary.";
    }
}