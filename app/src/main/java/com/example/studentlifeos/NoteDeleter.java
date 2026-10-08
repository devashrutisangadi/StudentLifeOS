package com.example.studentlifeos;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.FirebaseFirestore;

/** Shared "confirm, then delete this note" flow used by the notes list and the note viewer. */
public final class NoteDeleter {

    public interface Callback {
        void onDeleted();
    }

    private NoteDeleter() {}

    public static void confirmAndDelete(Context context, NoteItem note, Callback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Delete note?")
                .setMessage("\"" + note.displayTitle() + "\" will be permanently removed"
                        + (note.hasFile() ? ", including its attached file link." : "."))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> delete(context, note, callback))
                .show();
    }

    private static void delete(Context context, NoteItem note, Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("notes").document(note.id).delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show();

                    // Best-effort cleanup of the attachment record. Done separately so a
                    // permission problem on an old/imported record can't block the note delete.
                    // (The file itself stays on Cloudinary, which can't be deleted from the app.)
                    if (note.fileId != null) {
                        db.collection("uploaded_files").document(note.fileId).delete();
                    }
                    if (callback != null) callback.onDeleted();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Couldn't delete note: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}