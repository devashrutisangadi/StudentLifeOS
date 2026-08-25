package com.example.studentlifeos;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

/**
 * A plain flip-through study session: no spaced-repetition scheduling, just
 * "tap to reveal" and "next / I knew it" through the deck in order.
 */
public class FlashcardStudyActivity extends AppCompatActivity {

    private ArrayList<String> fronts, backs;
    private int currentIndex = 0;
    private int knownCount = 0;
    private boolean showingBack = false;

    private TextView tvCardText, tvProgressCounter, tvHint;
    private ProgressBar progressBar;
    private View btnKnew, btnReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_study);

        String unitTitle = getIntent().getStringExtra("unitTitle");
        fronts = getIntent().getStringArrayListExtra("fronts");
        backs = getIntent().getStringArrayListExtra("backs");

        ((TextView) findViewById(R.id.tvStudyTitle)).setText(unitTitle != null ? unitTitle : "Study");

        tvCardText = findViewById(R.id.tvCardText);
        tvProgressCounter = findViewById(R.id.tvProgressCounter);
        tvHint = findViewById(R.id.tvFlipHint);
        progressBar = findViewById(R.id.progressBarStudy);
        btnKnew = findViewById(R.id.btnKnewIt);
        btnReview = findViewById(R.id.btnStudyAgain);

        findViewById(R.id.btnCloseStudy).setOnClickListener(v -> finish());
        findViewById(R.id.cardContainer).setOnClickListener(v -> flipCard());
        btnKnew.setOnClickListener(v -> {
            knownCount++;
            advance();
        });
        btnReview.setOnClickListener(v -> advance());

        if (fronts == null || fronts.isEmpty()) {
            Toast.makeText(this, "No cards to study", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showCard();
    }

    private void showCard() {
        showingBack = false;
        tvCardText.setText(fronts.get(currentIndex));
        tvHint.setText("Tap the card to reveal the answer");
        setAnswerButtonsVisible(false);
        updateProgress();
    }

    private void flipCard() {
        showingBack = !showingBack;
        tvCardText.setText(showingBack ? backs.get(currentIndex) : fronts.get(currentIndex));
        tvHint.setText(showingBack ? "" : "Tap the card to reveal the answer");
        setAnswerButtonsVisible(showingBack);
    }

    private void setAnswerButtonsVisible(boolean visible) {
        btnKnew.setVisibility(visible ? View.VISIBLE : View.GONE);
        btnReview.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void advance() {
        // "Knew it" vs "Study again" only affects the end-of-session tally below —
        // there's no scheduling, every card is shown once per session either way.
        currentIndex++;
        if (currentIndex >= fronts.size()) {
            showSummary();
        } else {
            showCard();
        }
    }

    private void updateProgress() {
        tvProgressCounter.setText("Card " + (currentIndex + 1) + " of " + fronts.size());
        progressBar.setMax(fronts.size());
        progressBar.setProgress(currentIndex);
    }

    private void showSummary() {
        Toast.makeText(this, "Done! You knew " + knownCount + " of " + fronts.size() + " cards.",
                Toast.LENGTH_LONG).show();
        finish();
    }
}
