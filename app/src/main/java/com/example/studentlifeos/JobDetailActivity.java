package com.example.studentlifeos;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;

/** One listing in full: fit score, why, skills you have and need, listing check, and a link out. */
public class JobDetailActivity extends AppCompatActivity {

    public static final String EXTRA_JOB_ID = "jobId";

    private String jobId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        jobId = getIntent().getStringExtra(EXTRA_JOB_ID);
        if (jobId == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        JobRepository.load(this, new JobRepository.Callback() {
            @Override public void onLoaded(JobRepository repo) {
                Job job = repo.findById(jobId);
                if (job == null) {
                    Toast.makeText(JobDetailActivity.this, "Listing not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                SkillsProfileStore.load(uid, new SkillsProfileStore.LoadCallback() {
                    @Override public void onLoaded(SkillsProfile p) {
                        bind(Matching.forRepo(repo).match(p, job), p);
                    }
                    @Override public void onError(Exception e) {
                        // still show the listing, scored against an empty profile
                        bind(Matching.forRepo(repo).match(new SkillsProfile(), job), new SkillsProfile());
                    }
                });
            }
            @Override public void onError(Exception e) {
                Toast.makeText(JobDetailActivity.this, "Couldn't load job data", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bind(MatchResult r, SkillsProfile profile) {
        Job job = r.job;
        boolean avoid = r.verdict == MatchResult.Verdict.AVOID;
        boolean noProfile = profile.isEmpty();

        ((TextView) findViewById(R.id.tvDetailScore)).setText(avoid || noProfile ? "–" : r.score + "%");
        JobMatchAdapter.styleVerdict(findViewById(R.id.tvDetailVerdict), r.verdict);
        if (noProfile && !avoid) findViewById(R.id.tvDetailVerdict).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.tvDetailTitle)).setText(job.title);
        ((TextView) findViewById(R.id.tvDetailCompany)).setText(job.company);

        StringBuilder meta = new StringBuilder(job.locationLabel());
        String pay = job.stipendShort();
        if (!pay.isEmpty()) meta.append(" · ").append(pay);
        meta.append(job.partTime ? " · Part time" : " · Internship");
        if (job.incentives) meta.append(" · + incentives");
        ((TextView) findViewById(R.id.tvDetailMeta)).setText(meta);

        TextView summary = findViewById(R.id.tvDetailSummary);
        summary.setText(noProfile ? "Add your skills to see how well you fit this listing." : MatchText.summary(r));

        fillChips(R.id.haveSection, R.id.haveChips, r.matched, false, r);
        fillChips(R.id.missingSection, R.id.missingChips, r.missing, true, r);

        StringBuilder check = new StringBuilder();
        if (r.warning != null) {
            check.append("⚠ ").append(r.warning);
        } else {
            check.append("✓ Listed skills fit the job title");
        }
        if (r.roleMatch) check.append("\n✓ Matches your target role: ").append(job.role);
        else if (profile.getTargetRole() != null) check.append("\n– Role: ").append(job.role)
                .append(" (your target is ").append(profile.getTargetRole()).append(")");
        ((TextView) findViewById(R.id.tvListingCheck)).setText(check);

        TextView desc = findViewById(R.id.tvDetailDescription);
        TextView toggle = findViewById(R.id.tvDescriptionToggle);
        desc.setText(job.description == null || job.description.isEmpty()
                ? "No description in the listing." : job.description);
        desc.post(() -> {
            // only offer "Show more" when the text is actually cut off
            if (desc.getLayout() != null
                    && desc.getLayout().getEllipsisCount(Math.max(0, desc.getLineCount() - 1)) > 0) {
                toggle.setVisibility(View.VISIBLE);
            }
        });
        toggle.setOnClickListener(v -> {
            boolean expanded = desc.getMaxLines() == Integer.MAX_VALUE;
            desc.setMaxLines(expanded ? 8 : Integer.MAX_VALUE);
            toggle.setText(expanded ? "Show more" : "Show less");
        });

        findViewById(R.id.btnOpenListing).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(job.url)));
            } catch (ActivityNotFoundException | NullPointerException e) {
                Toast.makeText(this, "No app can open this link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fillChips(int sectionId, int groupId, java.util.List<String> skills, boolean missing, MatchResult r) {
        View section = findViewById(sectionId);
        ChipGroup group = findViewById(groupId);
        group.removeAllViews();
        section.setVisibility(skills.isEmpty() ? View.GONE : View.VISIBLE);
        for (String s : skills) {
            boolean close = missing && r.related.contains(s);
            TextView tv = new TextView(this);
            tv.setText(close ? s + " (close to one you know)" : s);
            tv.setTextSize(13);
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundResource(missing ? R.drawable.bg_category_pill : R.drawable.bg_category_pill_selected);
            tv.setTextColor(ContextCompat.getColor(this,
                    missing ? R.color.papers_category_text_inactive : R.color.papers_category_text_active));
            int h = Math.round(12 * getResources().getDisplayMetrics().density);
            int v = Math.round(7 * getResources().getDisplayMetrics().density);
            tv.setPadding(h, v, h, v);
            group.addView(tv);
        }
    }
}