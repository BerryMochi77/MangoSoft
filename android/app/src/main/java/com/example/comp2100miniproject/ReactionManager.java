package com.example.comp2100miniproject;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ReactionManager {

    private static ReactionManager instance;

    private final HashMap<UUID, ReactionData> reactions;
    private final HashMap<UUID, HashMap<UUID, String>> userReactions;
    private final HashMap<UUID, LinkedHashSet<String>> postReactionOptions;

    private ReactionManager() {
        reactions = new HashMap<>();
        userReactions = new HashMap<>();
        postReactionOptions = new HashMap<>();
    }

    public static ReactionManager getInstance() {

        if (instance == null) {
            instance = new ReactionManager();
        }

        return instance;
    }

    public ReactionData getReactionData(UUID messageId) {

        if (!reactions.containsKey(messageId)) {
            reactions.put(messageId, new ReactionData());
        }

        return reactions.get(messageId);
    }

    public void addReaction(UUID messageId, ReactionType type) {
        getReactionData(messageId).increment(type);
    }

    public Set<String> getReactionOptions(UUID postId) {
        LinkedHashSet<String> options = postReactionOptions.get(postId);
        if (options == null) {
            options = new LinkedHashSet<>();
            options.add("\uD83D\uDC4D");
            options.add("\u2764\uFE0F");
            options.add("\uD83D\uDE02");
            options.add("\uD83D\uDE21");
            postReactionOptions.put(postId, options);
        }
        return options;
    }

    public String getUserReaction(UUID postId, UUID userId) {
        Map<UUID, String> byUser = userReactions.get(postId);
        if (byUser == null) return null;
        return byUser.get(userId);
    }

    public int getReactionCount(UUID postId, String emoji) {
        Map<UUID, String> byUser = userReactions.get(postId);
        if (byUser == null || emoji == null) return 0;
        int count = 0;
        for (String selected : byUser.values()) {
            if (emoji.equals(selected)) count++;
        }
        return count;
    }

    public void toggleReaction(UUID postId, UUID userId, String emoji) {
        if (postId == null || userId == null || emoji == null || emoji.trim().isEmpty()) return;
        String cleanEmoji = emoji.trim();
        getReactionOptions(postId).add(cleanEmoji);
        HashMap<UUID, String> byUser = userReactions.get(postId);
        if (byUser == null) {
            byUser = new HashMap<>();
            userReactions.put(postId, byUser);
        }

        String current = byUser.get(userId);
        if (cleanEmoji.equals(current)) {
            byUser.remove(userId);
        } else {
            byUser.put(userId, cleanEmoji);
        }
    }
}
