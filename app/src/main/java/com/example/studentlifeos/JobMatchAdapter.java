package com.example.studentlifeos;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/** One card per ranked listing in the Jobs feed. */
public class JobMatchAdapter extends RecyclerView.Adapter<JobMatchAdapter.Holder> {

    public interface Listener {
        void onClick(MatchResult result);
    }

    private static final int MAX_HAVE_CHIPS = 3;
    private static final int MAX_MISSING_CHIPS = 3;

    private final List<MatchResult> items = new ArrayList<>();
    private final Listener listener;

    public JobMatchAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<MatchResult> results) {
        items.clear();
        items.addAll(results);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_match, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        MatchResult r = items.get(position);
        Job job = r.job;
        boolean avoid = r.verdict == MatchResult.Verdict.AVOID;

        if (avoid) h.ring.set(0, ringColor(r.verdict), "!");
        else h.ring.set(r.score, ringColor(r.verdict), r.score + "%");

        h.title.setText(job.title);
        styleVerdict(h.verdict, r.verdict);
        h.company.setText(job.company);

        StringBuilder meta = new StringBuilder(job.locationLabel());
        String pay = job.stipendShort();
        if (!pay.isEmpty()) meta.append(" · ").append(pay);
        h.meta.setText(meta);

        h.chips.removeAllViews();
        if (avoid) {
            h.chips.setVisibility(View.GONE);
            h.warning.setVisibility(View.VISIBLE);
            h.warning.setText(r.warning);
        } else {
            h.warning.setVisibility(View.GONE);
            h.chips.setVisibility(View.VISIBLE);
            Context c = h.itemView.getContext();
            addChips(h.chips, c, r.matched, MAX_HAVE_CHIPS, true);
            addChips(h.chips, c, r.missing, MAX_MISSING_CHIPS, false);
        }
        h.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    /** Filled green "✓ Skill" chips for skills you have, outlined "+ Skill" chips for ones to learn. */
    private static void addChips(ChipGroup group, Context c, List<String> skills, int max, boolean have) {
        int shown = Math.min(max, skills.size());
        for (int i = 0; i < shown; i++) group.addView(chip(c, (have ? "✓ " : "+ ") + skills.get(i), have));
        if (skills.size() > shown) group.addView(more(c, "+" + (skills.size() - shown)));
    }

    private static TextView chip(Context c, String text, boolean have) {
        TextView tv = baseChip(c, text);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(100f);
        if (have) {
            bg.setColor(Color.parseColor("#CDEBD3"));
            tv.setTextColor(Color.parseColor("#1F5130"));
        } else {
            bg.setColor(Color.TRANSPARENT);
            bg.setStroke(dp(c, 1), Color.parseColor("#9A94B0"));
            tv.setTextColor(c.getColor(R.color.subject_secondary_text));
        }
        tv.setBackground(bg);
        return tv;
    }

    private static TextView more(Context c, String text) {
        TextView tv = baseChip(c, text);
        tv.setTextColor(c.getColor(R.color.subject_secondary_text));
        return tv;
    }

    private static TextView baseChip(Context c, String text) {
        TextView tv = new TextView(c);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setSingleLine(true);
        tv.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        return tv;
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Ring / accent colour for a verdict. */
    public static int ringColor(MatchResult.Verdict v) {
        switch (v) {
            case APPLY: return Color.parseColor("#3FA66B");
            case WAIT:  return Color.parseColor("#E0A030");
            case AVOID: return Color.parseColor("#D9534F");
            default:    return Color.parseColor("#9A94B0");
        }
    }

    /** Coloured pill for a verdict. Fixed light fills with dark text so it reads in both themes. */
    public static void styleVerdict(TextView tv, MatchResult.Verdict v) {
        int fill, text;
        switch (v) {
            case APPLY: fill = Color.parseColor("#CDEBD3"); text = Color.parseColor("#1F5130"); break;
            case WAIT:  fill = Color.parseColor("#FBE3B8"); text = Color.parseColor("#6B4510"); break;
            case AVOID: fill = Color.parseColor("#F4C7C3"); text = Color.parseColor("#7A1F1A"); break;
            default:    fill = Color.parseColor("#E3E0EC"); text = Color.parseColor("#4A4560"); break;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(100f);
        tv.setBackground(bg);
        tv.setTextColor(text);
        tv.setText(v.label());
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ScoreRingView ring;
        final TextView title, verdict, company, meta, warning;
        final ChipGroup chips;

        Holder(@NonNull View v) {
            super(v);
            ring = v.findViewById(R.id.scoreRing);
            title = v.findViewById(R.id.tvJobTitle);
            verdict = v.findViewById(R.id.tvVerdict);
            company = v.findViewById(R.id.tvJobCompany);
            meta = v.findViewById(R.id.tvJobMeta);
            chips = v.findViewById(R.id.skillChips);
            warning = v.findViewById(R.id.tvWarning);
        }
    }
}