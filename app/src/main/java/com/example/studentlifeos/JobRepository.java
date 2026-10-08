package com.example.studentlifeos;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads the bundled internship data (assets/jobs.json) once and keeps it in memory.
 *
 * Usage:
 *   JobRepository.load(context, new JobRepository.Callback() {
 *       public void onLoaded(JobRepository repo) { ... repo.getJobs() ... }
 *       public void onError(Exception e) { ... }
 *   });
 * Parsing runs on a background thread; callbacks arrive on the main thread.
 */
public final class JobRepository {

    public interface Callback {
        void onLoaded(JobRepository repo);
        void onError(Exception e);
    }

    /** One entry of the skill catalog: a canonical skill and how many listings mention it. */
    public static final class SkillInfo {
        public final String name;
        public final int count;
        /** "technical", "language" or "soft". */
        public final String kind;

        SkillInfo(String name, int count, String kind) {
            this.name = name;
            this.count = count;
            this.kind = kind;
        }
    }

    private static final String ASSET_NAME = "jobs.json";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static JobRepository instance;

    private final List<Job> jobs;
    private final Map<String, Job> byId = new HashMap<>();
    private final List<SkillInfo> skillCatalog;
    private final SkillNormalizer normalizer;

    private JobRepository(List<Job> jobs, List<SkillInfo> skillCatalog, SkillNormalizer normalizer) {
        this.jobs = Collections.unmodifiableList(jobs);
        this.skillCatalog = Collections.unmodifiableList(skillCatalog);
        this.normalizer = normalizer;
        for (Job j : jobs) byId.put(j.id, j);
    }

    /** The already-loaded repository, or null if load() hasn't finished yet. */
    public static synchronized JobRepository getCached() {
        return instance;
    }

    public static void load(Context context, Callback callback) {
        JobRepository cached = getCached();
        if (cached != null) {
            callback.onLoaded(cached);
            return;
        }
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                JobRepository repo = parse(readAsset(app));
                synchronized (JobRepository.class) {
                    instance = repo;
                }
                MAIN.post(() -> callback.onLoaded(repo));
            } catch (Exception e) {
                MAIN.post(() -> callback.onError(e));
            }
        });
    }

    public List<Job> getJobs() { return jobs; }

    public Job findById(String id) { return byId.get(id); }

    /** All known skills, most common first. Use for autocomplete when adding a skill. */
    public List<SkillInfo> getSkillCatalog() { return skillCatalog; }

    public SkillNormalizer getNormalizer() { return normalizer; }

    // ---------------------------------------------------------------- parsing

    private static String readAsset(Context context) throws Exception {
        try (InputStream in = context.getAssets().open(ASSET_NAME)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 20);
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static JobRepository parse(String json) throws Exception {
        JSONObject root = new JSONObject(json);

        Map<String, List<String>> aliases = new HashMap<>();
        JSONObject aliasObj = root.getJSONObject("aliases");
        for (java.util.Iterator<String> it = aliasObj.keys(); it.hasNext(); ) {
            String alias = it.next();
            aliases.put(alias, stringList(aliasObj.getJSONArray(alias)));
        }

        List<SkillInfo> catalog = new ArrayList<>();
        List<String> canonicalNames = new ArrayList<>();
        JSONArray skillArr = root.getJSONArray("skills");
        for (int i = 0; i < skillArr.length(); i++) {
            JSONObject s = skillArr.getJSONObject(i);
            catalog.add(new SkillInfo(s.getString("name"), s.getInt("count"), s.optString("kind", "technical")));
            canonicalNames.add(s.getString("name"));
        }

        List<Job> jobs = new ArrayList<>();
        JSONArray jobArr = root.getJSONArray("jobs");
        for (int i = 0; i < jobArr.length(); i++) {
            JSONObject o = jobArr.getJSONObject(i);
            Job j = new Job();
            j.id = o.getString("id");
            j.title = o.getString("title");
            j.company = o.getString("company");
            j.role = o.optString("role", "");
            j.remote = o.optBoolean("remote", false);
            j.location = o.optString("location", "");
            j.partTime = o.optBoolean("partTime", false);
            j.skills = stringList(o.getJSONArray("skills"));
            j.description = o.optString("description", "");
            j.url = o.optString("url", "");
            j.stipendText = o.optString("stipendText", "");
            j.stipendMin = o.isNull("stipendMin") ? null : o.getInt("stipendMin");
            j.stipendMax = o.isNull("stipendMax") ? null : o.getInt("stipendMax");
            j.stipendPeriod = o.isNull("stipendPeriod") ? null : o.getString("stipendPeriod");
            j.incentives = o.optBoolean("incentives", false);
            jobs.add(j);
        }

        return new JobRepository(jobs, catalog, new SkillNormalizer(aliases, canonicalNames));
    }

    private static List<String> stringList(JSONArray arr) throws Exception {
        List<String> out = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) out.add(arr.getString(i));
        return out;
    }
}