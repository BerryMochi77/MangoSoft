package messagestate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sidecar registry that records edits to messages without modifying the
 * immutable {@link dao.model.Message} domain object.
 *
 * Why a sidecar: the Hackathon brief requires that any additional per-message
 * state stay out of {@code Message}, so {@code Message.message()} keeps
 * returning the originally-posted content forever. Anything that wants to
 * display "what the post looks like right now" should consult this registry
 * via {@link #currentContent(UUID, String)}.
 *
 * This is the canonical pattern for adding new per-message state in this
 * codebase. {@link MessageDeletionRegistry} follows the same shape, and a
 * future Reddit-style reply-thread feature should add a sibling
 * {@code MessageThreadRegistry} here rather than touching {@code Message}.
 */
public final class MessageEditRegistry {

    private static final MessageEditRegistry INSTANCE = new MessageEditRegistry();

    /** Maps message id -> the latest user-supplied content. */
    private final Map<UUID, String> edits = new HashMap<>();

    private MessageEditRegistry() {}

    public static MessageEditRegistry getInstance() {
        return INSTANCE;
    }

    /** Whether {@code messageId} has been edited since it was first posted. */
    public boolean isEdited(UUID messageId) {
        return messageId != null && edits.containsKey(messageId);
    }

    /**
     * Resolve the content to display for a message. If the user edited it
     * the latest version is returned, otherwise the original is returned
     * verbatim.
     */
    public String currentContent(UUID messageId, String originalContent) {
        if (messageId == null) return originalContent;
        String edited = edits.get(messageId);
        return edited == null ? originalContent : edited;
    }

    /** Record (or replace) the latest content for {@code messageId}. */
    public void recordEdit(UUID messageId, String newContent) {
        if (messageId == null || newContent == null) return;
        edits.put(messageId, newContent);
    }

    /** Forget any edit history for {@code messageId}. Used by tests / wipes. */
    public void clear(UUID messageId) {
        edits.remove(messageId);
    }

    /** Drop everything. Test helper. */
    public void clearAll() {
        edits.clear();
    }
}
