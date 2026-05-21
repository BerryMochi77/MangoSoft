package com.example.comp2100miniproject.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dao.model.Post;

/**
 * Calls the DeepSeek chat-completions API once with the entire post list and
 * parses the model's JSON verdict back into {@link CuratedPost} rows.
 *
 * <p>One batch request rather than one per post: each network round-trip
 * costs ~500 ms and a few cents, so a 10-post feed turns into one request
 * instead of ten. The model is told to return strict JSON which we parse
 * defensively.</p>
 *
 * <p>Threading: callers stay on the main thread. We do the HTTP call on a
 * single-thread executor and post the result back via the main looper.</p>
 *
 * <p>No new dependencies — pure {@link HttpURLConnection} + {@code org.json}
 * — to keep teammate gradle setup unchanged.</p>
 */
public class AiPostCurationStrategy implements PostCurationStrategy {

    private static final String TAG = "AiPostCuration";
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void curate(List<Post> posts, String viewerHint,
                       String preferences, Callback callback) {
        if (posts == null || posts.isEmpty()) {
            mainHandler.post(() -> callback.onResult(new ArrayList<>()));
            return;
        }
        if (ApiKeys.DEEPSEEK_API_KEY == null || ApiKeys.DEEPSEEK_API_KEY.isBlank()) {
            mainHandler.post(() -> callback.onError(
                    new IllegalStateException("DeepSeek API key is not configured.")));
            return;
        }

        // Snapshot the list before crossing to a background thread — the
        // caller's collection may mutate or get cleared while we wait.
        final ArrayList<Post> snapshot = new ArrayList<>(posts);
        final String safePreferences = preferences == null ? "" : preferences.trim();

        executor.execute(() -> {
            try {
                List<CuratedPost> curated = curateBlocking(snapshot, viewerHint, safePreferences);
                mainHandler.post(() -> callback.onResult(curated));
            } catch (Throwable t) {
                Log.w(TAG, "Curation request failed", t);
                mainHandler.post(() -> callback.onError(t));
            }
        });
    }

    // ---------- internals ----------

    private List<CuratedPost> curateBlocking(List<Post> posts, String viewerHint,
                                             String preferences)
            throws IOException, JSONException {
        String prompt = buildUserMessage(posts, viewerHint, preferences);
        String requestBody = buildRequestBody(prompt, preferences);

        HttpURLConnection conn = (HttpURLConnection) new URL(ApiKeys.DEEPSEEK_CHAT_COMPLETIONS_URL)
                .openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + ApiKeys.DEEPSEEK_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");

            try (OutputStream out = conn.getOutputStream()) {
                out.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String responseBody = readAll(stream);

            if (status >= 400) {
                throw new IOException("DeepSeek HTTP " + status + ": " + responseBody);
            }
            return parseResponse(responseBody, posts);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Build the user-role message we send to the model. Posts are numbered
     * 1..N — the model echoes the same {@code n} back so we can map each
     * verdict to the right {@link Post} without trusting it to send UUIDs.
     */
    private String buildUserMessage(List<Post> posts, String viewerHint,
                                    String preferences) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < posts.size(); i++) {
            Post p = posts.get(i);
            JSONObject entry = new JSONObject();
            entry.put("n", i + 1);
            entry.put("title", p.topic == null ? "" : p.topic);
            entry.put("body", p.getBody());
            entry.put("hashtags", new JSONArray(p.getHashtags()));
            entry.put("reply_count", countReplies(p));
            array.put(entry);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Viewer: ")
                .append(viewerHint == null ? "anonymous" : viewerHint)
                .append('\n');
        if (preferences != null && !preferences.isEmpty()) {
            // Wrap in clear delimiters so the model can't be confused by
            // commas/colons inside the user's preference text.
            sb.append("Viewer preferences (verbatim, treat as the dominant filter):\n")
                    .append("<<<\n")
                    .append(preferences)
                    .append("\n>>>\n");
        } else {
            sb.append("Viewer preferences: (none — use generic forum relevance heuristics)\n");
        }
        sb.append("Posts (JSON array follows):\n").append(array.toString());
        return sb.toString();
    }

    private int countReplies(Post p) {
        int count = 0;
        var it = p.getVisibleMessages(false).getAll();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }

    private String buildRequestBody(String userPrompt, String preferences) throws JSONException {
        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put("role", "system");

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append(
                "You are a PERSONAL forum-feed curator. Your single job is to " +
                "decide which posts match THIS viewer's stated preferences. " +
                "Reply with ONLY a JSON array — no markdown fences, no prose. " +
                "Each element MUST have the shape " +
                "{\"n\": <integer matching the input>, \"worth_reading\": <bool>, " +
                "\"summary\": <one short sentence explaining WHY it matches or " +
                "does not match the viewer's preferences>, " +
                "\"score\": <integer 0..10 = how well it matches the viewer>}.");
        if (preferences != null && !preferences.isEmpty()) {
            systemPrompt.append(
                "\nThe viewer's preferences (which appear in the user message) " +
                "are the ONLY criterion. Do not impose your own taste about " +
                "what is high or low quality. A post is worth_reading only if " +
                "the preferences would say so.");
        } else {
            systemPrompt.append(
                "\nThe viewer has not stated preferences. Fall back to a generic " +
                "forum-relevance heuristic: real questions, useful info, " +
                "moderation issues = high; venting / near-duplicates / spam = low.");
        }
        system.put("content", systemPrompt.toString());
        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.put(user);

        JSONObject body = new JSONObject();
        body.put("model", ApiKeys.DEEPSEEK_DEFAULT_MODEL);
        body.put("messages", messages);
        body.put("temperature", 0.4);
        // Some OpenAI-compatible APIs honour this and force valid JSON output.
        // Harmless if ignored.
        body.put("response_format", new JSONObject().put("type", "json_object"));
        return body.toString();
    }

    /**
     * Parse the chat-completions envelope, extract the assistant content,
     * then parse that content as a JSON array of verdicts and zip back
     * against the original posts.
     */
    private List<CuratedPost> parseResponse(String responseBody, List<Post> posts)
            throws JSONException {
        JSONObject root = new JSONObject(responseBody);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new JSONException("DeepSeek returned no choices: " + responseBody);
        }
        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "");

