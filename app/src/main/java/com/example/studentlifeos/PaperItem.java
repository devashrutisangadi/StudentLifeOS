package com.example.studentlifeos;

public class PaperItem {

    public enum Type { PDF, NOTE }

    public final String id;
    public final Type type;
    public final String title;
    public final String subtitle; // e.g. "Vaswani et al. · 2017" or "Edited 2 hours ago"
    public final String tag;      // e.g. "ML", "Thesis" — nullable
    public final int progressPercent; // 0-100, only meaningful for PDF type

    public PaperItem(String id, Type type, String title, String subtitle,
                     String tag, int progressPercent) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.tag = tag;
        this.progressPercent = progressPercent;
    }
}