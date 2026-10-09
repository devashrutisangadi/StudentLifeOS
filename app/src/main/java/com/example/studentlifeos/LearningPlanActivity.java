package com.example.studentlifeos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.noties.markwon.Markwon;

/**
 * Shows a Groq-written learning plan for a set of skills. Plans are cached on the phone per
 * (listing, skills) so reopening one doesn't spend another API request; "Generate a new plan" forces a fresh one.
 */
public class LearningPlanActivity extends AppCompatActivity {

    public static final String EXTRA_GOAL = "goal";        // e.g. "the Backend Intern role at Acme"
    public static final String EXTRA_SKILLS = "skills";    // String[] of skills to learn
    public static final String EXTRA_CACHE_KEY = "cacheKey";

    private static final String PREFS = "learning_plans";

    private String goal, cacheKey;
    private List<String> skills;
    private SkillsProfile profile = new SkillsProfile();
    private Markwon markwon;

    private View loading, scroll;
    private TextView tvStatus, tvPlan;
    private ProgressBar progress;
    private View btnRegenerate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_plan);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        goal = getIntent().getStringExtra(EXTRA_GOAL);
        String[] arr = getIntent().getStringArrayExtra(EXTRA_SKILLS);
        String key = getIntent().getStringExtra(EXTRA_CACHE_KEY);
        if (goal == null || arr == null || arr.length == 0) {
            finish();
            return;
        }
        skills = new ArrayList<>(Arrays.asList(arr));
        cacheKey = "plan_" + (key == null ? "x" : key) + "_" + String.join("|", skills);

        markwon = Markwon.create(this);
        loading = findViewById(R.id.planLoading);
        scroll = findViewById(R.id.planScroll);
        tvStatus = findViewById(R.id.tvPlanStatus);
        tvPlan = findViewById(R.id.tvPlan);
        progress = findViewById(R.id.planProgress);
        btnRegenerate = findViewById(R.id.btnRegenerate);

        ((TextView) findViewById(R.id.tvPlanGoal)).setText("To get ready for " + goal
                + ": " + String.join(", ", skills));
        btnRegenerate.setOnClickListener(v -> start(true));

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            start(false);
            return;
        }
        // the plan is written around what the student already knows, so load their skills first
        SkillsProfileStore.load(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                new SkillsProfileStore.LoadCallback() {
                    @Override public void onLoaded(SkillsProfile p) { profile = p; start(false); }
                    @Override public void onError(Exception e) { start(false); } // plan without that context
                });
    }

    private void start(boolean forceNew) {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String cached = forceNew ? null : prefs.getString(cacheKey, null);
        if (cached != null) {
            show(cached);
            return;
        }

        loading.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        tvStatus.setText("Writing your plan…");
        scroll.setVisibility(View.GONE);
        btnRegenerate.setVisibility(View.GONE);

        String prompt = LearningPlanPrompt.build(goal, profile.getSkills(), skills);
        LearningPlanClient.generate(this, prompt, new LearningPlanClient.Callback() {
            @Override public void onSuccess(LearningPlan plan) {
                String md = plan.toMarkdown();
                prefs.edit().putString(cacheKey, md).apply();
                show(md);
            }
            @Override public void onError(String message) {
                progress.setVisibility(View.GONE);
                tvStatus.setText("Couldn't write the plan.\n" + message);
                btnRegenerate.setVisibility(View.VISIBLE);
                ((TextView) btnRegenerate).setText("Try again");
            }
        });
    }

    private void show(String markdown) {
        loading.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);
        btnRegenerate.setVisibility(View.VISIBLE);
        ((TextView) btnRegenerate).setText("Generate a new plan");
        markwon.setMarkdown(tvPlan, markdown);
    }
}