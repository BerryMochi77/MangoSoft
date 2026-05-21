package com.example.comp2100miniproject;

import java.util.List;

/**
 * Immutable aggregate of all admin analytics statistics, computed once by
 * {@link AdminAnalyticsService} and handed to the UI layer for rendering.
 *
 * Demonstrates encapsulation: the Activity only reads these values — it
 * cannot mutate them or reach into the underlying repositories.
 */
public final class DashboardStats {

    // ── System Overview ───────────────────────────────────────────────────────
    /** Number of registered users in UserDAO. */
    public final int totalUsers;
    /** Number of non-deleted posts in PostDAO. */
    public final int totalPosts;
    /** Total replies (messages) across all posts. */
    public final int totalReplies;
    /** Posts whose in-memory createdAt timestamp falls on today's calendar date. */
    public final int postsToday;
    /** PENDING entries in PostReportRepository. */
    public final int pendingReports;

    // ── Popular Posts ─────────────────────────────────────────────────────────
    /** Top posts by view count (PostViewService), descending. */
    public final List<PostStat> mostViewedPosts;
    /** Top posts by total emoji reactions (ReactionManager), descending. */
    public final List<PostStat> mostReactedPosts;

    // ── Active Users ──────────────────────────────────────────────────────────
    /** Users ranked by number of (non-deleted) posts they have authored. */
    public final List<UserStat> activeUsers;

    DashboardStats(int totalUsers, int totalPosts, int totalReplies, int postsToday,
                   int pendingReports,
                   List<PostStat> mostViewedPosts, List<PostStat> mostReactedPosts,
                   List<UserStat> activeUsers) {
        this.totalUsers      = totalUsers;
        this.totalPosts      = totalPosts;
        this.totalReplies    = totalReplies;
        this.postsToday      = postsToday;
        this.pendingReports  = pendingReports;
        this.mostViewedPosts  = mostViewedPosts;
        this.mostReactedPosts = mostReactedPosts;
        this.activeUsers      = activeUsers;
    }
}
