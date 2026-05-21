package com.example.comp2100miniproject;

import java.util.UUID;

/**
 * Immutable admin-enriched snapshot of a user account.
 * Contains fields computed by {@link AdminAnalyticsService} so that
 * the list Activity only needs to render, never query repositories.
 *
 * Message.java is not modified — this DTO carries only user-level data.
 */
public final class UserAdminSummary {
    public final UUID   userId;
    public final String username;
    public final String role;       // "Admin" or "Member"
    public final int    postCount;
    public final int    replyCount;
    public final boolean banned;

    UserAdminSummary(UUID userId, String username, String role,
                     int postCount, int replyCount, boolean banned) {
        this.userId     = userId;
        this.username   = username  != null ? username  : "?";
        this.role       = role      != null ? role      : "Member";
        this.postCount  = postCount;
        this.replyCount = replyCount;
        this.banned     = banned;
    }
}
