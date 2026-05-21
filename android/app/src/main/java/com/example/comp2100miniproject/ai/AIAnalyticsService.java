package com.example.comp2100miniproject.ai;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless analytics service that reads {@link AIAnalyticsRepository} and
 * computes an {@link AIAnalyticsSummary} for the Admin AI Filter Analytics page.
 *
 * Design principles:
 *  - Separation of concerns: all calculation here; Activity/Fragment only renders.
 *  - Single Responsibility: only computes AI analytics; never writes data.
 *  - Reuses existing AI data sources; no new API calls.
 *  - Message.java is NOT modified.
 *
 * Category inference: since the existing DeepSeek response does not return
 * an explicit category field (only {@code worth_reading}, {@code score}, and
 * a free-text {@code summary}), categories are derived by scanning the AI's
 * own summary text for known keywords. This is an honest heuristic — it
 * reflects the AI's stated reason for filtering a post.
 */
public final class AIAnalyticsService {

    private AIAnalyticsService() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /** Compute a full {@link AIAnalyticsSummary} from current repository state. */
    public static AIAnalyticsSummary computeSummary() {
        List<AIAnalysisRecord> records  = AIAnalyticsRepository.getInstance().getAllRecords();
        List<AIFeedbackRecord> feedbacks = AIAnalyticsRepository.getInstance().getAllFeedback();

        int total   = records.size();
        int hidden  = 0;
        double scoreSum = 0.0;
        Map<String, Integer> catCounts = new HashMap<>();

        for (AIAnalysisRecord r : records) {
            scoreSum += r.score;
            if (!r.worthReading) {
                hidden++;
                catCounts.merge(r.category, 1, Integer::sum);
            }
        }

        double avgScore = total == 0 ? 0.0 : scoreSum / total;

        String topCategory = "N/A";
        if (!catCounts.isEmpty()) {
            topCategory = catCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");
        }

        int notRelevant = 0, showAnyway = 0;
        for (AIFeedbackRecord fb : feedbacks) {
            if (fb.type == AIFeedbackType.NOT_RELEVANT) notRelevant++;
            else if (fb.type == AIFeedbackType.SHOW_ANYWAY) showAnyway++;
        }

        return new AIAnalyticsSummary(
                total, hidden, avgScore, topCategory,
                Collections.unmodifiableMap(catCounts),
                feedbacks.size(), notRelevant, showAnyway
        );
    }

    // ── Category inference ────────────────────────────────────────────────────

    /**
     * Infer a human-readable filter category from the AI's own summary text.
     * Uses keyword matching against the AI's free-text verdict; the result
     * is returned as a lower-case stable label.
     *
     * Called by {@link AIAnalyticsRepository#recordBatch} so the category
     * is computed once at record time, not re-computed on every query.
     *
     * @param worthReading  true = AI kept the post; false = AI filtered it
     * @param summary       AI's one-line explanation
     * @return  a short category label, e.g. "spam", "harassment", "irrelevant"
     */
    public static String inferCategory(boolean worthReading, String summary) {
        if (worthReading) return "relevant";
        if (summary == null || summary.isBlank()) return "low_relevance";
        String lower = summary.toLowerCase();
        if (lower.contains("spam"))                    return "spam";
        if (lower.contains("harass"))                  return "harassment";
        if (lower.contains("abuse"))                   return "abuse";
        if (lower.contains("irrelevant") ||
                lower.contains("not relevant"))        return "irrelevant";
        if (lower.contains("off-topic") ||
                lower.contains("off topic"))           return "off-topic";
        if (lower.contains("duplicate"))               return "duplicate";
        if (lower.contains("inappropriate"))           return "inappropriate";
        return "low_relevance";
    }
}
