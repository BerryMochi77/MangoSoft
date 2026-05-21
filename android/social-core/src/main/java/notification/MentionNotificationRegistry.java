package notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sidecar notification store for replies and @ mentions. Notification state is intentionally
 * separate from Message so the domain model remains unchanged.
 */
public class MentionNotificationRegistry {
    private static MentionNotificationRegistry instance;

    private final Map<UUID, LinkedHashMap<UUID, MentionNotification>> mentionsByRecipient =
            new LinkedHashMap<>();
    private final Map<UUID, LinkedHashMap<UUID, MentionNotification>> repliesByRecipient =
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

        return add(NotificationType.MENTION, mentionsByRecipient,
                recipient, sender, post, message, timestamp, preview);
    }

    public boolean addReply(UUID recipient,
                            UUID sender,
                            UUID post,
                            UUID message,
                            long timestamp,
                            String preview) {
        return add(NotificationType.REPLY, repliesByRecipient,
                recipient, sender, post, message, timestamp, preview);
    }

    private boolean add(NotificationType type,
                        Map<UUID, LinkedHashMap<UUID, MentionNotification>> target,
                        UUID recipient,
                        UUID sender,
                        UUID post,
                        UUID message,
                        long timestamp,
                        String preview) {
        if (recipient == null || sender == null || post == null || message == null) return false;
        if (recipient.equals(sender)) return false;

        LinkedHashMap<UUID, MentionNotification> notifications =
                target.computeIfAbsent(recipient, ignored -> new LinkedHashMap<>());
        if (notifications.containsKey(message)) return false;

        notifications.put(message, new MentionNotification(
                type,
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
        return notificationsFor(mentionsByRecipient, recipient);
    }

    public List<MentionNotification> repliesFor(UUID recipient) {
        return notificationsFor(repliesByRecipient, recipient);
    }

    public int unreadCountFor(UUID recipient) {
        return unreadCount(mentionsByRecipient, recipient) + unreadCount(repliesByRecipient, recipient);
    }

    public void markAllRead(UUID recipient) {
        markAllRead(mentionsByRecipient, recipient);
        markAllRead(repliesByRecipient, recipient);
    }

    public void markRepliesRead(UUID recipient) {
        markAllRead(repliesByRecipient, recipient);
    }

    public void markMentionsRead(UUID recipient) {
        markAllRead(mentionsByRecipient, recipient);
    }

    private List<MentionNotification> notificationsFor(
            Map<UUID, LinkedHashMap<UUID, MentionNotification>> target,
            UUID recipient) {
        LinkedHashMap<UUID, MentionNotification> notifications = target.get(recipient);
        if (notifications == null || notifications.isEmpty()) return Collections.emptyList();

        ArrayList<MentionNotification> result = new ArrayList<>(notifications.values());
        result.sort((left, right) -> Long.compare(right.timestamp(), left.timestamp()));
        return Collections.unmodifiableList(result);
    }

    private int unreadCount(Map<UUID, LinkedHashMap<UUID, MentionNotification>> target,
                            UUID recipient) {
        LinkedHashMap<UUID, MentionNotification> notifications = target.get(recipient);
        if (notifications == null || notifications.isEmpty()) return 0;

        int count = 0;
        for (MentionNotification notification : notifications.values()) {
            if (!notification.isRead()) count++;
        }
        return count;
    }

    private void markAllRead(Map<UUID, LinkedHashMap<UUID, MentionNotification>> target,
                             UUID recipient) {
        LinkedHashMap<UUID, MentionNotification> notifications = target.get(recipient);
        if (notifications == null || notifications.isEmpty()) return;

        for (MentionNotification notification : notifications.values()) {
            notification.markRead();
        }
    }

    public void removeForMessage(UUID message) {
        if (message == null) return;
        removeFrom(mentionsByRecipient, message);
        removeFrom(repliesByRecipient, message);
    }

    private void removeFrom(Map<UUID, LinkedHashMap<UUID, MentionNotification>> target, UUID message) {
        Iterator<LinkedHashMap<UUID, MentionNotification>> it = target.values().iterator();
        while (it.hasNext()) {
            LinkedHashMap<UUID, MentionNotification> notifications = it.next();
            notifications.remove(message);
            if (notifications.isEmpty()) it.remove();
        }
    }

    public void clear() {
        mentionsByRecipient.clear();
        repliesByRecipient.clear();
    }

    public enum NotificationType {
        REPLY,
        MENTION
    }

    public static final class MentionNotification {
        private final NotificationType type;
        private final UUID id;
        private final UUID recipient;
        private final UUID sender;
        private final UUID post;
        private final UUID message;
        private final long timestamp;
        private final String preview;
        private boolean read;

        private MentionNotification(NotificationType type,
                                    UUID id,
                                    UUID recipient,
                                    UUID sender,
                                    UUID post,
                                    UUID message,
                                    long timestamp,
                                    String preview) {
            this.type = type;
            this.id = id;
            this.recipient = recipient;
            this.sender = sender;
            this.post = post;
            this.message = message;
            this.timestamp = timestamp;
            this.preview = preview;
        }

        public NotificationType type() {
            return type;
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

        public boolean isRead() {
            return read;
        }

        private void markRead() {
            read = true;
        }
    }
}
