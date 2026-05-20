package postview;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton in-memory service that tracks how many times each post has been opened.
 *
 * Design principles:
 *  - Separation of concerns: view-count logic lives here, not in Activity code.
 *  - Encapsulation: the internal map is never exposed directly.
 *  - Single Responsibility: this class only counts post views.
 *  - Repository / Service pattern: matches HashtagService and ReactionManager.
 *
 * Persistence note: view counts are held in memory for the lifetime of the app
 * process. They are NOT persisted across app restarts (no CSV pipeline is wired up
 * for this runtime). This matches all other runtime-only state in this prototype
 * (reactions, hashtag index, etc.).
 *
 * Message.java is intentionally NOT modified — view counts are post-level state,
 * not message-level state.
 */
public final class PostViewService {

    private static PostViewService instance;

    /** postId → total number of times the detail screen has been opened. */
    private final Map<UUID, Integer> viewCounts = new HashMap<>();

    private PostViewService() {}

    public static PostViewService getInstance() {
        if (instance == null) instance = new PostViewService();
        return instance;
    }

    /**
     * Increment the view count for {@code postId} by one.
     * Call this once each time the post detail screen is opened fresh
     * (i.e. on a new Activity creation, not on configuration changes).
     *
     * @param postId the UUID of the post being viewed
     */
    public void recordView(UUID postId) {
        viewCounts.merge(postId, 1, Integer::sum);
    }

    /**
     * Return the total number of times {@code postId} has been viewed.
     * Returns 0 if the post has never been opened.
     *
     * @param postId the UUID of the post to query
     * @return non-negative view count
     */
    public int getViewCount(UUID postId) {
        return viewCounts.getOrDefault(postId, 0);
    }

    /** Reset all counts. Used in tests and when data is fully reloaded. */
    public void clear() {
        viewCounts.clear();
    }
}
