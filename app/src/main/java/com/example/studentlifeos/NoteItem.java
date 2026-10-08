package com.example.studentlifeos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

/** One entry in a unit's notes list: text, a PDF/file attachment, or both. */
public class NoteItem {

    public String id, title, markdown, fileId, fileName, fileType, fileUrl;
    public Date createdAt;

    public static NoteItem fromDoc(DocumentSnapshot doc) {
        NoteItem n = new NoteItem();
        n.id = doc.getId();
        n.title = doc.getString("title");
        n.markdown = doc.getString("markdownContent");
        n.fileId = doc.getString("fileId");
        n.fileName = doc.getString("fileName");
        n.fileType = doc.getString("fileType");
        n.fileUrl = doc.getString("fileUrl");
        Timestamp ts = doc.getTimestamp("createdAt");
        n.createdAt = ts != null ? ts.toDate() : null;
        return n;
    }

    public boolean hasText() {
        return markdown != null && !markdown.trim().isEmpty();
    }

    public boolean hasFile() {
        return fileId != null || fileUrl != null || fileName != null;
    }

    /** Title shown in the list: explicit title, else first line of the text, else the file name. */
    public String displayTitle() {
        if (title != null && !title.trim().isEmpty()) return title.trim();
        if (hasText()) {
            for (String line : markdown.split("\n")) {
                String t = line.replaceAll("^[#>\\-*\\s]+", "").replaceAll("[*_`]", "").trim();
                if (!t.isEmpty()) return t.length() > 60 ? t.substring(0, 60) + "…" : t;
            }
        }
        if (fileName != null && !fileName.isEmpty()) return fileName;
        return "Untitled note";
    }

    /** Short plain-text preview of the body. */
    public String preview() {
        if (!hasText()) return "";
        String flat = markdown.replaceAll("[#>*_`]", "").replaceAll("\\s+", " ").trim();
        return flat.length() > 110 ? flat.substring(0, 110) + "…" : flat;
    }

    /** Older notes only stored a fileId; fill in name/type/url from uploaded_files when missing. */
    public void loadFileInfoIfNeeded(Runnable onDone) {
        if (fileId == null || fileUrl != null || fileName != null) {
            if (onDone != null) onDone.run();
            return;
        }
        FirebaseFirestore.getInstance().collection("uploaded_files").document(fileId).get()
                .addOnSuccessListener(d -> {
                    if (d.exists()) {
                        fileName = d.getString("fileName");
                        fileType = d.getString("fileType");
                        fileUrl = d.getString("fileUrl");
                    }
                    if (onDone != null) onDone.run();
                })
                .addOnFailureListener(e -> {
                    if (onDone != null) onDone.run();
                });
    }
}