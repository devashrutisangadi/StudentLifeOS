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
 * Thin client for Groq's OpenAI-compatible chat completions API, used to turn a unit's
 * note content into draft flashcards. Uses plain HttpURLConnection + org.json so no new
 * Gradle dependency is required.
 *
 * Groq's free tier (no credit card) is the most generous of the no-cost options as of 2026:
 * roughly 30 requests/minute and 1,000 requests/day. Get a key at
 * https://console.groq.com/keys.
 *
 * Groq retires models on a schedule (see console.groq.com/docs/deprecations) — that's why
 * the model id below is read from BuildConfig.GROQ_MODEL instead of hardcoded, so it can be
 * swapped in local.properties without touching code. DEFAULT_MODEL is only a fallback for
 * when GROQ_MODEL isn't set.
 *
 * IMPORTANT — API key handling:
 * GROQ_API_KEY is read from BuildConfig, populated from local.properties at build time
 * (see setup notes). Never hardcode the key here. Note that any key baked into an APK via
 * BuildConfig can be extracted by a motivated user — fine for personal/testing use, but for
 * anything wider, proxy this call through a small backend (e.g. a Firebase Cloud Function)
 * that holds the real key server-side.
 */
public class GroqApiClient {

    // Fallback used only if GROQ_MODEL is blank/unset in local.properties.
    // Last confirmed working: openai/gpt-oss-120b (Sept 2026), Groq's recommended
    // replacement after llama-3.3-70b-versatile was decommissioned Aug 16, 2026.
    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface GenerateCallback {
        void onSuccess(List<FlashcardItem> drafts);
        void onError(String message);
    }

    /** Runs the network call off the main thread; delivers the callback back on it. */
    public static void generateFlashcards(android.app.Activity activity, String unitTitle,
                                          String noteMarkdown, GenerateCallback callback) {
        executor.execute(() -> {
            try {
                List<FlashcardItem> drafts = doGenerate(unitTitle, noteMarkdown);
                activity.runOnUiThread(() -> callback.onSuccess(drafts));
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    private static String resolveModel() {
        try {
            String configured = BuildConfig.GROQ_MODEL;
            return (configured != null && !configured.trim().isEmpty()) ? configured.trim() : DEFAULT_MODEL;
        } catch (NoSuchFieldError e) {
            // GROQ_MODEL not added to build.gradle.kts yet — fall back quietly.
            return DEFAULT_MODEL;
        }
    }

    private static List<FlashcardItem> doGenerate(String unitTitle, String noteMarkdown) throws Exception {
        String apiKey = BuildConfig.GROQ_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing GROQ_API_KEY — add it to local.properties");
        }
        String model = resolveModel();

        // json_object mode (OpenAI-compatible) requires a JSON *object* at the top level,
        // not a bare array, and the word "json" must appear in the prompt.
        String prompt = "You are helping a student turn their class notes into flashcards.\n"
                + "Unit: " + (unitTitle != null ? unitTitle : "Untitled") + "\n\n"
                + "Notes (markdown):\n" + noteMarkdown + "\n\n"
                + "Generate 6 to 12 flashcards covering the key facts, definitions, and concepts "
                + "in these notes. Respond with JSON in exactly this shape: "
                + "{\"cards\": [{\"front\": \"...\", \"back\": \"...\"}]}. "
                + "Keep each side concise — a sentence or two at most. No extra commentary.";

        JSONObject message = new JSONObject().put("role", "user").put("content", prompt);
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", new JSONArray().put(message));
        body.put("response_format", new JSONObject().put("type", "json_object"));
        body.put("temperature", 0.5);

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);

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

        JSONObject parsed = new JSONObject(rawText);
        JSONArray cardsJson = parsed.getJSONArray("cards");

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