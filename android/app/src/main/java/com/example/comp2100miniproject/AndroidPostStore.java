package com.example.comp2100miniproject;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Message;
import dao.model.Post;
import hashtag.HashtagService;
import messagestate.MessageDeletionRegistry;
import messagestate.MessageEditRegistry;
import messagestate.MessageThreadRegistry;

/**
 * Android-private persistence for the in-memory post DAO.
 *
 * The social-core DataManager writes to a desktop-style relative "saved/"
 * folder, which is not a reliable Android app data location. This store keeps
 * the app prototype simple while making posts survive logout and process restarts.
 */
public final class AndroidPostStore {
    private static final String POSTS_FILE = "posts.json";
    private static final String SEED_POSTS_FILE = "posts_seed.json";

    private AndroidPostStore() {}

    public static boolean loadAll(Context context) {
        Context appContext = context.getApplicationContext();
        File file = postsFile(appContext);

        PostDAO dao = PostDAO.getInstance();
        HashtagService hashtags = HashtagService.getInstance();
        MessageThreadRegistry threads = MessageThreadRegistry.getInstance();
        MessageEditRegistry edits = MessageEditRegistry.getInstance();
        MessageDeletionRegistry deletions = MessageDeletionRegistry.getInstance();

        dao.clear();
        hashtags.clear();
        threads.clearAll();
        edits.clearAll();
        deletions.clearAll();

        Set<UUID> loaded = new HashSet<>();
        int localCount = file.exists()
                ? addPosts(readArray(file), loaded, threads, edits, deletions)
                : 0;
        int seedCount = addPosts(readSeedArray(appContext), loaded, threads, edits, deletions);
        hashtags.rebuildIndex();
        return seedCount + localCount > 0;
    }

    public static void saveAll(Context context) {
        JSONArray posts = new JSONArray();
        Iterator<Post> iterator = PostDAO.getInstance().getAll();
        while (iterator.hasNext()) {
            posts.put(postToJson(iterator.next()));
        }
        writeArray(postsFile(context), posts);
    }

    private static File postsFile(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), POSTS_FILE);
    }

    private static JSONArray readArray(File file) {
        try (FileInputStream input = new FileInputStream(file)) {
            return readArray(input);
        } catch (IOException | JSONException ignored) {
            return new JSONArray();
        }
    }

    private static JSONArray readSeedArray(Context context) {
        try (InputStream input = context.getAssets().open(SEED_POSTS_FILE)) {
            return readArray(input);
        } catch (IOException | JSONException ignored) {
            return new JSONArray();
        }
    }

    private static JSONArray readArray(InputStream input) throws IOException, JSONException {
        byte[] bytes = input.readAllBytes();
        if (bytes.length == 0) return new JSONArray();
        return new JSONArray(new String(bytes, StandardCharsets.UTF_8));
    }

    private static void writeArray(File file, JSONArray array) {
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException ignored) {
            // Prototype storage should not crash the UI; a later action can retry.
        }
    }

    private static int addPosts(JSONArray posts,
                                Set<UUID> loaded,
                                MessageThreadRegistry threads,
                                MessageEditRegistry edits,
                                MessageDeletionRegistry deletions) {
        int count = 0;
        for (int i = 0; i < posts.length(); i++) {
            Post post = postFromJson(posts.optJSONObject(i), threads, edits, deletions);
            if (post == null || !loaded.add(post.id)) continue;
            PostDAO.getInstance().add(post);
            count++;
        }
        return count;
    }

    private static JSONObject postToJson(Post post) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", post.id.toString());
            json.put("poster", post.poster.toString());
            json.put("topic", post.topic);
            json.put("body", post.getBody());
            json.put("edited", post.isEdited());
            json.put("deleted", post.isDeleted());
            json.put("createdAt", post.getCreatedAt());
            JSONArray tags = new JSONArray();
            for (String tag : post.getHashtags()) tags.put(tag);
            json.put("hashtags", tags);

            JSONArray messages = new JSONArray();
            Iterator<Message> iterator = post.messages.getAll();
            while (iterator.hasNext()) {
                messages.put(messageToJson(iterator.next()));
            }
            json.put("messages", messages);
        } catch (JSONException ignored) {
        }
        return json;
    }

    private static JSONObject messageToJson(Message message) throws JSONException {
        MessageThreadRegistry threads = MessageThreadRegistry.getInstance();
        MessageEditRegistry edits = MessageEditRegistry.getInstance();
        MessageDeletionRegistry deletions = MessageDeletionRegistry.getInstance();

        JSONObject json = new JSONObject();
        json.put("id", message.id().toString());
        json.put("poster", message.poster().toString());
        json.put("thread", message.thread().toString());
        json.put("timestamp", message.timestamp());
        json.put("content", message.message());
        json.put("hidden", message.isHidden());
        json.put("deleted", deletions.isDeleted(message.id()));

        UUID parent = threads.parentOf(message.id());
        if (parent != null) json.put("parent", parent.toString());
        if (edits.isEdited(message.id())) {
            json.put("editedContent", edits.currentContent(message.id(), message.message()));
        }
        return json;
    }

    private static Post postFromJson(JSONObject json,
                                     MessageThreadRegistry threads,
                                     MessageEditRegistry edits,
                                     MessageDeletionRegistry deletions) {
        if (json == null) return null;
        try {
            Post post = new Post(
                    UUID.fromString(json.getString("id")),
                    UUID.fromString(json.getString("poster")),
                    json.optString("topic", ""),
                    json.optBoolean("edited", false),
                    json.optBoolean("deleted", false),
                    json.optLong("createdAt", System.currentTimeMillis())
            );
            post.setBody(json.optString("body", ""));

            JSONArray tags = json.optJSONArray("hashtags");
            if (tags != null) {
                for (int i = 0; i < tags.length(); i++) {
                    String tag = tags.optString(i, "").trim();
                    if (!tag.isEmpty()) post.addHashtag(tag);
                }
            }

            JSONArray messages = json.optJSONArray("messages");
            if (messages != null) {
                for (int i = 0; i < messages.length(); i++) {
                    addMessageFromJson(post, messages.optJSONObject(i), threads, edits, deletions);
                }
            }
            return post;
        } catch (IllegalArgumentException | JSONException ignored) {
            return null;
        }
    }

    private static void addMessageFromJson(Post post,
                                           JSONObject json,
                                           MessageThreadRegistry threads,
                                           MessageEditRegistry edits,
                                           MessageDeletionRegistry deletions) {
        if (json == null) return;
        try {
            UUID id = UUID.fromString(json.getString("id"));
            Message message = new Message(
                    id,
                    UUID.fromString(json.getString("poster")),
                    UUID.fromString(json.optString("thread", post.id.toString())),
                    json.optLong("timestamp", System.currentTimeMillis()),
                    json.optString("content", ""),
                    json.optBoolean("hidden", false)
            );
            post.messages.insert(message);

            String parent = json.optString("parent", "");
            if (!parent.isEmpty()) threads.setParent(id, UUID.fromString(parent));

            if (json.optBoolean("deleted", false)) deletions.markDeleted(id);
            String editedContent = json.optString("editedContent", null);
            if (editedContent != null) edits.recordEdit(id, editedContent);
        } catch (IllegalArgumentException | JSONException ignored) {
        }
    }
}
