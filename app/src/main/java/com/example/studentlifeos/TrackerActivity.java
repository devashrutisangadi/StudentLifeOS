package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * "Saved &amp; applied": every listing the student has saved or applied to, filterable by status.
 * Tap a row to open the listing, where the status can be changed.
 */
public class TrackerActivity extends AppCompatActivity {

    private JobRepository repo;
    private JobTracker tracker = new JobTracker();
    /** null = all statuses */
    private TrackedJob.Status filter = null;
    private String uid;

    private LinearLayout list, filterChips;
    private View filterScroll;
    private ProgressBar progress;
    private TextView intro, empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        list = findViewById(R.id.trackerList);
        filterChips = findViewById(R.id.trackerFilterChips);
        filterScroll = findViewById(R.id.trackerFilterScroll);
        progress = findViewById(R.id.trackerProgress);
        intro = findViewById(R.id.tvTrackerIntro);
        empty = findViewById(R.id.tvTrackerEmpty);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        JobRepository.load(this, new JobRepository.Callback() {
            @Override public void onLoaded(JobRepository r) { repo = r; reload(); }
            @Override public void onError(Exception e) {
                progress.setVisibility(View.GONE);
                Toast.makeText(TrackerActivity.this, "Couldn't load job data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repo != null) reload(); // status may have changed on the listing screen
    }

    private void reload() {
        TrackerStore.load(uid, new TrackerStore.LoadCallback() {
            @Override public void onLoaded(JobTracker t) {
                if (isFinishing() || isDestroyed()) return;
                tracker = t;
                progress.setVisibility(View.GONE);
                render();
            }
            @Override public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                Toast.makeText(TrackerActivity.this, "Couldn't load your tracker: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void render() {
        list.removeAllViews();
        if (tracker.size() == 0) {
            intro.setText("Keep track of the internships you're interested in and where each application stands.");
            filterScroll.setVisibility(View.GONE);
            empty.setText("Nothing here yet.\nOpen a listing in Jobs and tap Save, or mark it as applied.");
            empty.setVisibility(View.VISIBLE);
            return;
        }
        int applied = tracker.size() - tracker.count(TrackedJob.Status.SAVED);
        intro.setText(tracker.size() + (tracker.size() == 1 ? " listing" : " listings") + " · "
                + applied + " applied");
        filterScroll.setVisibility(View.VISIBLE);
        renderFilterChips();

        List<TrackedJob> shown = tracker.withStatus(filter);
        int rows = 0;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (TrackedJob t : shown) {
            Job job = repo.findById(t.jobId);
            if (job == null) continue; // listing no longer in the bundled data
            View row = inflater.inflate(R.layout.item_tracked_job, list, false);
            ((TextView) row.findViewById(R.id.tvTrackedTitle)).setText(job.title);
            ((TextView) row.findViewById(R.id.tvTrackedCompany)).setText(job.company);
            ((TextView) row.findViewById(R.id.tvTrackedDate)).setText(TrackerUi.dateLine(t));
            TrackerUi.styleStatus(row.findViewById(R.id.tvTrackedStatus), t.status);
            row.setOnClickListener(v -> {
                Intent i = new Intent(this, JobDetailActivity.class);
                i.putExtra(JobDetailActivity.EXTRA_JOB_ID, job.id);
                startActivity(i);
            });
            list.addView(row);
            rows++;
        }
        if (rows == 0) {
            empty.setText("Nothing marked " + (filter == null ? "" : filter.label().toLowerCase()) + " yet.");
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    private void renderFilterChips() {
        filterChips.removeAllViews();
        addChip("All " + tracker.size(), filter == null, null);
        for (TrackedJob.Status s : TrackedJob.Status.values()) {
            int n = tracker.count(s);
            if (n == 0 && s != filter) continue; // don't show empty statuses
            addChip(s.label() + " " + n, filter == s, s);
        }
    }

    private void addChip(String label, boolean selected, TrackedJob.Status status) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(selected ? R.drawable.bg_category_pill_selected : R.drawable.bg_category_pill);
        tv.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.papers_category_text_active : R.color.papers_category_text_inactive));
        int h = dp(14), v = dp(8);
        tv.setPadding(h, v, h, v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        tv.setLayoutParams(lp);
        tv.setOnClickListener(x -> {
            filter = status;
            render();
        });
        filterChips.addView(tv);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}