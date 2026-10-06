package com.example.studentlifeos;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thin client for Groq's OpenAI-compatible chat completions API. Used for three things in
 * this app: turning a unit's notes into draft flashcards, turning an uploaded timetable
 * PDF's extracted text into structured weekly class entries, and — for PDFs with no text
 * layer (scanned/photographed timetables) — reading a rendered page image directly with a
 * vision model instead. Uses plain HttpURLConnection + org.json so no new Gradle dependency
 * is required.
 *
 * Groq's free tier (no credit card) is the most generous of the no-cost options as of 2026:
 * roughly 30 requests/minute and 1,000 requests/day. Get a key at
 * https://console.groq.com/keys.
 *
 * Groq retires models on a schedule (see console.groq.com/docs/deprecations) — that's why
 * both model ids are read from BuildConfig instead of hardcoded, so they can be swapped in
 * local.properties without touching code. The DEFAULT_* constants are only fallbacks for
 * when those BuildConfig fields aren't set.
 *
 * NOTE on reasoning models: both the default text model (openai/gpt-oss-120b) and the
 * default vision model (qwen/qwen3.8-27b) are *reasoning* models — their hidden
 * chain-of-thought draws from the same max_tokens budget as the final answer. Without an
 * explicit, generous max_tokens, a long input can burn the whole default budget on reasoning
 * and leave nothing for the actual JSON, which Groq then rejects with a 400
 * "json_validate_failed" and an empty failed_generation. We set both a larger max_tokens and
 * reasoning_effort="low" to avoid that — structured extraction doesn't need deep reasoning.
 *
 * IMPORTANT — API key handling:
 * GROQ_API_KEY is read from BuildConfig, populated from local.properties at build time.
 * Never hardcode the key here. Note that any key baked into an APK via BuildConfig can be
 * extracted by a motivated user — fine for personal/testing use, but for anything wider,
 * proxy this call through a small backend (e.g. a Firebase Cloud Function) that holds the
 * real key server-side.
 */
public class GroqApiClient {

    // Fallbacks used only if the matching BuildConfig field is blank/unset.
    // Last confirmed working (Oct 2026):
    //  - text:   openai/gpt-oss-120b (replacement for llama-3.3-70b-versatile, retired Aug 2026)
    //  - vision: qwen/qwen3.8-27b (Groq's only active vision model — the Llama 4 vision
    //            models were retired in 2026 with no direct vision replacement until this)
    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";
    private static final String DEFAULT_VISION_MODEL = "qwen/qwen3.8-27b";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final int MAX_TOKENS = 4096; // generous enough to cover reasoning + a full week's JSON

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ---------------------------------------------------------------------
    // Flashcards
    // ---------------------------------------------------------------------

    public interface GenerateFlashcardsCallback {
        void onSuccess(List<FlashcardItem> drafts);
        void onError(String message);
    }

