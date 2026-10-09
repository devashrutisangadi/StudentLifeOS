package com.example.studentlifeos;

import java.util.List;

/** Plain-English wording for a {@link MatchResult}. Pure Java. */
public final class MatchText {

    private MatchText() {}

    /** One or two sentences: how many skills match and what the gap is. */
    public static String summary(MatchResult r) {
        if (r.warning != null) return r.warning;
        int have = r.matched.size();
        int total = have + r.missing.size();
        if (total == 0) return "This listing doesn't name specific skills.";
        if (have == 0) {
            return "None of the " + total + " skills it lists are on your profile yet.";
        }
        StringBuilder sb = new StringBuilder("You match ")
                .append(have).append(" of the ").append(total).append(" skills it lists. ");
        List<String> gaps = r.missing;
        if (gaps.isEmpty()) {
            sb.append("You have everything it asks for.");
        } else if (gaps.size() == 1) {
            sb.append("The one gap is ").append(gaps.get(0)).append('.');
        } else if (gaps.size() <= 3) {
            sb.append("The gaps are ").append(join(gaps, gaps.size())).append('.');
        } else {
            sb.append("The biggest gaps are ").append(join(gaps, 3))
                    .append(" and ").append(gaps.size() - 3).append(" more.");
        }
        if (!r.related.isEmpty()) {
            sb.append(' ').append(join(r.related, r.related.size()))
                    .append(r.related.size() == 1 ? " is" : " are")
                    .append(" close to a skill you already know.");
        }
        return sb.toString();
    }

    /** "Python, JavaScript, HTML +2" (first {@code max}, then a count of the rest). */
    public static String listWithMore(List<String> items, int max) {
        if (items.size() <= max) return join(items, items.size());
        return join(items, max) + " +" + (items.size() - max);
    }

    private static String join(List<String> items, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}