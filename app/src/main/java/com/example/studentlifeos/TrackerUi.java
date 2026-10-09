package com.example.studentlifeos;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small shared helpers for showing a tracked listing's status and date. */
public final class TrackerUi {

    private TrackerUi() {}

    /** Coloured pill for a status. Fixed light fills with dark text so it reads in both themes. */
    public static void styleStatus(TextView tv, TrackedJob.Status s) {
        int fill, text;
        switch (s) {
            case APPLIED:      fill = Color.parseColor("#D3E3FA"); text = Color.parseColor("#1B3F73"); break;
            case INTERVIEWING: fill = Color.parseColor("#FBE3B8"); text = Color.parseColor("#6B4510"); break;
            case OFFER:        fill = Color.parseColor("#CDEBD3"); text = Color.parseColor("#1F5130"); break;
            case REJECTED:     fill = Color.parseColor("#F4C7C3"); text = Color.parseColor("#7A1F1A"); break;
            default:           fill = Color.parseColor("#E3E0EC"); text = Color.parseColor("#4A4560"); break;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(100f);
        tv.setBackground(bg);
        tv.setTextColor(text);
        tv.setText(s.label());
    }

    /** "Saved 9 Oct" or "Applied 9 Oct". */
    public static String dateLine(TrackedJob job) {
        String when = new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(job.shownDate()));
        return (job.hasApplied() ? "Applied " : "Saved ") + when;
    }
}