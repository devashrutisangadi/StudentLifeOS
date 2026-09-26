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
 * Thin client for Groq's OpenAI-compatible chat completions API. Used for two things in
 * this app: turning a unit's notes into draft flashcards, and turning an uploaded timetable
 * PDF into structured weekly class entries. Uses plain HttpURLConnection + org.json so no
 * new Gradle dependency is required.
 *
 * Groq's free tier (no credit card) is the most generous of the no-cost options as of 2026:
 * roughly 30 requests/minute and 1,000 requests/day. Get a key at
 * https://console.groq.com/keys.
 *
 * Groq retires models on a schedule (see console.groq.com/docs/deprecations) — that's why
 * the model id is read from BuildConfig.GROQ_MODEL instead of hardcoded, so it can be
 * swapped in local.properties without touching code. DEFAULT_MODEL is only a fallback for
 * when GROQ_MODEL isn't set.
 *
 * IMPORTANT — API key handling:
 * GROQ_API_KEY is read from BuildConfig, populated from local.properties at build time.
 * Never hardcode the key here. Note that any key baked into an APK via BuildConfig can be
 * extracted by a motivated user — fine for personal/testing use, but for anything wider,
 * proxy this call through a small backend (e.g. a Firebase Cloud Function) that holds the
 * real key server-side.
 */
public class GroqApiClient {

    // Fallback used only if GROQ_MODEL is blank/unset in local.properties.
    // Last confirmed working: openai/gpt-oss-120b (Sept 2026), Groq's recommended
    // replacement after llama-3.3-70b-versatile was decommissioned Aug 16, 2026.
    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

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

                JSONObject responseObj = callGroqForJson(prompt);
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

                activity.runOnUiThread(() -> callback.onSuccess(drafts));
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    // ---------------------------------------------------------------------
    // Timetable
    // ---------------------------------------------------------------------

    public interface GenerateTimetableCallback {
        void onSuccess(List<TimetableEntry> drafts);
        void onError(String message);
    }

    public static void generateTimetable(android.app.Activity activity, String pdfText,
                                          GenerateTimetableCallback callback) {
        executor.execute(() -> {
            try {
                String prompt = "You are extracting a weekly class timetable from a student's uploaded PDF.\n\n"
                        + "Raw text extracted from the PDF:\n" + pdfText + "\n\n"
                        + "Identify every class slot and respond with JSON in exactly this shape: "
                        + "{\"entries\": [{\"day\": \"Monday\", \"startTime\": \"09:00\", \"endTime\": \"10:00\", "
                        + "\"subject\": \"...\", \"professor\": \"...\", \"room\": \"...\"}]}. "
                        + "\"day\" must be a full weekday name (Monday through Sunday). "
                        + "\"startTime\" and \"endTime\" must be 24-hour \"HH:mm\" (e.g. \"14:00\", not \"2 PM\"). "
                        + "If professor or room isn't mentioned for a slot, use an empty string for that field. "
                        + "Only include actual class/lab slots, not headers or free periods. No extra commentary.";

                JSONObject responseObj = callGroqForJson(prompt);
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

                    TimetableEntry entry = new TimetableEntry(
                            dayIndex,
                            TimetableEntry.dayNameFor(dayIndex),
                            startTime,
                            endTime,
                            subject,
                            e.optString("professor", "").trim(),
                            e.optString("room", "").trim()
                    );
                    drafts.add(entry);
                }
                if (drafts.isEmpty()) {
                    throw new RuntimeException("Couldn't find any class slots in this PDF — try a clearer export or add entries manually");
                }

                activity.runOnUiThread(() -> callback.onSuccess(drafts));
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    // ---------------------------------------------------------------------
    // Shared HTTP/JSON plumbing
    // ---------------------------------------------------------------------

    private static String resolveModel() {
        try {
            String configured = BuildConfig.GROQ_MODEL;
            return (configured != null && !configured.trim().isEmpty()) ? configured.trim() : DEFAULT_MODEL;
        } catch (NoSuchFieldError e) {
            // GROQ_MODEL not added to build.gradle.kts yet — fall back quietly.
            return DEFAULT_MODEL;
        }
    }

    /**
     * Sends a prompt to Groq in JSON-object response mode and returns the parsed JSON object
     * the model replied with. The prompt must itself specify the exact JSON shape wanted, and
     * must contain the word "json" somewhere (OpenAI-compatible json_object mode requirement).
     */
    private static JSONObject callGroqForJson(String prompt) throws Exception {
        String apiKey = BuildConfig.GROQ_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing GROQ_API_KEY — add it to local.properties");
        }
        String model = resolveModel();

        JSONObject message = new JSONObject().put("role", "user").put("content", prompt);
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", new JSONArray().put(message));
        body.put("response_format", new JSONObject().put("type", "json_object"));
        body.put("temperature", 0.3);

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(45000);

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
                    + "update GROQ_MODEL in local.properties. Check console.groq.com/docs/deprecations");
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
