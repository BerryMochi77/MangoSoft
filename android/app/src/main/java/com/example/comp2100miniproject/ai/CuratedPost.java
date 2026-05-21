package com.example.comp2100miniproject.ai;

import dao.model.Post;

/**
 * One row of curated-feed output: an original {@link Post} plus the AI's
 * verdict on whether the user should bother reading it, a one-line
 * summary, and a 0–10 score for ranking.
 *
 * <p>Pure data record — no behaviour — so it can travel from the
 * strategy through the adapter without dragging in either side's
 * dependencies.</p>
 */
public final class CuratedPost {

    public final Post post;
    public final boolean worthReading;
    public final String summary;
    public final int score;

    public CuratedPost(Post post, boolean worthReading, String summary, int score) {
        this.post = post;
        this.worthReading = worthReading;
        this.summary = summary == null ? "" : summary;
        this.score = Math.max(0, Math.min(10, score));
    }
}
