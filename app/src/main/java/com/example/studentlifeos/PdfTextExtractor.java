package com.example.studentlifeos;

import android.app.Activity;
import android.content.Context;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads a PDF from a (Cloudinary) URL and pulls its plain text, entirely on-device —
 * no external API involved. Used by GenerateFlashcardsActivity so flashcard generation can
 * read the actual content of an attached PDF note, not just whatever short text the student
 * typed alongside it.
 *
 * Only works on text-based PDFs (typed/exported documents). A scanned PDF with no embedded
 * text layer will come back empty — that would need OCR, which is a separate, heavier
 * feature (e.g. ML Kit Text Recognition) if you ever need it.
 */
public class PdfTextExtractor {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int MAX_CHARS = 15000; // keeps the Groq prompt (and free-tier usage) reasonable
    private static volatile boolean resourceLoaderInitialized = false;

    public interface ExtractCallback {
        void onSuccess(String text);
        void onError(String message);
    }

    public static void extractFromUrl(Activity activity, String fileUrl, ExtractCallback callback) {
        ensureInit(activity.getApplicationContext());
        executor.execute(() -> {
            try {
                String text = doExtract(fileUrl);
                activity.runOnUiThread(() -> callback.onSuccess(text));
            } catch (Exception e) {
                activity.runOnUiThread(() ->
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Couldn't read the PDF"));
            }
        });
    }

    private static synchronized void ensureInit(Context appContext) {
        if (!resourceLoaderInitialized) {
            PDFBoxResourceLoader.init(appContext);
            resourceLoaderInitialized = true;
        }
    }

    private static String doExtract(String fileUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);

        try (InputStream in = conn.getInputStream();
             PDDocument document = PDDocument.load(in)) {

            String text = new PDFTextStripper().getText(document);
            if (text == null) text = "";
            text = text.trim();

            if (text.isEmpty()) {
                throw new RuntimeException("No readable text found in this PDF — "
                        + "it may be a scanned image rather than a text document");
            }
            if (text.length() > MAX_CHARS) {
                text = text.substring(0, MAX_CHARS);
            }
            return text;
        }
    }
}