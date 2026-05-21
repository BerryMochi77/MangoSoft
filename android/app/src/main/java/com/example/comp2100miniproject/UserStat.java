package com.example.comp2100miniproject;

/** Immutable snapshot of per-user post-count statistics for the Active Users ranking. */
public final class UserStat {
    public final String username;
    public final int    postCount;

    UserStat(String username, int postCount) {
        this.username  = username != null ? username : "?";
        this.postCount = postCount;
    }
}
