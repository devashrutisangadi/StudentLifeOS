package com.example.studentlifeos;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** One card per ranked listing in the Jobs feed. */
public class JobMatchAdapter extends RecyclerView.Adapter<JobMatchAdapter.Holder> {

    public interface Listener {
        void onClick(MatchResult result);
    }

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

        h.score.setText(avoid ? "–" : r.score + "%");
        h.title.setText(job.title);
        styleVerdict(h.verdict, r.verdict);

        StringBuilder meta = new StringBuilder(job.company).append(" · ").append(job.locationLabel());
        String pay = job.stipendShort();
        if (!pay.isEmpty()) meta.append(" · ").append(pay);
        h.meta.setText(meta);

        if (avoid) {
            h.have.setVisibility(View.GONE);
            h.missing.setVisibility(View.GONE);
            h.warning.setVisibility(View.VISIBLE);
            h.warning.setText(r.warning);
        } else {
            h.warning.setVisibility(View.GONE);
            show(h.have, r.matched.isEmpty() ? null : "You have: " + MatchText.listWithMore(r.matched, 4));
            show(h.missing, r.missing.isEmpty() ? null : "To learn: " + MatchText.listWithMore(r.missing, 3));
        }
        h.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    private static void show(TextView tv, String text) {
        tv.setVisibility(text == null ? View.GONE : View.VISIBLE);
        if (text != null) tv.setText(text);
    }

    @Override
    public int getItemCount() {
        return items.size();
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
        final TextView score, title, verdict, meta, have, missing, warning;

        Holder(@NonNull View v) {
            super(v);
            score = v.findViewById(R.id.tvScore);
            title = v.findViewById(R.id.tvJobTitle);
            verdict = v.findViewById(R.id.tvVerdict);
            meta = v.findViewById(R.id.tvJobMeta);
            have = v.findViewById(R.id.tvHave);
            missing = v.findViewById(R.id.tvMissing);
            warning = v.findViewById(R.id.tvWarning);
        }
    }
}