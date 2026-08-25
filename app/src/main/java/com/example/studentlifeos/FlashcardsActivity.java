package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists flashcards for a single syllabus unit. Reached from NotesActivity (same
 * unitId/unitTitle extras pattern used across the app).
 */
public class FlashcardsActivity extends AppCompatActivity {

    private String unitId, unitTitle;
    private FlashcardAdapter adapter;
    private TextView tvEmptyState, tvCardCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        unitId = getIntent().getStringExtra("unitId");
        unitTitle = getIntent().getStringExtra("unitTitle");

        ((TextView) findViewById(R.id.tvFlashcardsTitle))
                .setText(unitTitle != null ? unitTitle : "Flashcards");
        tvEmptyState = findViewById(R.id.tvFlashcardsEmptyState);
        tvCardCount = findViewById(R.id.tvCardCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnAddFlashcard).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddFlashcardActivity.class);
            intent.putExtra("unitId", unitId);
            intent.putExtra("unitTitle", unitTitle);
            startActivity(intent);
        });

        findViewById(R.id.btnGenerateFromNotes).setOnClickListener(v -> {
            Intent intent = new Intent(this, GenerateFlashcardsActivity.class);
            intent.putExtra("unitId", unitId);
            intent.putExtra("unitTitle", unitTitle);
            startActivity(intent);
        });

        findViewById(R.id.btnStudy).setOnClickListener(v -> {
            List<FlashcardItem> cards = adapter.getCards();
            if (cards == null || cards.isEmpty()) {
                Toast.makeText(this, "Add some cards before studying", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FlashcardStudyActivity.class);
            intent.putExtra("unitTitle", unitTitle);
            ArrayList<String> fronts = new ArrayList<>();
            ArrayList<String> backs = new ArrayList<>();
            for (FlashcardItem c : cards) {
                fronts.add(c.front);
                backs.add(c.back);
            }
            intent.putStringArrayListExtra("fronts", fronts);
            intent.putStringArrayListExtra("backs", backs);
            startActivity(intent);
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerFlashcards);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FlashcardAdapter(new ArrayList<>(), this::openEditCard, this::confirmDelete);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFlashcards();
    }

    private void openEditCard(FlashcardItem card) {
        Intent intent = new Intent(this, AddFlashcardActivity.class);
        intent.putExtra("unitId", unitId);
        intent.putExtra("unitTitle", unitTitle);
        intent.putExtra("cardId", card.id);
        intent.putExtra("front", card.front);
        intent.putExtra("back", card.back);
        startActivity(intent);
    }

    private void confirmDelete(FlashcardItem card) {
        new AlertDialog.Builder(this)
                .setTitle("Delete flashcard?")
                .setMessage("This can't be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCard(card))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCard(FlashcardItem card) {
        FirebaseFirestore.getInstance().collection("flashcards").document(card.id)
                .delete()
                .addOnSuccessListener(unused -> loadFlashcards())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadFlashcards() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (unitId == null || uid == null) {
            showEmptyState(true);
            return;
        }

        FirebaseFirestore.getInstance().collection("flashcards")
                .whereEqualTo("unitId", unitId)
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(this::bindCards)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load flashcards: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    private void bindCards(QuerySnapshot snapshot) {
        List<FlashcardItem> cards = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            FlashcardItem card = new FlashcardItem();
            card.id = doc.getId();
            card.unitId = doc.getString("unitId");
            card.front = doc.getString("front");
            card.back = doc.getString("back");
            card.source = doc.getString("source");
            cards.add(card);
        }
        adapter.updateData(cards);
        tvCardCount.setText(cards.size() + (cards.size() == 1 ? " card" : " cards"));
        showEmptyState(cards.isEmpty());
    }

    private void showEmptyState(boolean empty) {
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        findViewById(R.id.recyclerFlashcards).setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
