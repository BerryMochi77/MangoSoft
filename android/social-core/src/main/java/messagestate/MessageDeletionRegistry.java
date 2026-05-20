package messagestate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Sidecar registry that soft-deletes messages without modifying the immutable
 * {@link dao.model.Message} domain object.
 *
 * Hidden ({@link dao.model.Message#isHidden}) was already part of the message
 * model before Hackathon 2 and represents a moderator-driven visibility flag.
 * "Deleted" is a separate, user-driven action and is the kind of additional
 * per-message state the Hackathon brief explicitly tells us to keep out of
 * the {@code Message} class.
 *
 * Consult this registry whenever you need to know whether a message should
 * still be displayed. {@link dao.model.Post#getVisibleMessages(boolean)} does
 * this filtering for the common case.
 */
public final class MessageDeletionRegistry {

    private static final MessageDeletionRegistry INSTANCE = new MessageDeletionRegistry();

    private final Set<UUID> deleted = new HashSet<>();

    private MessageDeletionRegistry() {}

    public static MessageDeletionRegistry getInstance() {
        return INSTANCE;
    }

    public boolean isDeleted(UUID messageId) {
        return messageId != null && deleted.contains(messageId);
    }

    /** Mark {@code messageId} as deleted. Idempotent. */
    public void markDeleted(UUID messageId) {
        if (messageId == null) return;
        deleted.add(messageId);
    }

    /** Reverse a soft-delete. Currently unused by the UI but useful for tests. */
    public void restore(UUID messageId) {
        deleted.remove(messageId);
    }

    /** Drop everything. Test helper. */
    public void clearAll() {
        deleted.clear();
    }
}
