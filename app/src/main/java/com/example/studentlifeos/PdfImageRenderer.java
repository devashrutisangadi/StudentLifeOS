package com.example.studentlifeos;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Renders a PDF's pages to images (base64 JPEG) entirely on-device, using Android's built-in
 * PdfRenderer. This is the fallback for PDFs with no extractable text layer (scanned/photo
 * timetables, typically) — the rendered page images get sent to a vision-capable model
 * instead, which reads the table visually rather than needing OCR text at all.
 */
public class PdfImageRenderer {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int TARGET_WIDTH_PX = 1600; // enough resolution for a model to read small table text
    private static final int JPEG_QUALITY = 85;

    public interface RenderCallback {
        void onSuccess(List<String> base64JpegImages); // no "data:" prefix — caller adds that
        void onError(String message);
    }

    public static void renderPagesAsBase64(Activity activity, Uri pdfUri, int maxPages, RenderCallback callback) {
        executor.execute(() -> {
            try {
                List<String> images = doRender(activity, pdfUri, maxPages);
                activity.runOnUiThread(() -> callback.onSuccess(images));
            } catch (Exception e) {
                activity.runOnUiThread(() ->
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Couldn't render the PDF"));
            }
        });
    }

    private static List<String> doRender(Activity activity, Uri pdfUri, int maxPages) throws Exception {
        List<String> results = new ArrayList<>();

        try (ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(pdfUri, "r")) {
            if (pfd == null) throw new RuntimeException("Couldn't open the file");

            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                int pageCount = Math.min(renderer.getPageCount(), maxPages);
                for (int i = 0; i < pageCount; i++) {
                    try (PdfRenderer.Page page = renderer.openPage(i)) {
                        float scale = TARGET_WIDTH_PX / (float) page.getWidth();
                        int width = TARGET_WIDTH_PX;
                        int height = Math.round(page.getHeight() * scale);

                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        bitmap.eraseColor(0xFFFFFFFF); // white background — PDF pages render transparent otherwise
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
                        bitmap.recycle();

                        results.add(Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP));
                    }
                }
            }
        }

        if (results.isEmpty()) {
            throw new RuntimeException("This PDF has no pages to read");
        }
        return results;
    }
}