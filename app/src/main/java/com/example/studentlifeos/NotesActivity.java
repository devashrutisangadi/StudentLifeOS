package com.example.studentlifeos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import io.noties.markwon.Markwon;

public class NotesActivity extends AppCompatActivity {

    private Markwon markwon;
    private String unitId, unitTitle;
    private String currentNoteId, currentMarkdown, currentFileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        unitId = getIntent().getStringExtra("unitId");
        unitTitle = getIntent().getStringExtra("unitTitle");

        ((TextView) findViewById(R.id.tvUnitTitleHeader)).setText(unitTitle != null ? unitTitle : "Notes");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnEditNote).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditNoteActivity.class);
            intent.putExtra("unitId", unitId);
            intent.putExtra("unitTitle", unitTitle);
            intent.putExtra("noteId", currentNoteId);
            intent.putExtra("fileId", currentFileId);
            intent.putExtra("markdownContent", currentMarkdown);
            startActivity(intent);
        });

        markwon = Markwon.create(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNote(unitId); // refresh in case we just came back from editing
    }

    private void loadNote(String unitId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (unitId == null || uid == null) {
            showEmptyState();
            return;
        }

        FirebaseFirestore.getInstance().collection("notes")
                .whereEqualTo("unitId", unitId)
                .whereEqualTo("studentId", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(this::bindNote)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load notes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void bindNote(QuerySnapshot snapshot) {
        if (snapshot.isEmpty()) {
            currentNoteId = null;
            currentMarkdown = null;
            currentFileId = null;
            showEmptyState();
            return;
        }

        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        currentNoteId = doc.getId();
        currentMarkdown = doc.getString("markdownContent");
        currentFileId = doc.getString("fileId");

        TextView tvContent = findViewById(R.id.tvNoteContent);
        findViewById(R.id.tvEmptyState).setVisibility(View.GONE);
        tvContent.setVisibility(View.VISIBLE);

        markwon.setMarkdown(tvContent, currentMarkdown != null ? currentMarkdown : "_No content available._");

        findViewById(R.id.tvAttachment).setVisibility(View.GONE);
        if (currentFileId != null) {
            loadAttachment(currentFileId);
        }
    }

    private void loadAttachment(String fileId) {
        FirebaseFirestore.getInstance().collection("uploaded_files").document(fileId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String fileName = doc.getString("fileName");
                    String fileType = doc.getString("fileType");
                    String fileUrl = doc.getString("fileUrl"); // only present on real uploads

                    TextView tvAttachment = findViewById(R.id.tvAttachment);
                    tvAttachment.setVisibility(View.VISIBLE);
                    tvAttachment.setText("📎 " + (fileName != null ? fileName : "attachment")
                            + (fileType != null ? " (" + fileType + ")" : ""));

                    if (fileUrl != null) {
                        tvAttachment.setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
                            startActivity(intent);
                        });
                    } else {
                        // Dataset-imported placeholder metadata — no real file behind it
                        tvAttachment.setOnClickListener(v ->
                                Toast.makeText(this, "This is sample data from the imported dataset — no real file attached",
                                        Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void showEmptyState() {
        findViewById(R.id.tvNoteContent).setVisibility(View.GONE);
        findViewById(R.id.tvEmptyState).setVisibility(View.VISIBLE);
    }
}