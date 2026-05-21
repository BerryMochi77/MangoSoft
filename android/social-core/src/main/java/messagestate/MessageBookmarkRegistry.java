package messagestate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sidecar that tracks which messages each user has saved / bookmarked.
 * Per-message state, kept out of {@link dao.model.Message} per the
 * Hackathon brief.
 *
 * <p>Indexed by {@code userId} rather than {@code messageId} because the
 * dominant read is "what has this user bookmarked" (e.g., a future
 * "Saved" tab) — but {@link #isBookmarked} is still O(1).</p>
 */
public final class MessageBookmarkRegistry {

    private static final MessageBookmarkRegistry INSTANCE = new MessageBookmarkRegistry();

    /** user id -> set of bookmarked message ids. */
    private final Map<UUID, Set<UUID>> bookmarks = new HashMap<>();

    private MessageBookmarkRegistry() {}

    public static MessageBookmarkRegistry getInstance() {
        return INSTANCE;
    }

    public boolean isBookmarked(UUID userId, UUID messageId) {
        if (userId == null || messageId == null) return false;
        Set<UUID> set = bookmarks.get(userId);
        return set != null && set.contains(messageId);
    }

    /**
     * Add or remove a bookmark. Returns the new state ({@code true} if
     * the message is now bookmarked by {@code userId}).
     */
    public boolean toggle(UUID userId, UUID messageId) {
        if (userId == null || messageId == null) return false;
        Set<UUID> set = bookmarks.computeIfAbsent(userId, k -> new HashSet<>());
        if (set.contains(messageId)) {
            set.remove(messageId);
            return false;
        }
        set.add(messageId);
        return true;
    }

    /** Read-only view of the user's bookmarked message ids. */
    public Set<UUID> bookmarksOf(UUID userId) {
        if (userId == null) return Collections.emptySet();
        Set<UUID> set = bookmarks.get(userId);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    /** Test helper. */
    public void clearAll() {
        bookmarks.clear();
    }
}
