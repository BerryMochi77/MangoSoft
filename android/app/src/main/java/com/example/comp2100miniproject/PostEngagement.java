package com.example.comp2100miniproject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dao.model.Post;
import postview.PostViewService;

public final class PostEngagement {
    private static final int REACTION_WEIGHT = 4;
    private static final int MAX_REACTIONS_IN_SUMMARY = 4;

    private PostEngagement() {}

    public static int hotScore(Post post) {
        return viewCount(post) + reactionCount(post) * REACTION_WEIGHT;
    }

    public static int viewCount(Post post) {
        return PostViewService.getInstance().getViewCount(post.id);
    }

    public static int reactionCount(Post post) {
        return ReactionManager.getInstance().getTotalReactionCount(post.id);
    }

    public static String formatCreatedAt(Post post) {
        SimpleDateFormat format = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        return format.format(new Date(post.getCreatedAt()));
    }

    public static String statsLine(Post post) {
        StringBuilder builder = new StringBuilder();
        builder.append(viewCount(post)).append(" views");
        String reactionSummary = reactionSummary(post);
        if (!reactionSummary.isEmpty()) {
            builder.append("  ").append(reactionSummary);
        }
        return builder.toString();
    }

    public static String reactionSummary(Post post) {
        ReactionManager manager = ReactionManager.getInstance();
        List<String> parts = new ArrayList<>();
        for (String emoji : manager.getReactionOptions(post.id)) {
            int count = manager.getReactionCount(post.id, emoji);
            if (count > 0) {
                parts.add(emoji + " " + count);
            }
            if (parts.size() >= MAX_REACTIONS_IN_SUMMARY) {
                break;
            }
        }
        return String.join("  ", parts);
    }
}
