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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asks Groq for a {@link LearningPlan}. Same API, key and model settings as GroqApiClient
 * (BuildConfig.GROQ_API_KEY / GROQ_MODEL); kept separate so GroqApiClient doesn't need editing.
 */
public final class LearningPlanClient {

    public interface Callback {
        void onSuccess(LearningPlan plan);
        void onError(String message);
    }

    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private LearningPlanClient() {}

    public static void generate(android.app.Activity activity, String prompt, Callback callback) {
        executor.execute(() -> {
            try {
                LearningPlan plan = parse(call(prompt));
                activity.runOnUiThread(() -> callback.onSuccess(plan));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                activity.runOnUiThread(() -> callback.onError(msg));
            }
        });
    }

    static LearningPlan parse(JSONObject root) throws Exception {
        LearningPlan plan = new LearningPlan();
        plan.overview = root.optString("overview", "").trim();
        plan.project = root.optString("project", "").trim();
        JSONArray steps = root.optJSONArray("steps");
        if (steps != null) {
            for (int i = 0; i < steps.length(); i++) {
                JSONObject o = steps.getJSONObject(i);
                LearningPlan.Step s = new LearningPlan.Step();
                s.skill = o.optString("skill", "").trim();
                if (s.skill.isEmpty()) continue;
                s.why = o.optString("why", "").trim();
                s.days = o.optInt("days", 0);
                s.resource = o.optString("resource", "").trim();
                JSONArray actions = o.optJSONArray("actions");
                if (actions != null) {
                    for (int a = 0; a < actions.length(); a++) s.actions.add(actions.optString(a, ""));
                }
                plan.steps.add(s);
            }
        }
        if (plan.steps.isEmpty()) {
            throw new RuntimeException("The model didn't return a usable plan — try again");
        }
        return plan;
    }

    private static JSONObject call(String prompt) throws Exception {
        String apiKey = BuildConfig.GROQ_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing GROQ_API_KEY — add it to local.properties");
        }
        String model = BuildConfig.GROQ_MODEL;
        if (model == null || model.trim().isEmpty()) model = DEFAULT_MODEL;
        model = model.trim();

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", prompt)));
        body.put("response_format", new JSONObject().put("type", "json_object"));
        body.put("temperature", 0.4);
        body.put("max_tokens", 4096);
        if (model.contains("gpt-oss") || model.contains("qwen")) body.put("reasoning_effort", "low");

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(60000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String text = read(stream);
        if (status == 429) throw new RuntimeException("Free tier rate limit hit — wait a minute and try again");
        if (status == 404 && text.contains("model_not_found")) {
            throw new RuntimeException("Model \"" + model + "\" isn't available on Groq anymore — update GROQ_MODEL in local.properties");
        }
        if (status == 400 && text.contains("json_validate_failed")) {
            throw new RuntimeException("The model ran out of room to finish its answer — try again");
        }
        if (status < 200 || status >= 300) throw new RuntimeException("API error (" + status + "): " + text);

        String content = new JSONObject(text).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim();
        return new JSONObject(content);
    }

    private static String read(InputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}