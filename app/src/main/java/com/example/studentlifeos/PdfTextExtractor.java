package com.example.studentlifeos;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads or reads a PDF and pulls its plain text, entirely on-device — no external API
 * involved. Two entry points:
 *  - extractFromUrl: for a PDF already uploaded somewhere (e.g. a note's Cloudinary URL)
 *  - extractFromUri: for a PDF the student just picked from their device (e.g. a timetable
 *    upload, before/without ever being uploaded anywhere)
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
                String text = extractText(() -> {
                    HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(30000);
                    return conn.getInputStream();
                });
                activity.runOnUiThread(() -> callback.onSuccess(text));
            } catch (Exception e) {
                activity.runOnUiThread(() ->
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Couldn't read the PDF"));
            }
        });
    }

    public static void extractFromUri(Activity activity, Uri fileUri, ExtractCallback callback) {
        ensureInit(activity.getApplicationContext());
        executor.execute(() -> {
            try {
                String text = extractText(() -> activity.getContentResolver().openInputStream(fileUri));
                activity.runOnUiThread(() -> callback.onSuccess(text));
            } catch (Exception e) {
                activity.runOnUiThread(() ->
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Couldn't read the PDF"));
            }
        });
    }

    private interface StreamOpener {
        InputStream open() throws Exception;
    }

    private static String extractText(StreamOpener opener) throws Exception {
        try (InputStream in = opener.open();
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

    private static synchronized void ensureInit(Context appContext) {
        if (!resourceLoaderInitialized) {
            PDFBoxResourceLoader.init(appContext);
            resourceLoaderInitialized = true;
        }
    }
}
