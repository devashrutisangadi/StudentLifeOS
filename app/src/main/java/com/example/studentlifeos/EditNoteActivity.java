package com.example.studentlifeos;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a NEW note (no noteId extra) or edits an existing one. A note can be text, a
 * PDF/file, or both. Every "Add" creates a separate note, so a unit can hold many.
 */
public class EditNoteActivity extends AppCompatActivity {

    private String unitId, existingNoteId, existingFileId, existingFileName;
    private Uri selectedFileUri;
    private String selectedFileName;

    private EditText etTitle, etMarkdown;
    private TextView tvSelectedFile;
    private ProgressBar uploadProgress, saveProgress;
    private Button btnSave;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    selectedFileName = getFileName(uri);
                    tvSelectedFile.setVisibility(View.VISIBLE);
                    tvSelectedFile.setText("Selected: " + selectedFileName);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        unitId = getIntent().getStringExtra("unitId");
        String unitTitle = getIntent().getStringExtra("unitTitle");
        existingNoteId = getIntent().getStringExtra("noteId");     // null => new note
        existingFileId = getIntent().getStringExtra("fileId");     // null => no attachment yet
        existingFileName = getIntent().getStringExtra("fileName");
        String existingTitle = getIntent().getStringExtra("noteTitle");
        String existingMarkdown = getIntent().getStringExtra("markdownContent");

        ((TextView) findViewById(R.id.tvEditNoteTitle))
                .setText((existingNoteId != null ? "Edit Note · " : "Add Note · ")
                        + (unitTitle != null ? unitTitle : ""));

        etTitle = findViewById(R.id.etNoteTitle);
        etMarkdown = findViewById(R.id.etMarkdownContent);
        if (existingTitle != null) etTitle.setText(existingTitle);
        if (existingMarkdown != null) etMarkdown.setText(existingMarkdown);

        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        uploadProgress = findViewById(R.id.uploadProgress);
        saveProgress = findViewById(R.id.saveProgress);
        btnSave = findViewById(R.id.btnSaveNote);

        if (existingFileId != null) {
            tvSelectedFile.setVisibility(View.VISIBLE);
            tvSelectedFile.setText("Current attachment: "
                    + (existingFileName != null ? existingFileName : "file") + "  (pick a file to replace it)");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAttachFile).setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnSave.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String markdown = etMarkdown.getText().toString().trim();

        boolean hasFile = selectedFileUri != null || existingFileId != null;
        if (title.isEmpty() && markdown.isEmpty() && !hasFile) {
            Toast.makeText(this, "Add a title, some text, or a file before saving", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        btnSave.setEnabled(false);
        saveProgress.setVisibility(View.VISIBLE);

        if (selectedFileUri != null) {
            uploadProgress.setVisibility(View.VISIBLE);
            CloudinaryUploader.upload(selectedFileUri, selectedFileName, new CloudinaryUploader.UploadResultListener() {
                @Override
                public void onSuccess(String secureUrl, String fileName) {
                    saveUploadedFileRecord(uid, fileName, secureUrl, newFileId ->
                            saveNoteDocument(uid, title, markdown, newFileId, fileName, fileTypeOf(fileName), secureUrl));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        resetSaveState();
                        Toast.makeText(EditNoteActivity.this, "Upload failed: " + message, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onProgress(int percent) {
                    runOnUiThread(() -> uploadProgress.setProgress(percent));
                }
            });
        } else {
            // No new file: keep whatever attachment the note already had (file fields left untouched)
            saveNoteDocument(uid, title, markdown, null, null, null, null);
        }
    }

    private interface FileIdCallback {
        void onFileId(String fileId);
    }

    private static String fileTypeOf(String fileName) {
        return fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
    }

    private void saveUploadedFileRecord(String uid, String fileName, String fileUrl, FileIdCallback callback) {
        Map<String, Object> fileDoc = new HashMap<>();
        fileDoc.put("unitId", unitId);
        fileDoc.put("studentId", uid);
        fileDoc.put("fileName", fileName);
        fileDoc.put("fileType", fileTypeOf(fileName));
        fileDoc.put("fileUrl", fileUrl);

        FirebaseFirestore.getInstance().collection("uploaded_files")
                .add(fileDoc)
                .addOnSuccessListener(ref -> callback.onFileId(ref.getId()))
                .addOnFailureListener(e -> {
                    resetSaveState();
                    Toast.makeText(this, "Couldn't save attachment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveNoteDocument(String uid, String title, String markdown,
                                  String fileId, String fileName, String fileType, String fileUrl) {
        Map<String, Object> note = new HashMap<>();
        note.put("unitId", unitId);
        note.put("studentId", uid);
        note.put("title", title);
        note.put("markdownContent", markdown);
        note.put("updatedAt", FieldValue.serverTimestamp());
        if (fileId != null) {
            note.put("fileId", fileId);
            note.put("fileName", fileName);
            note.put("fileType", fileType);
            note.put("fileUrl", fileUrl);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (existingNoteId != null) {
            db.collection("notes").document(existingNoteId)
                    .set(note, SetOptions.merge())
                    .addOnSuccessListener(unused -> finishSuccessfully())
                    .addOnFailureListener(e -> {
                        resetSaveState();
                        Toast.makeText(this, "Couldn't save note: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            note.put("createdAt", FieldValue.serverTimestamp());
            db.collection("notes").add(note)
                    .addOnSuccessListener(ref -> finishSuccessfully())
                    .addOnFailureListener(e -> {
                        resetSaveState();
                        Toast.makeText(this, "Couldn't save note: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void finishSuccessfully() {
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void resetSaveState() {
        btnSave.setEnabled(true);
        saveProgress.setVisibility(View.GONE);
        uploadProgress.setVisibility(View.GONE);
    }

    private String getFileName(Uri uri) {
        String result = null;
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
            }
        }
        return result != null ? result : uri.getLastPathSegment();
    }
}