package com.example.studentlifeos;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AddFlashcardActivity extends AppCompatActivity {

    private String unitId, existingCardId;
    private EditText etFront, etBack;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_flashcard);

        unitId = getIntent().getStringExtra("unitId");
        String unitTitle = getIntent().getStringExtra("unitTitle");
        existingCardId = getIntent().getStringExtra("cardId"); // null if creating a new card

        ((TextView) findViewById(R.id.tvAddFlashcardTitle))
                .setText((existingCardId != null ? "Edit flashcard · " : "New flashcard · ")
                        + (unitTitle != null ? unitTitle : ""));

        etFront = findViewById(R.id.etCardFront);
        etBack = findViewById(R.id.etCardBack);
        btnSave = findViewById(R.id.btnSaveFlashcard);

        String existingFront = getIntent().getStringExtra("front");
        String existingBack = getIntent().getStringExtra("back");
        if (existingFront != null) etFront.setText(existingFront);
        if (existingBack != null) etBack.setText(existingBack);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveCard());
    }

    private void saveCard() {
        String front = etFront.getText().toString().trim();
        String back = etBack.getText().toString().trim();

        if (front.isEmpty() || back.isEmpty()) {
            Toast.makeText(this, "Fill in both the front and back", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || unitId == null) return;

        btnSave.setEnabled(false);

        Map<String, Object> card = new HashMap<>();
        card.put("unitId", unitId);
        card.put("studentId", uid);
        card.put("front", front);
        card.put("back", back);
        if (existingCardId == null) card.put("source", "manual");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (existingCardId != null) {
            db.collection("flashcards").document(existingCardId)
                    .set(card, SetOptions.merge())
                    .addOnSuccessListener(unused -> finishSuccessfully())
                    .addOnFailureListener(this::onSaveFailed);
        } else {
            db.collection("flashcards").add(card)
                    .addOnSuccessListener(ref -> finishSuccessfully())
                    .addOnFailureListener(this::onSaveFailed);
        }
    }

    private void onSaveFailed(Exception e) {
        btnSave.setEnabled(true);
        Toast.makeText(this, "Couldn't save flashcard: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void finishSuccessfully() {
        Toast.makeText(this, "Flashcard saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
