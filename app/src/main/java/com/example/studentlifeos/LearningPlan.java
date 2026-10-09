package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.List;

/** A short learning plan for closing skill gaps. Plain data + markdown rendering, so it is easy to test. */
public class LearningPlan {

    public static class Step {
        public String skill = "";
        public String why = "";
        public int days = 0;
        public final List<String> actions = new ArrayList<>();
        public String resource = "";
    }

    public String overview = "";
    public final List<Step> steps = new ArrayList<>();
    public String project = "";

    public int totalDays() {
        int sum = 0;
        for (Step s : steps) sum += Math.max(0, s.days);
        return sum;
    }

    /** Markdown for Markwon: overview, one section per skill in learning order, then a weekend project. */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        if (!overview.isEmpty()) sb.append(overview).append("\n\n");
        if (totalDays() > 0) sb.append("**About ").append(totalDays()).append(" days in total**\n\n");
        int n = 1;
        for (Step s : steps) {
            sb.append("## ").append(n++).append(". ").append(s.skill);
            if (s.days > 0) sb.append(" · ~").append(s.days).append(s.days == 1 ? " day" : " days");
            sb.append("\n\n");
            if (!s.why.isEmpty()) sb.append(s.why).append("\n\n");
            for (String a : s.actions) if (!a.trim().isEmpty()) sb.append("- ").append(a.trim()).append('\n');
            if (!s.actions.isEmpty()) sb.append('\n');
            if (!s.resource.isEmpty()) sb.append("**Free resource:** ").append(s.resource).append("\n\n");
        }
        if (!project.isEmpty()) sb.append("## Weekend project\n\n").append(project).append('\n');
        return sb.toString().trim();
    }
}