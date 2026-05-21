package com.example.comp2100miniproject;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ReactionManager {

    private static ReactionManager instance;

    private final HashMap<UUID, ReactionData> reactions;
    private final HashMap<UUID, HashMap<UUID, LinkedHashSet<String>>> userReactions;
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

    public boolean hasUserReaction(UUID postId, UUID userId, String emoji) {
        Map<UUID, LinkedHashSet<String>> byUser = userReactions.get(postId);
        if (byUser == null) return false;
        Set<String> selected = byUser.get(userId);
        return selected != null && selected.contains(emoji);
    }

    public int getReactionCount(UUID postId, String emoji) {
        Map<UUID, LinkedHashSet<String>> byUser = userReactions.get(postId);
        if (byUser == null || emoji == null) return 0;
        int count = 0;
        for (Set<String> selected : byUser.values()) {
            if (selected.contains(emoji)) count++;
        }
        return count;
    }

    public int getTotalReactionCount(UUID postId) {
        int total = 0;
        for (String emoji : getReactionOptions(postId)) {
            total += getReactionCount(postId, emoji);
        }
        return total;
    }

    public void toggleReaction(UUID postId, UUID userId, String emoji) {
        if (postId == null || userId == null || emoji == null || emoji.trim().isEmpty()) return;
        String cleanEmoji = emoji.trim();
        getReactionOptions(postId).add(cleanEmoji);
        HashMap<UUID, LinkedHashSet<String>> byUser = userReactions.get(postId);
        if (byUser == null) {
            byUser = new HashMap<>();
            userReactions.put(postId, byUser);
        }

        LinkedHashSet<String> selected = byUser.get(userId);
        if (selected == null) {
            selected = new LinkedHashSet<>();
            byUser.put(userId, selected);
        }

        if (selected.contains(cleanEmoji)) {
            selected.remove(cleanEmoji);
        } else {
            selected.add(cleanEmoji);
        }

        if (selected.isEmpty()) {
            byUser.remove(userId);
        }
    }

    public void addUserReaction(UUID postId, UUID userId, String emoji) {
        if (postId == null || userId == null || emoji == null || emoji.trim().isEmpty()) return;
        String cleanEmoji = emoji.trim();
        getReactionOptions(postId).add(cleanEmoji);
        HashMap<UUID, LinkedHashSet<String>> byUser = userReactions.get(postId);
        if (byUser == null) {
            byUser = new HashMap<>();
            userReactions.put(postId, byUser);
        }
        LinkedHashSet<String> selected = byUser.get(userId);
        if (selected == null) {
            selected = new LinkedHashSet<>();
            byUser.put(userId, selected);
        }
        selected.add(cleanEmoji);
    }
}
