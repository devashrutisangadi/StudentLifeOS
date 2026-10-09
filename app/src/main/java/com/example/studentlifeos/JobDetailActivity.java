package com.example.studentlifeos;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;

/**
 * One listing in full: fit score, why, skills you have and need, listing check, a link out, and the
 * student's own save / applied status for it.
 */
public class JobDetailActivity extends AppCompatActivity {

    public static final String EXTRA_JOB_ID = "jobId";

    private String jobId;
    private String uid;
    private Job currentJob;
    private JobTracker tracker = new JobTracker();
    /** Set when the student leaves for the listing, so we can ask "did you apply?" when they come back. */
    private boolean openedListing = false;

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
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        JobRepository.load(this, new JobRepository.Callback() {
            @Override public void onLoaded(JobRepository repo) {
                Job job = repo.findById(jobId);
                if (job == null) {
                    Toast.makeText(JobDetailActivity.this, "Listing not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                currentJob = job;
                TrackerStore.load(uid, new TrackerStore.LoadCallback() {
                    @Override public void onLoaded(JobTracker t) {
                        tracker = t;
                        loadProfileAndBind(repo, job);
                    }
                    @Override public void onError(Exception e) {
                        loadProfileAndBind(repo, job); // the listing still shows; save/apply start from empty
                    }
                });
            }
            @Override public void onError(Exception e) {
                Toast.makeText(JobDetailActivity.this, "Couldn't load job data", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadProfileAndBind(JobRepository repo, Job job) {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (!openedListing) return;
        openedListing = false;
        TrackedJob t = tracker.get(jobId);
        if (currentJob == null || (t != null && t.hasApplied())) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Did you apply?")
                .setMessage("Mark \"" + currentJob.title + "\" as applied so you can track it.")
                .setPositiveButton("Yes, I applied", (d, w) -> changeStatus(TrackedJob.Status.APPLIED))
                .setNegativeButton("Not yet", null)
                .show();
    }

    private void bind(MatchResult r, SkillsProfile profile) {
        Job job = r.job;
        boolean avoid = r.verdict == MatchResult.Verdict.AVOID;
        boolean noProfile = profile.isEmpty();

        ScoreRingView ring = findViewById(R.id.detailRing);
        if (avoid) ring.set(0, JobMatchAdapter.ringColor(r.verdict), "!");
        else if (noProfile) ring.set(0, JobMatchAdapter.ringColor(r.verdict), "–");
        else ring.set(r.score, JobMatchAdapter.ringColor(r.verdict), r.score + "%");
        findViewById(R.id.tvDetailFit).setVisibility(avoid ? View.INVISIBLE : View.VISIBLE);
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

        findViewById(R.id.btnLearningPlan).setOnClickListener(v -> {
            Intent plan = new Intent(this, LearningPlanActivity.class);
            plan.putExtra(LearningPlanActivity.EXTRA_GOAL, "the " + job.title + " role at " + job.company);
            plan.putExtra(LearningPlanActivity.EXTRA_SKILLS, r.missing.toArray(new String[0]));
            plan.putExtra(LearningPlanActivity.EXTRA_CACHE_KEY, job.id);
            startActivity(plan);
        });

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

        findViewById(R.id.btnSaveJob).setOnClickListener(v -> toggleSaved());
        findViewById(R.id.btnJobStatus).setOnClickListener(v -> pickStatus());
        renderTracking();

        findViewById(R.id.btnOpenListing).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(job.url)));
                openedListing = true;
            } catch (ActivityNotFoundException | NullPointerException e) {
                Toast.makeText(this, "No app can open this link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ------------------------------------------------------------------ save / applied tracking

    private void renderTracking() {
        Button save = findViewById(R.id.btnSaveJob);
        Button status = findViewById(R.id.btnJobStatus);
        TextView note = findViewById(R.id.tvTrackNote);
        TrackedJob t = tracker.get(jobId);
        if (t == null) {
            save.setText("Save");
            status.setText("Mark as applied");
            note.setVisibility(View.GONE);
        } else {
            save.setText("Saved ✓");
            status.setText(t.hasApplied() ? "Status: " + t.status.label() + " ▾" : "Mark as applied");
            note.setText(TrackerUi.dateLine(t) + (t.hasApplied() ? " · tap Status to update" : ""));
            note.setVisibility(View.VISIBLE);
        }
    }

    /** Save, or (after confirming if it has progressed past Saved) remove from the tracker. */
    private void toggleSaved() {
        TrackedJob t = tracker.get(jobId);
        if (t == null) {
            apply(tracker.save(jobId, System.currentTimeMillis()), null);
        } else if (t.hasApplied()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Remove from tracker?")
                    .setMessage("This listing is marked " + t.status.label().toLowerCase()
                            + ". Removing it deletes that history.")
                    .setPositiveButton("Remove", (d, w) -> removeTracked(t))
                    .setNegativeButton("Keep", null)
                    .show();
        } else {
            removeTracked(t);
        }
    }

    private void removeTracked(TrackedJob previous) {
        tracker.remove(jobId);
        renderTracking();
        TrackerStore.remove(uid, jobId, new TrackerStore.DoneCallback() {
            @Override public void onDone() {}
            @Override public void onError(Exception e) {
                tracker.put(previous); // the save didn't go through, so put it back
                renderTracking();
                Toast.makeText(JobDetailActivity.this, "Couldn't update: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void pickStatus() {
        TrackedJob.Status[] all = TrackedJob.Status.values();
        String[] labels = new String[all.length];
        TrackedJob t = tracker.get(jobId);
        int checked = t == null ? -1 : t.status.ordinal();
        for (int i = 0; i < all.length; i++) labels[i] = all[i].label();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Application status")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    d.dismiss();
                    changeStatus(all[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void changeStatus(TrackedJob.Status status) {
        TrackedJob before = tracker.get(jobId);
        apply(tracker.setStatus(jobId, status, System.currentTimeMillis()), before);
    }

    /** Shows the new state straight away, saves it, and puts the old state back if saving fails. */
    private void apply(TrackedJob updated, TrackedJob before) {
        renderTracking();
        TrackerStore.put(uid, updated, new TrackerStore.DoneCallback() {
            @Override public void onDone() {}
            @Override public void onError(Exception e) {
                if (before == null) tracker.remove(jobId); else tracker.put(before);
                renderTracking();
                Toast.makeText(JobDetailActivity.this, "Couldn't save: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
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