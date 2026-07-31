package com.example.studentlifeos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditNoteActivity extends AppCompatActivity {

    private String unitId, existingNoteId, existingFileId;
    private Uri selectedFileUri;
    private String selectedFileName;

    private EditText etMarkdown;
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
        existingNoteId = getIntent().getStringExtra("noteId"); // null if this is a new note
        existingFileId = getIntent().getStringExtra("fileId"); // null if no attachment yet
        String existingMarkdown = getIntent().getStringExtra("markdownContent");

        ((TextView) findViewById(R.id.tvEditNoteTitle))
                .setText((existingNoteId != null ? "Edit Note · " : "Add Note · ")
                        + (unitTitle != null ? unitTitle : ""));

        etMarkdown = findViewById(R.id.etMarkdownContent);
        if (existingMarkdown != null) etMarkdown.setText(existingMarkdown);

        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        uploadProgress = findViewById(R.id.uploadProgress);
        saveProgress = findViewById(R.id.saveProgress);
        btnSave = findViewById(R.id.btnSaveNote);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAttachFile).setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnSave.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String markdown = etMarkdown.getText().toString().trim();
        if (markdown.isEmpty()) {
            Toast.makeText(this, "Write something before saving", Toast.LENGTH_SHORT).show();
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
                            saveNoteDocument(uid, markdown, newFileId));
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
            // No new file selected — keep whatever fileId (or null) already existed
            saveNoteDocument(uid, markdown, existingFileId);
        }
    }

    private interface FileIdCallback {
        void onFileId(String fileId);
    }

    private void saveUploadedFileRecord(String uid, String fileName, String fileUrl, FileIdCallback callback) {
        String fileType = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";

        Map<String, Object> fileDoc = new HashMap<>();
        fileDoc.put("unitId", unitId);
        fileDoc.put("studentId", uid);
        fileDoc.put("fileName", fileName);
        fileDoc.put("fileType", fileType);
        fileDoc.put("fileUrl", fileUrl);

        FirebaseFirestore.getInstance().collection("uploaded_files")
                .add(fileDoc)
                .addOnSuccessListener(ref -> callback.onFileId(ref.getId()))
                .addOnFailureListener(e -> {
                    resetSaveState();
                    Toast.makeText(this, "Couldn't save attachment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveNoteDocument(String uid, String markdown, String fileId) {
        Map<String, Object> note = new HashMap<>();
        note.put("unitId", unitId);
        note.put("studentId", uid);
        note.put("markdownContent", markdown);
        if (fileId != null) note.put("fileId", fileId);

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