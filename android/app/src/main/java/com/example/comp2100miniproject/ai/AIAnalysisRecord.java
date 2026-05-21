package com.example.comp2100miniproject.ai;

import java.util.UUID;

/**
 * Immutable snapshot of one AI curation verdict for a single post.
 *
 * Stored in {@link AIAnalyticsRepository} so admin analytics can report
 * on the AI's filtering behaviour without modifying {@link dao.model.Post}
 * or {@link dao.model.Message} — both remain untouched.
 *
 * Fields map directly from {@link CuratedPost} fields returned by
 * {@link AiPostCurationStrategy}; no new AI API calls are made here.
 */
public final class AIAnalysisRecord {

    /** Post that was scored. */
    public final UUID   postId;
    /** 0–10 relevance score as assigned by the AI. */
    public final int    score;
    /** True = AI said "worth reading"; false = AI said "not relevant / filter". */
    public final boolean worthReading;
    /** AI's one-line explanation (used for category inference). */
    public final String  summary;
    /** Wall-clock ms when this verdict was recorded. */
    public final long    scoredAt;
    /** Derived category from {@link AIAnalyticsService#inferCategory(String)}. */
    public final String  category;

    AIAnalysisRecord(UUID postId, int score, boolean worthReading,
                     String summary, long scoredAt, String category) {
        this.postId       = postId;
        this.score        = Math.max(0, Math.min(10, score));
        this.worthReading = worthReading;
        this.summary      = summary != null ? summary : "";
        this.scoredAt     = scoredAt;
        this.category     = category != null ? category : "low_relevance";
    }
}
