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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddPaperActivity extends AppCompatActivity {

    private String subjectId;
    private String selectedExamType = "Mid-Semester";
    private Uri selectedFileUri;
    private String selectedFileName;

    private Button btnMidSem, btnEndSem, btnSavePaper;
    private ProgressBar uploadProgress, saveProgress;
    private TextView tvSelectedFile;

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
        setContentView(R.layout.activity_add_paper);

        subjectId = getIntent().getStringExtra("subjectId");
        String subjectName = getIntent().getStringExtra("subjectName");

        ((TextView) findViewById(R.id.tvAddPaperTitle))
                .setText("Add Paper · " + (subjectName != null ? subjectName : ""));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnMidSem = findViewById(R.id.btnMidSem);
        btnEndSem = findViewById(R.id.btnEndSem);
        selectExamType("Mid-Semester");

        btnMidSem.setOnClickListener(v -> selectExamType("Mid-Semester"));
        btnEndSem.setOnClickListener(v -> selectExamType("End-Semester"));

        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        uploadProgress = findViewById(R.id.uploadProgress);
        saveProgress = findViewById(R.id.saveProgress);
        btnSavePaper = findViewById(R.id.btnSavePaper);

        findViewById(R.id.btnAttachFile).setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnSavePaper.setOnClickListener(v -> savePaper());
    }

    private void selectExamType(String examType) {
        selectedExamType = examType;
        btnMidSem.setBackgroundResource(examType.equals("Mid-Semester") ? R.drawable.bg_button_peach : R.drawable.bg_button_outline);
        btnEndSem.setBackgroundResource(examType.equals("End-Semester") ? R.drawable.bg_button_peach : R.drawable.bg_button_outline);
    }

    private void savePaper() {
        EditText etTitle = findViewById(R.id.etPaperTitle);
        EditText etYear = findViewById(R.id.etYear);

        String title = etTitle.getText().toString().trim();
        String yearText = etYear.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }
        if (yearText.isEmpty()) {
            etYear.setError("Year is required");
            return;
        }
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please attach a file", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        int year = Integer.parseInt(yearText);

        btnSavePaper.setEnabled(false);
        saveProgress.setVisibility(View.VISIBLE);
        uploadProgress.setVisibility(View.VISIBLE);

        CloudinaryUploader.upload(selectedFileUri, selectedFileName, new CloudinaryUploader.UploadResultListener() {
            @Override
            public void onSuccess(String secureUrl, String fileName) {
                String fileType = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";

                Map<String, Object> paper = new HashMap<>();
                paper.put("studentId", uid);
                paper.put("subjectId", subjectId);
                paper.put("title", title);
                paper.put("examType", selectedExamType);
                paper.put("year", year);
                paper.put("fileName", fileName);
                paper.put("fileType", fileType);
                paper.put("fileUrl", secureUrl);

                FirebaseFirestore.getInstance().collection("papers")
                        .add(paper)
                        .addOnSuccessListener(ref -> {
                            Toast.makeText(AddPaperActivity.this, "Paper saved", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            resetSaveState();
                            Toast.makeText(AddPaperActivity.this, "Couldn't save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    resetSaveState();
                    Toast.makeText(AddPaperActivity.this, "Upload failed: " + message, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> uploadProgress.setProgress(percent));
            }
        });
    }

    private void resetSaveState() {
        btnSavePaper.setEnabled(true);
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