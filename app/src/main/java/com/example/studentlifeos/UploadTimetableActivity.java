package com.example.studentlifeos;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets the student pick a timetable PDF straight from their device, extracts its text
 * on-device, sends it to Groq to structure into weekly class entries, then shows an
 * editable review list before saving. Saving REPLACES any existing timetable entirely
 * (deletes old entries first) rather than appending, since this represents "my current
 * weekly schedule" as a whole, not an incremental addition.
 */
public class UploadTimetableActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvStatus;
    private RecyclerView recyclerReview;
    private Button btnSaveSelected, btnPickFile;
    private TimetableReviewAdapter reviewAdapter;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) startExtraction(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_timetable);

        progressBar = findViewById(R.id.progressUploadTimetable);
        tvStatus = findViewById(R.id.tvUploadTimetableStatus);
        recyclerReview = findViewById(R.id.recyclerTimetableReview);
        btnSaveSelected = findViewById(R.id.btnSaveTimetable);
        btnPickFile = findViewById(R.id.btnPickTimetableFile);

        recyclerReview.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnPickFile.setOnClickListener(v -> filePickerLauncher.launch("application/pdf"));
        btnSaveSelected.setOnClickListener(v -> confirmAndSave());

        showPickState();
    }

    private void startExtraction(Uri fileUri) {
        setLoadingState("Reading your PDF…");
        PdfTextExtractor.extractFromUri(this, fileUri, new PdfTextExtractor.ExtractCallback() {
            @Override
            public void onSuccess(String text) {
                generateFromText(text);
            }

            @Override
            public void onError(String message) {
                showError("Couldn't read that PDF: " + message);
            }
        });
    }

    private void generateFromText(String pdfText) {
        setLoadingState("Extracting your class schedule…");
        GroqApiClient.generateTimetable(this, pdfText, new GroqApiClient.GenerateTimetableCallback() {
            @Override
            public void onSuccess(List<TimetableEntry> drafts) {
                showReview(drafts);
            }

            @Override
            public void onError(String message) {
                showError("Extraction failed: " + message);
            }
        });
    }

    private void showReview(List<TimetableEntry> drafts) {
        progressBar.setVisibility(View.GONE);
        btnPickFile.setVisibility(View.GONE);
        tvStatus.setText(drafts.size() + " classes found — review, edit, or uncheck any before saving. "
                + "Saving will replace your current timetable.");
        tvStatus.setVisibility(View.VISIBLE);
        recyclerReview.setVisibility(View.VISIBLE);
        btnSaveSelected.setVisibility(View.VISIBLE);

        reviewAdapter = new TimetableReviewAdapter(drafts);
        recyclerReview.setAdapter(reviewAdapter);
    }

    private void confirmAndSave() {
        if (reviewAdapter == null) return;
        List<TimetableEntry> selected = reviewAdapter.getIncludedDrafts();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one class to save", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Replace your timetable?")
                .setMessage("This will delete your current timetable and replace it with these "
                        + selected.size() + " classes.")
                .setPositiveButton("Replace", (dialog, which) -> replaceTimetable(selected))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void replaceTimetable(List<TimetableEntry> selected) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        btnSaveSelected.setEnabled(false);
        setLoadingState("Saving your timetable…");
        recyclerReview.setVisibility(View.GONE);
        btnSaveSelected.setVisibility(View.GONE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("timetable_entries")
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(existing -> deleteThenInsert(db, existing, uid, selected))
                .addOnFailureListener(e -> showError("Couldn't clear old timetable: " + e.getMessage()));
    }

    private void deleteThenInsert(FirebaseFirestore db, QuerySnapshot existing, String uid,
                                   List<TimetableEntry> selected) {
        int existingCount = existing.size();
        int[] deletesRemaining = {existingCount};

        Runnable insertAll = () -> {
            int[] remaining = {selected.size()};
            int[] failures = {0};
            for (TimetableEntry entry : selected) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("studentId", uid);
                doc.put("dayIndex", entry.dayIndex);
                doc.put("dayName", entry.dayName);
                doc.put("startTime", entry.startTime);
                doc.put("endTime", entry.endTime);
                doc.put("subjectName", entry.subjectName);
                doc.put("professorName", entry.professorName != null ? entry.professorName : "");
                doc.put("room", entry.room != null ? entry.room : "");

                db.collection("timetable_entries").add(doc)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) failures[0]++;
                            remaining[0]--;
                            if (remaining[0] == 0) onSaveDone(selected.size(), failures[0]);
                        });
            }
        };

        if (existingCount == 0) {
            insertAll.run();
            return;
        }
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : existing) {
            db.collection("timetable_entries").document(doc.getId()).delete()
                    .addOnCompleteListener(task -> {
                        deletesRemaining[0]--;
                        if (deletesRemaining[0] == 0) insertAll.run();
                    });
        }
    }

    private void onSaveDone(int total, int failures) {
        if (failures == 0) {
            Toast.makeText(this, "Timetable saved — " + total + " classes", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            showError(failures + " of " + total + " classes failed to save — try again");
        }
    }

    private void showPickState() {
        progressBar.setVisibility(View.GONE);
        tvStatus.setText("Pick a timetable PDF to extract your weekly schedule automatically.");
        tvStatus.setVisibility(View.VISIBLE);
        btnPickFile.setVisibility(View.VISIBLE);
        recyclerReview.setVisibility(View.GONE);
        btnSaveSelected.setVisibility(View.GONE);
    }

    private void setLoadingState(String message) {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
        btnPickFile.setVisibility(View.GONE);
        recyclerReview.setVisibility(View.GONE);
        btnSaveSelected.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
        btnPickFile.setVisibility(View.VISIBLE);
        btnPickFile.setText("Try another file");
        recyclerReview.setVisibility(View.GONE);
        btnSaveSelected.setVisibility(View.GONE);
    }
}
