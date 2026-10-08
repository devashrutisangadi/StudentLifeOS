package com.example.studentlifeos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import io.noties.markwon.Markwon;

/** Read view for a single note: rendered markdown plus a tappable attachment (PDF/file). */
public class NoteViewActivity extends AppCompatActivity {

    private Markwon markwon;
    private String noteId, unitId, unitTitle;
    private NoteItem current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);

        noteId = getIntent().getStringExtra("noteId");
        unitId = getIntent().getStringExtra("unitId");
        unitTitle = getIntent().getStringExtra("unitTitle");
        markwon = Markwon.create(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDeleteNote).setOnClickListener(v -> {
            if (current != null) NoteDeleter.confirmAndDelete(this, current, this::finish);
        });

        findViewById(R.id.btnEditNote).setOnClickListener(v -> {
            if (current == null) return;
            Intent intent = new Intent(this, EditNoteActivity.class);
            intent.putExtra("unitId", unitId);
            intent.putExtra("unitTitle", unitTitle);
            intent.putExtra("noteId", current.id);
            intent.putExtra("noteTitle", current.title);
            intent.putExtra("fileId", current.fileId);
            intent.putExtra("fileName", current.fileName);
            intent.putExtra("markdownContent", current.markdown);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load(); // refresh after returning from editing
    }

    private void load() {
        if (noteId == null) {
            finish();
            return;
        }
        FirebaseFirestore.getInstance().collection("notes").document(noteId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        finish(); // deleted
                        return;
                    }
                    current = NoteItem.fromDoc(doc);
                    bind();
                    current.loadFileInfoIfNeeded(this::bindAttachment);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bind() {
        ((TextView) findViewById(R.id.tvNoteTitleHeader)).setText(current.displayTitle());

        TextView tvContent = findViewById(R.id.tvNoteContent);
        if (current.hasText()) {
            tvContent.setVisibility(View.VISIBLE);
            markwon.setMarkdown(tvContent, current.markdown);
        } else {
            tvContent.setVisibility(View.GONE);
        }
        bindAttachment();
    }

    private void bindAttachment() {
        TextView tvAttachment = findViewById(R.id.tvAttachment);
        if (current == null || !current.hasFile()) {
            tvAttachment.setVisibility(View.GONE);
            return;
        }
        String type = current.fileType != null && !current.fileType.isEmpty()
                ? " (" + current.fileType + ")" : "";
        tvAttachment.setVisibility(View.VISIBLE);
        tvAttachment.setText("📎 " + (current.fileName != null ? current.fileName : "attachment") + type
                + "  ·  tap to open");
        tvAttachment.setOnClickListener(v -> {
            if (current.fileUrl == null) {
                Toast.makeText(this, "This is sample data from the imported dataset — no real file attached",
                        Toast.LENGTH_LONG).show();
                return;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(current.fileUrl)));
            } catch (Exception e) {
                Toast.makeText(this, "No app available to open this file", Toast.LENGTH_SHORT).show();
            }
        });
    }
}