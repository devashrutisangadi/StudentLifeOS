package com.example.studentlifeos;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "My skills": add skills (autocomplete from the skills the job listings use), set a level for each,
 * pick a target role, and tap suggestions drawn from the student's own subjects.
 * Saved to students/{uid}.skills.
 */
public class SkillsProfileActivity extends AppCompatActivity {

    private final SkillsProfile profile = new SkillsProfile();
    private final List<String> suggestions = new ArrayList<>();
    private final List<String> roles = new ArrayList<>();

    private JobRepository repo;
    private String uid;
    private boolean dirty = false;

    private AutoCompleteTextView etSkill;
    private LinearLayout roleChips, skillsContainer, suggestionChips;
    private View suggestionsSection;
    private TextView tvSkillsTitle, tvEmpty;
    private Button btnSave;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skills_profile);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etSkill = findViewById(R.id.etSkill);
        roleChips = findViewById(R.id.roleChips);
        skillsContainer = findViewById(R.id.skillsContainer);
        suggestionChips = findViewById(R.id.suggestionChips);
        suggestionsSection = findViewById(R.id.suggestionsSection);
        tvSkillsTitle = findViewById(R.id.tvSkillsTitle);
        tvEmpty = findViewById(R.id.tvEmptySkills);
        btnSave = findViewById(R.id.btnSaveSkills);
        progress = findViewById(R.id.skillsProgress);

        btnSave.setEnabled(false);
        progress.setVisibility(View.VISIBLE);

        findViewById(R.id.btnBack).setOnClickListener(v -> confirmExit());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmExit(); }
        });

        findViewById(R.id.btnAddSkill).setOnClickListener(v -> addFromInput());
        etSkill.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { addFromInput(); return true; }
            return false;
        });
        btnSave.setOnClickListener(v -> save());

        JobRepository.load(this, new JobRepository.Callback() {
            @Override public void onLoaded(JobRepository r) {
                repo = r;
                setUpAutocomplete();
                setUpRoles();
                loadProfile();
            }
            @Override public void onError(Exception e) {
                Toast.makeText(SkillsProfileActivity.this,
                        "Couldn't load job data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    // ------------------------------------------------------------------ setup

    private void setUpAutocomplete() {
        List<String> names = new ArrayList<>();
        for (JobRepository.SkillInfo s : repo.getSkillCatalog()) names.add(s.name);
        etSkill.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
        etSkill.setThreshold(1);
    }

    private void setUpRoles() {
        Map<String, Integer> counts = new HashMap<>();
        for (Job j : repo.getJobs()) counts.merge(j.role, 1, Integer::sum);
        roles.clear();
        roles.addAll(counts.keySet());
        roles.sort((a, b) -> {
            // "Business & Other" always last; otherwise most listings first
            if (a.equals("Business & Other")) return 1;
            if (b.equals("Business & Other")) return -1;
            return Integer.compare(counts.get(b), counts.get(a));
        });
    }

    private void loadProfile() {
        SkillsProfileStore.load(uid, new SkillsProfileStore.LoadCallback() {
            @Override public void onLoaded(SkillsProfile p) {
                profile.replaceWith(p);
                progress.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                render();
                loadSuggestions();
            }
            @Override public void onError(Exception e) {
                progress.setVisibility(View.GONE);
                btnSave.setEnabled(true); // allow starting fresh, but warn
                Toast.makeText(SkillsProfileActivity.this,
                        "Couldn't load your saved skills: " + e.getMessage(), Toast.LENGTH_LONG).show();
                render();
            }
        });
    }

    /** Subject names + unit titles -> skills the student probably already has. */
    private void loadSuggestions() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> subjectsTask = db.collection("subjects").whereEqualTo("studentId", uid).get();
        Task<QuerySnapshot> unitsTask = db.collection("units").whereEqualTo("studentId", uid).get();
        Tasks.whenAllSuccess(subjectsTask, unitsTask).addOnSuccessListener(results -> {
            List<String> texts = new ArrayList<>();
            for (Object r : results) {
                for (com.google.firebase.firestore.DocumentSnapshot d : ((QuerySnapshot) r).getDocuments()) {
                    String t = d.getString("name");
                    if (t == null) t = d.getString("title");
                    if (t != null) texts.add(t);
                }
            }
            List<String> technical = new ArrayList<>();
            for (JobRepository.SkillInfo s : repo.getSkillCatalog()) {
                if ("technical".equals(s.kind)) technical.add(s.name);
            }
            suggestions.clear();
            suggestions.addAll(SkillSuggester.suggest(texts, technical, profile.keys(), 10));
            renderSuggestions();
        }); // suggestions are a nicety: if this fails the section just stays hidden
    }

    // ------------------------------------------------------------------ actions

    private void addFromInput() {
        String text = etSkill.getText().toString().trim();
        if (text.isEmpty()) return;

        List<String> unknown = new ArrayList<>();
        boolean addedAny = false;
        for (String name : repo.getNormalizer().normalize(text)) {
            if (profile.add(name, StudentSkill.BEGINNER)) {
                addedAny = true;
                if (!repo.getNormalizer().isKnown(name)) unknown.add(name);
            }
        }
        etSkill.setText("");
        if (!addedAny) {
            Toast.makeText(this, "Already in your list", Toast.LENGTH_SHORT).show();
            return;
        }
        dirty = true;
        suggestions.removeIf(profile::has);
        render();
        if (!unknown.isEmpty()) {
            Toast.makeText(this, "No listing asks for \"" + unknown.get(0)
                    + "\" yet, so it won't affect your matches.", Toast.LENGTH_LONG).show();
        }
    }

    private void save() {
        btnSave.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        SkillsProfileStore.save(uid, profile, new SkillsProfileStore.SaveCallback() {
            @Override public void onSaved() {
                dirty = false;
                Toast.makeText(SkillsProfileActivity.this, "Skills saved", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override public void onError(Exception e) {
                progress.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(SkillsProfileActivity.this,
                        "Couldn't save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmExit() {
        if (!dirty) { finish(); return; }
        new AlertDialog.Builder(this)
                .setTitle("Discard changes?")
                .setMessage("Your skill changes haven't been saved.")
                .setPositiveButton("Discard", (d, w) -> finish())
                .setNegativeButton("Keep editing", null)
                .show();
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        renderRoles();
        renderSkills();
        renderSuggestions();
    }

    private void renderRoles() {
        roleChips.removeAllViews();
        for (String role : roles) {
            boolean selected = role.equals(profile.getTargetRole());
            TextView chip = chip(role, selected);
            chip.setOnClickListener(v -> {
                profile.setTargetRole(selected ? null : role); // tap again to clear
                dirty = true;
                renderRoles();
            });
            roleChips.addView(chip);
        }
    }

    private void renderSkills() {
        skillsContainer.removeAllViews();
        int n = profile.getSkills().size();
        tvSkillsTitle.setText(n == 0 ? "Your skills" : "Your skills (" + n + ")");
        tvEmpty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (StudentSkill skill : profile.getSkills()) {
            View row = inflater.inflate(R.layout.item_student_skill, skillsContainer, false);
            ((TextView) row.findViewById(R.id.tvSkillName)).setText(skill.name);
            TextView level = row.findViewById(R.id.tvSkillLevel);
            level.setText(skill.levelDots());
            level.setOnClickListener(v -> {
                skill.cycleLevel();
                dirty = true;
                level.setText(skill.levelDots());
            });
            row.findViewById(R.id.ivRemoveSkill).setOnClickListener(v -> {
                profile.remove(skill.name);
                dirty = true;
                render();
            });
            skillsContainer.addView(row);
        }
    }

    private void renderSuggestions() {
        suggestionChips.removeAllViews();
        suggestions.removeIf(profile::has);
        suggestionsSection.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
        for (String name : new ArrayList<>(suggestions)) {
            TextView chip = chip("+ " + name, false);
            chip.setOnClickListener(v -> {
                profile.add(name, StudentSkill.BEGINNER);
                dirty = true;
                render();
            });
            suggestionChips.addView(chip);
        }
    }

    private TextView chip(String text, boolean selected) {
        TextView tv = new TextView(this);
        tv.setText(text);
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
        return tv;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}