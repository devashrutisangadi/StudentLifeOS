package com.example.studentlifeos;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches the unit's existing note, sends it to Claude to draft flashcards, then shows
 * an editable review list (checkbox to include/exclude, inline edit) before writing the
 * chosen cards to Firestore.
 */
public class GenerateFlashcardsActivity extends AppCompatActivity {

    private String unitId, unitTitle;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private RecyclerView recyclerReview;
    private Button btnSaveSelected, btnRetry;
    private FlashcardReviewAdapter reviewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_flashcards);

        unitId = getIntent().getStringExtra("unitId");
        unitTitle = getIntent().getStringExtra("unitTitle");

        ((TextView) findViewById(R.id.tvGenerateTitle))
                .setText("Generate flashcards · " + (unitTitle != null ? unitTitle : ""));

        progressBar = findViewById(R.id.progressGenerate);
        tvStatus = findViewById(R.id.tvGenerateStatus);
        recyclerReview = findViewById(R.id.recyclerReview);
        btnSaveSelected = findViewById(R.id.btnSaveSelectedCards);
        btnRetry = findViewById(R.id.btnRetryGenerate);

        recyclerReview.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnRetry.setOnClickListener(v -> startGeneration());
        btnSaveSelected.setOnClickListener(v -> saveSelectedCards());

        startGeneration();
    }

    private void startGeneration() {
        setLoadingState();

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (unitId == null || uid == null) {
            showError("You need to be signed in to generate flashcards");
            return;
        }

        FirebaseFirestore.getInstance().collection("notes")
                .whereEqualTo("unitId", unitId)
                .whereEqualTo("studentId", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(this::onNoteFetched)
                .addOnFailureListener(e -> showError("Couldn't load notes: " + e.getMessage()));
    }

    private void onNoteFetched(QuerySnapshot snapshot) {
        if (snapshot.isEmpty()) {
            showError("Add a note for this unit first — generation reads from your notes");
            return;
        }

        String markdown = snapshot.getDocuments().get(0).getString("markdownContent");
        if (markdown == null || markdown.trim().isEmpty()) {
            showError("This unit's note is empty — write something first");
            return;
        }

        GroqApiClient.generateFlashcards(this, unitTitle, markdown, new GroqApiClient.GenerateCallback() {
            @Override
            public void onSuccess(List<FlashcardItem> drafts) {
                showReview(drafts);
            }

            @Override
            public void onError(String message) {
                showError("Generation failed: " + message);
            }
        });
    }

    private void showReview(List<FlashcardItem> drafts) {
        progressBar.setVisibility(View.GONE);
        tvStatus.setText(drafts.size() + " draft cards — edit or uncheck any before saving");
        tvStatus.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.VISIBLE);
        recyclerReview.setVisibility(View.VISIBLE);
        btnSaveSelected.setVisibility(View.VISIBLE);

        reviewAdapter = new FlashcardReviewAdapter(drafts);
        recyclerReview.setAdapter(reviewAdapter);
    }

    private void saveSelectedCards() {
        if (reviewAdapter == null) return;
        List<FlashcardItem> selected = reviewAdapter.getIncludedDrafts();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one card to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        btnSaveSelected.setEnabled(false);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        int[] remaining = {selected.size()};
        int[] failures = {0};

        for (FlashcardItem card : selected) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("unitId", unitId);
            doc.put("studentId", uid);
            doc.put("front", card.front.trim());
            doc.put("back", card.back.trim());
            doc.put("source", "generated");

            db.collection("flashcards").add(doc)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) failures[0]++;
                        remaining[0]--;
                        if (remaining[0] == 0) onSaveBatchDone(selected.size(), failures[0]);
                    });
        }
    }

    private void onSaveBatchDone(int total, int failures) {
        if (failures == 0) {
            Toast.makeText(this, "Saved " + total + " flashcards", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            btnSaveSelected.setEnabled(true);
            Toast.makeText(this, failures + " of " + total + " cards failed to save — try again",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Reading your notes and drafting cards…");
        tvStatus.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
        recyclerReview.setVisibility(View.GONE);
        btnSaveSelected.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.VISIBLE);
        recyclerReview.setVisibility(View.GONE);
        btnSaveSelected.setVisibility(View.GONE);
    }
}
