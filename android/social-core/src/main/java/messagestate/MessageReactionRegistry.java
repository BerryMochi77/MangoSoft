package messagestate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sidecar that tracks each user's thumbs-up / thumbs-down reaction against a
 * message. Per-message reaction state is the canonical example the Hackathon
 * brief gives for "additional per-message state" — it explicitly mandates
 * that this live outside {@link dao.model.Message}, so this registry is
 * exactly that.
 *
 * <p>Why per-user rather than just counters: lets a user toggle their own
 * vote and lets the UI light up the icon for the user's current choice.
 * It also keeps the count honest (no spam clicks inflating the score).</p>
 *
 * <p>This is the third sibling under {@code messagestate/}, following the
 * same shape as {@link MessageEditRegistry}, {@link MessageDeletionRegistry}
 * and {@link MessageThreadRegistry}.</p>
 */
public final class MessageReactionRegistry {

    public enum Direction { LIKE, DISLIKE }

    private static final MessageReactionRegistry INSTANCE = new MessageReactionRegistry();

    /** message id -> (user id -> direction). */
    private final Map<UUID, Map<UUID, Direction>> reactions = new HashMap<>();

    private MessageReactionRegistry() {}

    public static MessageReactionRegistry getInstance() {
        return INSTANCE;
    }

    /** The reaction the user currently holds for this message, or {@code null}. */
    public Direction reactionOf(UUID messageId, UUID userId) {
        if (messageId == null || userId == null) return null;
        Map<UUID, Direction> perMessage = reactions.get(messageId);
        return perMessage == null ? null : perMessage.get(userId);
    }

    /**
     * Toggle / switch a user's reaction. Clicking the same direction again
     * clears the vote. Returns the new direction, or {@code null} when the
     * vote was cleared.
     */
    public Direction toggle(UUID messageId, UUID userId, Direction direction) {
        if (messageId == null || userId == null || direction == null) return null;
        Map<UUID, Direction> perMessage =
                reactions.computeIfAbsent(messageId, k -> new HashMap<>());
        Direction current = perMessage.get(userId);
        if (current == direction) {
            perMessage.remove(userId);
            return null;
        }
        perMessage.put(userId, direction);
        return direction;
    }

    public int likeCount(UUID messageId) {
        return countOf(messageId, Direction.LIKE);
    }

    public int dislikeCount(UUID messageId) {
        return countOf(messageId, Direction.DISLIKE);
    }

    private int countOf(UUID messageId, Direction direction) {
        Map<UUID, Direction> perMessage = reactions.get(messageId);
        if (perMessage == null) return 0;
        int count = 0;
        for (Direction d : perMessage.values()) {
            if (d == direction) count++;
        }
        return count;
    }

    /** Test helper. */
    public void clearAll() {
        reactions.clear();
    }
}
