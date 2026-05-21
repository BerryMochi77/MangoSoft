package com.example.comp2100miniproject;

import java.util.UUID;

/** Immutable snapshot of per-post statistics used by the Admin Analytics Dashboard. */
public final class PostStat {
    public final UUID   postId;
    public final String title;
    public final int    viewCount;
    public final int    totalReactions;

    PostStat(UUID postId, String title, int viewCount, int totalReactions) {
        this.postId         = postId;
        this.title          = title  != null ? title : "";
        this.viewCount      = viewCount;
        this.totalReactions = totalReactions;
    }
}