        JSONArray verdicts = extractVerdictArray(content);

        // Map by the 1-based index we sent so misordered output still lines up.
        Map<Integer, JSONObject> verdictByN = new HashMap<>();
        for (int i = 0; i < verdicts.length(); i++) {
            JSONObject v = verdicts.optJSONObject(i);
            if (v == null) continue;
            int n = v.optInt("n", -1);
            if (n > 0) verdictByN.put(n, v);
        }

        ArrayList<CuratedPost> out = new ArrayList<>(posts.size());
        for (int i = 0; i < posts.size(); i++) {
            JSONObject v = verdictByN.get(i + 1);
            if (v == null) {
                // Model skipped this one — keep it conservatively (assume
                // worth reading, low score) so we don't silently drop posts.
                out.add(new CuratedPost(posts.get(i), true,
                        "(AI did not score this post)", 5));
                continue;
            }
            out.add(new CuratedPost(
                    posts.get(i),
                    v.optBoolean("worth_reading", true),
                    v.optString("summary", "").trim(),
                    v.optInt("score", 5)
            ));
        }
        return out;
    }

    /**
     * The model is told to reply with a bare JSON array, but real responses
     * sometimes wrap it: e.g. inside an object ({@code {"results":[...]}}),
     * inside markdown code fences, or with surrounding prose. Be lenient.
     */
    private JSONArray extractVerdictArray(String raw) throws JSONException {
        if (raw == null) throw new JSONException("Empty AI content.");
        String trimmed = raw.trim();
        // Strip ```json ... ``` or ``` ... ``` fences if present.
        if (trimmed.startsWith("```")) {
            int nl = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (nl > 0 && end > nl) {
                trimmed = trimmed.substring(nl + 1, end).trim();
            }
        }

        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        if (trimmed.startsWith("{")) {
            JSONObject obj = new JSONObject(trimmed);
            // Look for the first array-valued field — common keys first.
            for (String key : new String[]{"results", "verdicts", "posts", "data", "items"}) {
                JSONArray maybe = obj.optJSONArray(key);
                if (maybe != null) return maybe;
            }
            // Otherwise pick the first array we find.
            for (var it = obj.keys(); it.hasNext(); ) {
                JSONArray maybe = obj.optJSONArray(it.next());
                if (maybe != null) return maybe;
            }
        }
        // Last resort — find the outermost [...] substring.
        int open = trimmed.indexOf('[');
        int close = trimmed.lastIndexOf(']');
        if (open >= 0 && close > open) {
            return new JSONArray(trimmed.substring(open, close + 1));
        }
        throw new JSONException("Could not find a JSON array in AI content: " + raw);
    }

    private static String readAll(InputStream input) throws IOException {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Convenience for AiFragment: stable hash of the post list so callers
     * can cheaply detect whether the feed changed between successive
     * "Curate" taps without re-comparing each Post.
     */
    public static String feedFingerprint(List<Post> posts) {
        StringBuilder sb = new StringBuilder();
        for (Post p : posts) {
            sb.append(p.id.toString()).append('|');
            sb.append(p.topic == null ? "" : p.topic).append('|');
            sb.append(p.getBody()).append('\n');
        }
        // Hash by content, not by identity, so an unchanged DAO snapshot
        // can be detected even if the Post object instances differ.
        return UUID.nameUUIDFromBytes(sb.toString().getBytes(StandardCharsets.UTF_8))
                .toString();
    }
}
