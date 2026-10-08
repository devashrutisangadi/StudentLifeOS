package com.example.studentlifeos;

import android.content.Intent;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.List;

/** Lists ALL notes (text and/or PDFs) for one syllabus unit. Tap to open, long-press to delete. */
public class NotesActivity extends AppCompatActivity {

    private String unitId, unitTitle;
    private final List<NoteItem> notes = new ArrayList<>();
    private NoteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        unitId = getIntent().getStringExtra("unitId");
        unitTitle = getIntent().getStringExtra("unitTitle");

        ((TextView) findViewById(R.id.tvUnitTitleHeader)).setText(unitTitle != null ? unitTitle : "Notes");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // "+" always starts a NEW note (no noteId passed)
        findViewById(R.id.btnAddNote).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditNoteActivity.class);
            intent.putExtra("unitId", unitId);
            intent.putExtra("unitTitle", unitTitle);
            startActivity(intent);
        });

        findViewById(R.id.btnOpenFlashcards).setOnClickListener(v -> {
            Intent intent = new Intent(this, FlashcardsActivity.class);
            intent.putExtra("unitId", unitId);
            intent.putExtra("unitTitle", unitTitle);
            startActivity(intent);
        });

        RecyclerView rv = findViewById(R.id.rvNotes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(notes, new NoteAdapter.Listener() {
            @Override public void onClick(NoteItem note) { openNote(note); }
            @Override public void onLongClick(NoteItem note) { confirmDelete(note); }
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(); // refresh after adding/editing/deleting
    }

    private void loadNotes() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (unitId == null || uid == null) {
            showList();
            return;
        }

        FirebaseFirestore.getInstance().collection("notes")
                .whereEqualTo("unitId", unitId)
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    notes.clear();
                    snapshot.getDocuments().forEach(d -> notes.add(NoteItem.fromDoc(d)));

                    // newest first; notes without a createdAt (older data) go last
                    notes.sort((a, b) -> {
                        if (a.createdAt == null && b.createdAt == null) return 0;
                        if (a.createdAt == null) return 1;
                        if (b.createdAt == null) return -1;
                        return b.createdAt.compareTo(a.createdAt);
                    });

                    showList();
                    for (NoteItem n : notes) {
                        n.loadFileInfoIfNeeded(() -> adapter.notifyDataSetChanged());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load notes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showList();
                });
    }

    private void showList() {
        adapter.notifyDataSetChanged();
        findViewById(R.id.tvEmptyState).setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.rvNotes).setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** Text notes open in the viewer; a PDF/file-only entry opens the file straight away. */
    private void openNote(NoteItem note) {
        if (!note.hasText() && note.hasFile()) {
            openAttachment(note);
            return;
        }
        Intent intent = new Intent(this, NoteViewActivity.class);
        intent.putExtra("noteId", note.id);
        intent.putExtra("unitId", unitId);
        intent.putExtra("unitTitle", unitTitle);
        startActivity(intent);
    }

    private void openAttachment(NoteItem note) {
        if (note.fileUrl == null) {
            // Dataset-imported placeholder metadata: no real file behind it
            Toast.makeText(this, "This is sample data from the imported dataset — no real file attached",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(note.fileUrl)));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(NoteItem note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete note?")
                .setMessage("\"" + note.displayTitle() + "\" will be removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) ->
                        FirebaseFirestore.getInstance().collection("notes").document(note.id).delete()
                                .addOnSuccessListener(unused -> {
                                    notes.remove(note);
                                    showList();
                                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Couldn't delete: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .show();
    }
}