    public static void generateFlashcards(android.app.Activity activity, String unitTitle,
                                          String noteContent, GenerateFlashcardsCallback callback) {
        executor.execute(() -> {
            try {
                String prompt = "You are helping a student turn their class notes into flashcards.\n"
                        + "Unit: " + (unitTitle != null ? unitTitle : "Untitled") + "\n\n"
                        + "Notes:\n" + noteContent + "\n\n"
                        + "Generate 6 to 12 flashcards covering the key facts, definitions, and concepts "
                        + "in these notes. Respond with JSON in exactly this shape: "
                        + "{\"cards\": [{\"front\": \"...\", \"back\": \"...\"}]}. "
                        + "Keep each side concise — a sentence or two at most. No extra commentary.";

                JSONObject responseObj = callGroqForJson(resolveModel(), prompt);
                List<FlashcardItem> drafts = parseFlashcards(responseObj);
                activity.runOnUiThread(() -> callback.onSuccess(drafts));
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    private static List<FlashcardItem> parseFlashcards(JSONObject responseObj) throws Exception {
        JSONArray cardsJson = responseObj.getJSONArray("cards");
        List<FlashcardItem> drafts = new ArrayList<>();
        for (int i = 0; i < cardsJson.length(); i++) {
            JSONObject cardJson = cardsJson.getJSONObject(i);
            String front = cardJson.optString("front", "").trim();
            String back = cardJson.optString("back", "").trim();
            if (!front.isEmpty() && !back.isEmpty()) {
                drafts.add(new FlashcardItem(null, null, front, back, "generated"));
            }
        }
        if (drafts.isEmpty()) {
            throw new RuntimeException("Model didn't return any usable cards — try again");
        }
        return drafts;
    }

    // ---------------------------------------------------------------------
    // Timetable — from extracted PDF text
    // ---------------------------------------------------------------------

    public interface GenerateTimetableCallback {
        void onSuccess(List<TimetableEntry> drafts);
        void onError(String message);
    }

    private static final String TIMETABLE_SCHEMA_INSTRUCTIONS =
            "Identify every class slot and respond with JSON in exactly this shape: "
                    + "{\"entries\": [{\"day\": \"Monday\", \"startTime\": \"09:00\", \"endTime\": \"10:00\", "
                    + "\"subject\": \"...\", \"professor\": \"...\", \"room\": \"...\"}]}. "
                    + "\"day\" must be a full weekday name (Monday through Sunday). "
                    + "\"startTime\" and \"endTime\" must be 24-hour \"HH:mm\" (e.g. \"14:00\", not \"2 PM\"). "
                    + "If professor or room isn't mentioned for a slot, use an empty string for that field. "
                    + "Only include actual class/lab slots, not headers or free/break periods. No extra commentary.";

    public static void generateTimetable(android.app.Activity activity, String pdfText,
                                         GenerateTimetableCallback callback) {
        executor.execute(() -> {
            try {
                String prompt = "You are extracting a weekly class timetable from a student's uploaded PDF.\n\n"
                        + "Raw text extracted from the PDF:\n" + pdfText + "\n\n"
                        + TIMETABLE_SCHEMA_INSTRUCTIONS;

                JSONObject responseObj = callGroqForJson(resolveModel(), prompt);
                List<TimetableEntry> drafts = parseTimetable(responseObj);
                activity.runOnUiThread(() -> callback.onSuccess(drafts));
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    // ---------------------------------------------------------------------
    // Timetable — from page images (for PDFs with no text layer, e.g. scans/photos)
    // ---------------------------------------------------------------------

    public static void generateTimetableFromImages(android.app.Activity activity, List<String> base64JpegImages,
                                                   GenerateTimetableCallback callback) {
        executor.execute(() -> {
            try {
                JSONArray content = new JSONArray();
                content.put(new JSONObject().put("type", "text").put("text",
                        "You are extracting a weekly class timetable from this image of a student's "
                                + "timetable document. Read the table carefully, including which row/column "
                                + "each class falls under. " + TIMETABLE_SCHEMA_INSTRUCTIONS));
                for (String base64 : base64JpegImages) {
                    JSONObject imageUrl = new JSONObject().put("url", "data:image/jpeg;base64," + base64);
                    content.put(new JSONObject().put("type", "image_url").put("image_url", imageUrl));
                }

                JSONObject responseObj = callGroqForJson(resolveVisionModel(), content);
                List<TimetableEntry> drafts = parseTimetable(responseObj);
                activity.runOnUiThread(() -> callback.onSuccess(drafts));
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    private static List<TimetableEntry> parseTimetable(JSONObject responseObj) throws Exception {
        JSONArray entriesJson = responseObj.getJSONArray("entries");
        List<TimetableEntry> drafts = new ArrayList<>();
        for (int i = 0; i < entriesJson.length(); i++) {
            JSONObject e = entriesJson.getJSONObject(i);
            String dayRaw = e.optString("day", "").trim();
            int dayIndex = TimetableEntry.parseDayIndex(dayRaw);
            String startTime = e.optString("startTime", "").trim();
            String endTime = e.optString("endTime", "").trim();
            String subject = e.optString("subject", "").trim();

            if (dayIndex == 0 || startTime.isEmpty() || endTime.isEmpty() || subject.isEmpty()) {
                continue; // skip anything the model couldn't fill in properly — reviewable manually anyway
            }

            drafts.add(new TimetableEntry(
                    dayIndex,
                    TimetableEntry.dayNameFor(dayIndex),
                    startTime,
                    endTime,
                    subject,
                    e.optString("professor", "").trim(),
                    e.optString("room", "").trim()
            ));
        }
        if (drafts.isEmpty()) {
            throw new RuntimeException("Couldn't find any class slots — try a clearer file or add entries manually");
        }
        return drafts;
    }

    // ---------------------------------------------------------------------
    // Shared HTTP/JSON plumbing
    // ---------------------------------------------------------------------

    private static String resolveModel() {
        return resolveBuildConfigModel("GROQ_MODEL", DEFAULT_MODEL);
    }

    private static String resolveVisionModel() {
        return resolveBuildConfigModel("GROQ_VISION_MODEL", DEFAULT_VISION_MODEL);
    }

    private static String resolveBuildConfigModel(String fieldName, String fallback) {
        try {
            String configured = "GROQ_MODEL".equals(fieldName) ? BuildConfig.GROQ_MODEL : BuildConfig.GROQ_VISION_MODEL;
            return (configured != null && !configured.trim().isEmpty()) ? configured.trim() : fallback;
        } catch (NoSuchFieldError e) {
            // Field not added to build.gradle.kts yet — fall back quietly.
            return fallback;
        }
    }

    private static boolean isReasoningModel(String model) {
        return model != null && (model.contains("gpt-oss") || model.contains("qwen"));
    }

    /**
     * Sends a request to Groq in JSON-object response mode and returns the parsed JSON object
     * the model replied with. `content` is either a String (plain text prompt) or a JSONArray
     * (multimodal content blocks, for vision requests). The prompt/content must itself specify
     * the exact JSON shape wanted, and must contain the word "json" somewhere (OpenAI-compatible
     * json_object mode requirement).
     */
    private static JSONObject callGroqForJson(String model, Object content) throws Exception {
        String apiKey = BuildConfig.GROQ_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing GROQ_API_KEY — add it to local.properties");
        }

        JSONObject message = new JSONObject().put("role", "user").put("content", content);
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", new JSONArray().put(message));
        body.put("response_format", new JSONObject().put("type", "json_object"));
        body.put("temperature", 0.3);
        body.put("max_tokens", MAX_TOKENS);
        if (isReasoningModel(model)) {
            // Keep the budget for the actual JSON answer, not hidden chain-of-thought —
            // this is a straightforward extraction task, not one that needs deep reasoning.
            body.put("reasoning_effort", "low");
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(60000); // images take a bit longer

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String responseText = readStream(stream);

        if (status == 429) {
            throw new RuntimeException("Free tier rate limit hit — wait a minute and try again");
        }
        if (status == 404 && responseText.contains("model_not_found")) {
            throw new RuntimeException("Model \"" + model + "\" isn't available on Groq anymore — "
                    + "update GROQ_MODEL / GROQ_VISION_MODEL in local.properties. "
                    + "Check console.groq.com/docs/deprecations");
        }
        if (status == 400 && responseText.contains("json_validate_failed")) {
            throw new RuntimeException("The model ran out of room to finish its answer (common on longer "
                    + "documents) — try again, or try a shorter/cleaner file");
        }
        if (status < 200 || status >= 300) {
            throw new RuntimeException("API error (" + status + "): " + responseText);
        }

        JSONObject responseJson = new JSONObject(responseText);
        String rawText = responseJson
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();

        return new JSONObject(rawText);
    }

    private static String readStream(InputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}