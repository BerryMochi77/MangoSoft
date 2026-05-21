package notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sidecar notification store for @ mentions. Mention state is intentionally
 * separate from Message so the domain model remains unchanged.
 */
public class MentionNotificationRegistry {
    private static MentionNotificationRegistry instance;

    private final Map<UUID, LinkedHashMap<UUID, MentionNotification>> byRecipient =
            new LinkedHashMap<>();

    private MentionNotificationRegistry() {
    }

    public static MentionNotificationRegistry getInstance() {
        if (instance == null) instance = new MentionNotificationRegistry();
        return instance;
    }

    public boolean addMention(UUID recipient,
                              UUID sender,
                              UUID post,
                              UUID message,
                              long timestamp,
                              String preview) {
        if (recipient == null || sender == null || post == null || message == null) return false;

        LinkedHashMap<UUID, MentionNotification> notifications =
                byRecipient.computeIfAbsent(recipient, ignored -> new LinkedHashMap<>());
        if (notifications.containsKey(message)) return false;

        notifications.put(message, new MentionNotification(
                UUID.randomUUID(),
                recipient,
                sender,
                post,
                message,
                timestamp,
                preview == null ? "" : preview
        ));
        return true;
    }

    public List<MentionNotification> mentionsFor(UUID recipient) {
        LinkedHashMap<UUID, MentionNotification> notifications = byRecipient.get(recipient);
        if (notifications == null || notifications.isEmpty()) return Collections.emptyList();

        ArrayList<MentionNotification> result = new ArrayList<>(notifications.values());
        result.sort((left, right) -> Long.compare(right.timestamp(), left.timestamp()));
        return Collections.unmodifiableList(result);
    }

    public void removeForMessage(UUID message) {
        if (message == null) return;
        Iterator<LinkedHashMap<UUID, MentionNotification>> it = byRecipient.values().iterator();
        while (it.hasNext()) {
            LinkedHashMap<UUID, MentionNotification> notifications = it.next();
            notifications.remove(message);
            if (notifications.isEmpty()) it.remove();
        }
    }

    public void clear() {
        byRecipient.clear();
    }

    public static final class MentionNotification {
        private final UUID id;
        private final UUID recipient;
        private final UUID sender;
        private final UUID post;
        private final UUID message;
        private final long timestamp;
        private final String preview;

        private MentionNotification(UUID id,
                                    UUID recipient,
                                    UUID sender,
                                    UUID post,
                                    UUID message,
                                    long timestamp,
                                    String preview) {
            this.id = id;
            this.recipient = recipient;
            this.sender = sender;
            this.post = post;
            this.message = message;
            this.timestamp = timestamp;
            this.preview = preview;
        }

        public UUID id() {
            return id;
        }

        public UUID recipient() {
            return recipient;
        }

        public UUID sender() {
            return sender;
        }

        public UUID post() {
            return post;
        }

        public UUID message() {
            return message;
        }

        public long timestamp() {
            return timestamp;
        }

        public String preview() {
            return preview;
        }
    }
}
