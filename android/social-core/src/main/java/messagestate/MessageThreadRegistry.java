package messagestate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sidecar registry that records Reddit-style parent/child relationships
 * between messages without touching the {@link dao.model.Message} model.
 *
 * <p>Messages without a recorded parent are top-level replies to the post
 * itself (the original Hackathon 1 model). Messages with a parent are
 * nested replies and render indented under their parent.</p>
 *
 * <p>This follows the same shape as {@link MessageEditRegistry} and
 * {@link MessageDeletionRegistry}, completing the canonical sidecar pattern
 * for per-message state additions: {@code Message} stays immutable, each
 * additional concern lives in its own singleton.</p>
 */
public final class MessageThreadRegistry {

    private static final MessageThreadRegistry INSTANCE = new MessageThreadRegistry();

    /** child message id -> parent message id. */
    private final Map<UUID, UUID> parentOf = new HashMap<>();

    private MessageThreadRegistry() {}

    public static MessageThreadRegistry getInstance() {
        return INSTANCE;
    }

    /** Record that {@code childId} is a reply to {@code parentId}. */
    public void setParent(UUID childId, UUID parentId) {
        if (childId == null || parentId == null) return;
        // Defensive: never let a message claim itself as parent.
        if (childId.equals(parentId)) return;
        parentOf.put(childId, parentId);
    }

    /** The direct parent of {@code messageId}, or {@code null} if top-level. */
    public UUID parentOf(UUID messageId) {
        return messageId == null ? null : parentOf.get(messageId);
    }

    /** {@code true} if the message has no recorded parent. */
    public boolean isTopLevel(UUID messageId) {
        return messageId != null && !parentOf.containsKey(messageId);
    }

    /**
     * How deeply nested this message is — 0 for top-level replies, 1 for a
     * reply to a top-level reply, and so on.
     *
     * <p>Walks up the parent chain. Includes a cheap visited-set guard so a
     * malformed cycle cannot hang the UI.</p>
     */
    public int depthOf(UUID messageId) {
        if (messageId == null) return 0;
        int depth = 0;
        UUID current = parentOf.get(messageId);
        Set<UUID> visited = new HashSet<>();
        visited.add(messageId);
        while (current != null) {
            if (!visited.add(current)) break; // cycle guard
            depth++;
            current = parentOf.get(current);
        }
        return depth;
    }

    /** Drop all parent records. Test helper. */
    public void clearAll() {
        parentOf.clear();
    }

    /**
     * Re-order a flat, time-sorted list of messages into Reddit-style
     * depth-first order: each top-level reply followed immediately by its
     * own subtree.
     *
     * <p>Messages whose parent is not present in {@code timeSorted} are
     * treated as top-level (so a reply to a deleted/hidden parent still
     * surfaces rather than vanishing).</p>
     */
    public <T> List<T> flatten(List<T> timeSorted, java.util.function.Function<T, UUID> idOf) {
        if (timeSorted == null || timeSorted.isEmpty()) return Collections.emptyList();

        Set<UUID> present = new HashSet<>();
        for (T item : timeSorted) present.add(idOf.apply(item));

        Map<UUID, List<T>> children = new HashMap<>();
        List<T> roots = new ArrayList<>();
        for (T item : timeSorted) {
            UUID id = idOf.apply(item);
            UUID parent = parentOf.get(id);
            if (parent == null || !present.contains(parent)) {
                roots.add(item);
            } else {
                children.computeIfAbsent(parent, k -> new ArrayList<>()).add(item);
            }
        }

        List<T> result = new ArrayList<>(timeSorted.size());
        for (T root : roots) {
            appendSubtree(root, children, idOf, result);
        }
        return result;
    }

    private <T> void appendSubtree(T node, Map<UUID, List<T>> children,
                                   java.util.function.Function<T, UUID> idOf, List<T> out) {
        out.add(node);
        List<T> kids = children.get(idOf.apply(node));
        if (kids == null) return;
        for (T kid : kids) appendSubtree(kid, children, idOf, out);
    }
}
