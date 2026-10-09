package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/** The Jobs tab: internships ranked by how well they fit the student's skills. */
public class JobsFragment extends Fragment {

    private enum VerdictFilter { ALL, APPLY, WAIT, SKIP }

    private static final int PAID_AT_LEAST = 10000;

    private VerdictFilter verdictFilter = VerdictFilter.ALL;
    private boolean remoteOnly = false;
    private boolean paidOnly = false;

    private SkillsProfile profile = new SkillsProfile();
    private List<MatchResult> ranked = new ArrayList<>();
    private int totalJobs = 0;

    private JobMatchAdapter adapter;
    private LinearLayout filterChips, statsRow;
    private View filterScroll, emptyBox;
    private RecyclerView recycler;
    private TextView tvSubtitle, tvCount, tvEmptyTitle, tvEmptyBody;
    private View btnAddSkills;
    private ProgressBar progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_jobs, container, false);

        filterChips = v.findViewById(R.id.filterChips);
        statsRow = v.findViewById(R.id.statsRow);
        filterScroll = v.findViewById(R.id.filterScroll);
        emptyBox = v.findViewById(R.id.jobsEmpty);
        recycler = v.findViewById(R.id.recyclerJobs);
        tvSubtitle = v.findViewById(R.id.tvJobsSubtitle);
        tvCount = v.findViewById(R.id.tvJobsCount);
        tvEmptyTitle = v.findViewById(R.id.tvJobsEmptyTitle);
        tvEmptyBody = v.findViewById(R.id.tvJobsEmptyBody);
        btnAddSkills = v.findViewById(R.id.btnAddSkillsEmpty);
        progress = v.findViewById(R.id.jobsProgress);

        adapter = new JobMatchAdapter(result -> {
            Intent i = new Intent(requireContext(), JobDetailActivity.class);
            i.putExtra(JobDetailActivity.EXTRA_JOB_ID, result.job.id);
            startActivity(i);
        });
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        View.OnClickListener openSkills = x ->
                startActivity(new Intent(requireContext(), SkillsProfileActivity.class));
        v.findViewById(R.id.btnMySkills).setOnClickListener(openSkills);
        v.findViewById(R.id.btnTracker).setOnClickListener(x ->
                startActivity(new Intent(requireContext(), TrackerActivity.class)));
        v.findViewById(R.id.btnSkillGap).setOnClickListener(x ->
                startActivity(new Intent(requireContext(), SkillGapActivity.class)));
        btnAddSkills.setOnClickListener(openSkills);

        renderFilterChips();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        load(); // also runs when coming back from the skills screen, so changes show straight away
    }

    private void load() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (ranked.isEmpty()) progress.setVisibility(View.VISIBLE);

        JobRepository.load(requireContext(), new JobRepository.Callback() {
            @Override public void onLoaded(JobRepository repo) {
                SkillsProfileStore.load(uid, new SkillsProfileStore.LoadCallback() {
                    @Override public void onLoaded(SkillsProfile p) {
                        if (!isAdded()) return;
                        profile = p;
                        totalJobs = repo.getJobs().size();
                        ranked = p.isEmpty() ? new ArrayList<>() : Matching.forRepo(repo).rank(p);
                        progress.setVisibility(View.GONE);
                        render();
                        loadTracker(uid);
                    }
                    @Override public void onError(Exception e) {
                        if (!isAdded()) return;
                        progress.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Couldn't load your skills: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onError(Exception e) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Couldn't load job data: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** The Saved / Applied tags on the cards. A nicety: if it fails the cards simply show no tag. */
    private void loadTracker(String uid) {
        TrackerStore.load(uid, new TrackerStore.LoadCallback() {
            @Override public void onLoaded(JobTracker t) {
                if (isAdded()) adapter.setTracker(t);
            }
            @Override public void onError(Exception e) {}
        });
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        int n = profile.getSkills().size();
        if (n == 0) {
            tvSubtitle.setText("Add your skills to see matches");
            showEmpty("No skills yet",
                    "Add the skills you know and we'll rank " + totalJobs + " internships by how well you fit.",
                    true);
            filterScroll.setVisibility(View.GONE);
            statsRow.setVisibility(View.GONE);
            tvCount.setVisibility(View.GONE);
            adapter.submit(new ArrayList<>());
            return;
        }

        String role = profile.getTargetRole();
        tvSubtitle.setText("Ranked for your " + n + (n == 1 ? " skill" : " skills")
                + (role != null ? " · target: " + role : ""));
        filterScroll.setVisibility(View.VISIBLE);
        tvCount.setVisibility(View.VISIBLE);
        renderStats();

        List<MatchResult> shown = new ArrayList<>();
        for (MatchResult r : ranked) {
            if (verdictFilter == VerdictFilter.APPLY && r.verdict != MatchResult.Verdict.APPLY) continue;
            if (verdictFilter == VerdictFilter.WAIT && r.verdict != MatchResult.Verdict.WAIT) continue;
            if (verdictFilter == VerdictFilter.SKIP && r.verdict != MatchResult.Verdict.SKIP) continue;
            if (remoteOnly && !r.job.remote) continue;
            if (paidOnly && !r.job.paysAtLeast(PAID_AT_LEAST)) continue;
            shown.add(r);
        }
        tvCount.setText("Showing " + shown.size() + " of " + ranked.size() + " listings");
        adapter.submit(shown);
        recycler.scrollToPosition(0);

        if (shown.isEmpty()) {
            showEmpty("No listings match these filters", "Try removing a filter.", false);
        } else {
            emptyBox.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(String title, String body, boolean withButton) {
        recycler.setVisibility(View.GONE);
        emptyBox.setVisibility(View.VISIBLE);
        tvEmptyTitle.setText(title);
        tvEmptyBody.setText(body);
        btnAddSkills.setVisibility(withButton ? View.VISIBLE : View.GONE);
    }

    private void renderFilterChips() {
        filterChips.removeAllViews();
        addChip("All", verdictFilter == VerdictFilter.ALL && !remoteOnly && !paidOnly, () -> {
            verdictFilter = VerdictFilter.ALL;
            remoteOnly = false;
            paidOnly = false;
        });
        addChip("Remote", remoteOnly, () -> remoteOnly = !remoteOnly);
        addChip("₹10k+", paidOnly, () -> paidOnly = !paidOnly);
    }

    /** Three tiles: how many listings are Apply / Wait / Skip. Tap one to filter to it; tap again to clear. */
    private void renderStats() {
        int apply = 0, wait = 0, skip = 0;
        for (MatchResult r : ranked) {
            if (r.verdict == MatchResult.Verdict.APPLY) apply++;
            else if (r.verdict == MatchResult.Verdict.WAIT) wait++;
            else if (r.verdict == MatchResult.Verdict.SKIP) skip++;
        }
        statsRow.removeAllViews();
        statsRow.setVisibility(View.VISIBLE);
        addTile(apply, MatchResult.Verdict.APPLY, VerdictFilter.APPLY);
        addTile(wait, MatchResult.Verdict.WAIT, VerdictFilter.WAIT);
        addTile(skip, MatchResult.Verdict.SKIP, VerdictFilter.SKIP);
    }

    private void addTile(int count, MatchResult.Verdict verdict, VerdictFilter filter) {
        boolean selected = verdictFilter == filter;
        int accent = JobMatchAdapter.ringColor(verdict);

        LinearLayout tile = new LinearLayout(requireContext());
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(12), dp(8), dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(selected ? accent : (accent & 0x00FFFFFF) | 0x26000000); // solid when selected, 15% tint otherwise
        tile.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        tile.setLayoutParams(lp);

        TextView number = new TextView(requireContext());
        number.setText(String.valueOf(count));
        number.setTextSize(22);
        number.setTypeface(null, android.graphics.Typeface.BOLD);
        number.setTextColor(selected ? android.graphics.Color.WHITE : accent);
        TextView label = new TextView(requireContext());
        label.setText(verdict.label());
        label.setTextSize(12);
        label.setTextColor(selected ? android.graphics.Color.WHITE
                : ContextCompat.getColor(requireContext(), R.color.subject_secondary_text));
        tile.addView(number);
        tile.addView(label);

        tile.setOnClickListener(x -> {
            verdictFilter = selected ? VerdictFilter.ALL : filter;
            renderFilterChips();
            render();
        });
        statsRow.addView(tile);
    }

    private void addChip(String label, boolean selected, Runnable onTap) {
        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextSize(13);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(selected ? R.drawable.bg_category_pill_selected : R.drawable.bg_category_pill);
        tv.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.papers_category_text_active : R.color.papers_category_text_inactive));
        int h = dp(14), vv = dp(8);
        tv.setPadding(h, vv, h, vv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        tv.setLayoutParams(lp);
        tv.setOnClickListener(x -> {
            onTap.run();
            renderFilterChips();
            render();
        });
        filterChips.addView(tv);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}