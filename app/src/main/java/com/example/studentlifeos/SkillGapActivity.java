package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** "Which skill should I learn next?": the skills that would lift the most listings for this student. */
public class SkillGapActivity extends AppCompatActivity {

    private static final int MAX_ROWS = 10;

    private LinearLayout list;
    private ProgressBar progress;
    private TextView intro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_gap);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        list = findViewById(R.id.gapList);
        progress = findViewById(R.id.gapProgress);
        intro = findViewById(R.id.tvGapIntro);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        JobRepository.load(this, new JobRepository.Callback() {
            @Override public void onLoaded(JobRepository repo) {
                SkillsProfileStore.load(uid, new SkillsProfileStore.LoadCallback() {
                    @Override public void onLoaded(SkillsProfile p) { analyse(repo, p); }
                    @Override public void onError(Exception e) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(SkillGapActivity.this, "Couldn't load your skills", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onError(Exception e) {
                progress.setVisibility(View.GONE);
                Toast.makeText(SkillGapActivity.this, "Couldn't load job data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void analyse(JobRepository repo, SkillsProfile profile) {
        if (profile.isEmpty()) {
            progress.setVisibility(View.GONE);
            intro.setText("Add your skills first, then this screen shows which new skill would help you most.");
            return;
        }
        Map<String, String> kinds = new HashMap<>();
        for (JobRepository.SkillInfo s : repo.getSkillCatalog()) kinds.put(SkillNormalizer.key(s.name), s.kind);

        new Thread(() -> {
            List<SkillGapAnalyzer.Gap> gaps = SkillGapAnalyzer.topGaps(
                    profile, Matching.forRepo(repo), repo.getJobs(), kinds, MAX_ROWS);
            runOnUiThread(() -> show(gaps, profile, repo.getJobs().size()));
        }).start();
    }

    private void show(List<SkillGapAnalyzer.Gap> gaps, SkillsProfile profile, int totalJobs) {
        progress.setVisibility(View.GONE);
        intro.setText("Learning one of these would move the most internships up a level for you "
                + "(Skip → Wait → Apply), out of " + totalJobs + " listings.");
        LayoutInflater inflater = LayoutInflater.from(this);
        for (SkillGapAnalyzer.Gap g : gaps) {
            View row = inflater.inflate(R.layout.item_skill_gap, list, false);
            ((TextView) row.findViewById(R.id.tvGapSkill)).setText(g.skill);
            String detail;
            if (g.unlocks > 0) {
                detail = "Moves " + g.unlocks + (g.unlocks == 1 ? " listing" : " listings") + " up"
                        + (g.becomeApply > 0 ? " · " + g.becomeApply + " become Apply" : "")
                        + " · asked by " + g.asking;
            } else {
                detail = "Asked by " + g.asking + (g.asking == 1 ? " listing" : " listings");
            }
            ((TextView) row.findViewById(R.id.tvGapDetail)).setText(detail);
            row.setOnClickListener(v -> openPlan(g.skill, profile));
            list.addView(row);
        }
    }

    private void openPlan(String skill, SkillsProfile profile) {
        String role = profile.getTargetRole();
        Intent i = new Intent(this, LearningPlanActivity.class);
        i.putExtra(LearningPlanActivity.EXTRA_GOAL,
                role != null ? "internships in " + role : "internships that match your profile");
        i.putExtra(LearningPlanActivity.EXTRA_SKILLS, new String[]{skill});
        i.putExtra(LearningPlanActivity.EXTRA_CACHE_KEY, "gap");
        startActivity(i);
    }
}