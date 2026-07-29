package com.example.studentlifeos;

import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

public class CloudinaryUploader {

    private static final String UPLOAD_PRESET = "studentlifeos_unsigned"; // from Step 2

    public interface UploadResultListener {
        void onSuccess(String secureUrl, String fileName);
        void onError(String message);
        void onProgress(int percent);
    }

    /** Uploads a file picked via the Storage Access Framework (a content:// Uri). */
    public static void upload(Uri fileUri, String displayFileName, UploadResultListener listener) {
        MediaManager.get().upload(fileUri)
                .unsigned(UPLOAD_PRESET)
                .option("resource_type", "auto") // handles pdf, images, docs, etc.
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int percent = (int) (100.0 * bytes / totalBytes);
                        listener.onProgress(percent);
                    }

                    @Override
                    public void onSuccess(String requestId, java.util.Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        listener.onSuccess(secureUrl, displayFileName);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        listener.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        listener.onError("Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }
}