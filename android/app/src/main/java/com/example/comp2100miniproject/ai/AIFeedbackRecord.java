package com.example.comp2100miniproject.ai;

import java.util.UUID;

/**
 * One user feedback action on an AI curation decision.
 * Stored in {@link AIAnalyticsRepository}; never added to Message.java.
 */
public final class AIFeedbackRecord {

    public final UUID           postId;
    public final UUID           userId;
    public final AIFeedbackType type;
    public final long           createdAt;

    AIFeedbackRecord(UUID postId, UUID userId, AIFeedbackType type) {
        this.postId    = postId;
        this.userId    = userId;
        this.type      = type;
        this.createdAt = System.currentTimeMillis();
    }
}
