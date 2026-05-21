package com.example.comp2100miniproject.ai;

import java.util.List;

import dao.model.Post;

/**
 * Strategy interface for "given these posts, which are worth the user's
 * time?". One implementation (AI-powered) lives in
 * {@link AiPostCurationStrategy}; future offline / keyword-based
 * strategies can plug in here without changing the UI.
 *
 * <p>Implementations are called off the main thread. They must not
 * touch the UI, must complete within a reasonable timeout, and must
 * surface failures via {@link Callback#onError(Throwable)} instead of
 * throwing.</p>
 */
public interface PostCurationStrategy {

    interface Callback {
        void onResult(List<CuratedPost> curated);

        void onError(Throwable error);
    }

    /**
     * Curate {@code posts}, invoking {@code callback} on completion.
     *
     * @param posts        the posts to evaluate (caller-supplied order).
     * @param viewerHint   short description of the viewer for context
     *                     (e.g. "username + role"). Implementations may
     *                     ignore it.
     * @param preferences  the viewer's stated interests, free-text. An
     *                     empty string means "no preferences set, use a
     *                     generic relevance heuristic". Implementations
     *                     should treat this as the dominant filter
     *                     criterion when non-empty.
     * @param callback     always invoked exactly once.
     */
    void curate(List<Post> posts, String viewerHint, String preferences, Callback callback);
}
