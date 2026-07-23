package com.example.studentlifeos;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        String unitId = getIntent().getStringExtra("unitId");
        String unitTitle = getIntent().getStringExtra("unitTitle");

        ((TextView) findViewById(R.id.tvUnitTitleHeader)).setText(unitTitle != null ? unitTitle : "Notes");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        markwon = Markwon.create(this);

        loadNote(unitId);
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
            showEmptyState();
            return;
        }

        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        String markdown = doc.getString("markdownContent");
        String fileId = doc.getString("fileId");

        TextView tvContent = findViewById(R.id.tvNoteContent);
        findViewById(R.id.tvEmptyState).setVisibility(View.GONE);
        tvContent.setVisibility(View.VISIBLE);

        markwon.setMarkdown(tvContent, markdown != null ? markdown : "_No content available._");

        if (fileId != null) {
            loadAttachment(fileId);
        }
    }

    private void loadAttachment(String fileId) {
        FirebaseFirestore.getInstance().collection("uploaded_files").document(fileId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String fileName = doc.getString("fileName");
                    String fileType = doc.getString("fileType");
                    TextView tvAttachment = findViewById(R.id.tvAttachment);
                    tvAttachment.setVisibility(View.VISIBLE);
                    tvAttachment.setText("📎 " + (fileName != null ? fileName : "attachment")
                            + (fileType != null ? " (" + fileType + ")" : ""));
                    tvAttachment.setOnClickListener(v ->
                            Toast.makeText(this,
                                    "File uploads aren't available yet — this is placeholder metadata only",
                                    Toast.LENGTH_LONG).show());
                });
    }

    private void showEmptyState() {
        findViewById(R.id.tvNoteContent).setVisibility(View.GONE);
        findViewById(R.id.tvEmptyState).setVisibility(View.VISIBLE);
    }
}