package com.example.comp2100miniproject;

import java.util.List;
import java.util.UUID;

/**
 * Immutable admin-enriched snapshot of a post.
 * Contains all fields needed for both the post list and post detail dialog
 * so that Activities only call the service once, then render from this DTO.
 *
 * Message.java is not modified — reply counts are derived by reading
 * existing raw message storage, never by adding fields to Message.
 */
public final class PostAdminSummary {
    public final UUID        postId;
    public final String      title;
    public final String      authorUsername;
    public final long        createdAt;
    public final int         viewCount;
    public final int         totalReactions;
    public final int         replyCount;
    public final int         reportCount;
    public final boolean     deleted;
    /** Raw iteration index in PostDAO — passed to PostViewerActivity as "post_index". */
    public final int         postIndex;
    public final List<String> hashtags;

    PostAdminSummary(UUID postId, String title, String authorUsername,
                     long createdAt, int viewCount, int totalReactions,
                     int replyCount, int reportCount, boolean deleted,
                     int postIndex, List<String> hashtags) {
        this.postId          = postId;
        this.title           = title != null ? title : "";
        this.authorUsername  = authorUsername != null ? authorUsername : "?";
        this.createdAt       = createdAt;
        this.viewCount       = viewCount;
        this.totalReactions  = totalReactions;
        this.replyCount      = replyCount;
        this.reportCount     = reportCount;
        this.deleted         = deleted;
        this.postIndex       = postIndex;
        this.hashtags        = hashtags;
    }
}
