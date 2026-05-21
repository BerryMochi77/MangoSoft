package com.example.comp2100miniproject.ai;

import java.util.Map;

/**
 * Immutable aggregate of all AI filter analytics metrics.
 * Computed by {@link AIAnalyticsService} and rendered by
 * {@link com.example.comp2100miniproject.AdminAiAnalyticsActivity}.
 *
 * Demonstrates encapsulation: the Activity only reads these values —
 * it cannot mutate them or reach into the underlying repositories.
 * Message.java is not modified.
 */
public final class AIAnalyticsSummary {

    // ── AI Overview ───────────────────────────────────────────────────────────
    /** Total distinct posts that have been AI-scored this session. */
    public final int    totalScoredPosts;
    /** Posts where AI returned worthReading == false. */
    public final int    hiddenCollapsedPosts;
    /** Average 0–10 score over all scored posts; 0.0 if none. */
    public final double averageRelevanceScore;

    // ── Filtering Pattern ─────────────────────────────────────────────────────
    /** Most common inferred category among filtered posts; null/"N/A" if none. */
    public final String mostFilteredCategory;
    /** Count per inferred category for all filtered posts. */
    public final Map<String, Integer> filteredCategoryCounts;

    // ── User Feedback ─────────────────────────────────────────────────────────
    /** Total feedback events (NOT_RELEVANT + SHOW_ANYWAY). */
    public final int userFeedbackCount;
    /** Times users said "Not relevant" on an AI-recommended post. */
    public final int notRelevantClicks;
    /** Times users clicked "Show anyway" on an AI-filtered post. */
    public final int showAnywayClicks;

    AIAnalyticsSummary(int totalScoredPosts, int hiddenCollapsedPosts,
                       double averageRelevanceScore, String mostFilteredCategory,
                       Map<String, Integer> filteredCategoryCounts,
                       int userFeedbackCount, int notRelevantClicks, int showAnywayClicks) {
        this.totalScoredPosts       = totalScoredPosts;
        this.hiddenCollapsedPosts   = hiddenCollapsedPosts;
        this.averageRelevanceScore  = averageRelevanceScore;
        this.mostFilteredCategory   = mostFilteredCategory;
        this.filteredCategoryCounts = filteredCategoryCounts;
        this.userFeedbackCount      = userFeedbackCount;
        this.notRelevantClicks      = notRelevantClicks;
        this.showAnywayClicks       = showAnywayClicks;
    }
}
