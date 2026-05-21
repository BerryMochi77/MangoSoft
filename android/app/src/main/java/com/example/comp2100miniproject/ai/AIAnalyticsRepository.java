package com.example.comp2100miniproject.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton in-memory store for AI curation analytics.
 *
 * Design principles:
 *  - Repository pattern: all AI analytics I/O goes through this class.
 *  - Encapsulation: internal maps are never exposed; callers get copies.
 *  - Single Responsibility: only stores/retrieves AI curation data.
 *  - Separation of concerns: analytics calculations live in
 *    {@link AIAnalyticsService}, not here.
 *
 * Populated by {@link AiFragment} whenever a curation batch completes.
 * Message.java is NOT modified — this stores post-level, not message-level state.
 */
public final class AIAnalyticsRepository {

    private static AIAnalyticsRepository instance;

    /**
     * Latest curation verdict per post (keyed by postId).
     * A LinkedHashMap preserves insertion order for predictable analytics.
     * If the same post is scored twice (user runs curation again), the new
     * verdict replaces the old one — deduplication by postId.
     */
    private final Map<UUID, AIAnalysisRecord> records = new LinkedHashMap<>();

    /** All user feedback events — one per tap, no deduplication. */
    private final List<AIFeedbackRecord> feedback = new ArrayList<>();

    private AIAnalyticsRepository() {}

    public static AIAnalyticsRepository getInstance() {
        if (instance == null) instance = new AIAnalyticsRepository();
        return instance;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Record an entire curation batch returned by
     * {@link AiPostCurationStrategy}.  The latest verdict for each post
     * replaces any earlier one from a previous curation run.
     *
     * @param curated  full result list from the strategy (includes both
     *                 worth-reading and filtered posts)
     */
    public void recordBatch(List<CuratedPost> curated) {
        if (curated == null) return;
        long now = System.currentTimeMillis();
        for (CuratedPost cp : curated) {
            if (cp == null || cp.post == null) continue;
            String category = AIAnalyticsService.inferCategory(cp.worthReading, cp.summary);
            records.put(cp.post.id, new AIAnalysisRecord(
                    cp.post.id, cp.score, cp.worthReading, cp.summary, now, category));
        }
    }

    /**
     * Record one user feedback action (e.g. "Not relevant" or "Show anyway").
     */
    public void recordFeedback(UUID postId, UUID userId, AIFeedbackType type) {
        if (postId == null || type == null) return;
        feedback.add(new AIFeedbackRecord(postId, userId, type));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All AI analysis records, newest first. */
    public List<AIAnalysisRecord> getAllRecords() {
        List<AIAnalysisRecord> copy = new ArrayList<>(records.values());
        Collections.reverse(copy);   // LinkedHashMap is insertion-ordered; newest = most recently put
        return Collections.unmodifiableList(copy);
    }

    /** All user feedback records. */
    public List<AIFeedbackRecord> getAllFeedback() {
        return Collections.unmodifiableList(new ArrayList<>(feedback));
    }

    /** Reset — for tests. */
    public void clear() {
        records.clear();
        feedback.clear();
    }
}
