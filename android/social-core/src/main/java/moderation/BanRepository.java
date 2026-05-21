package moderation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Singleton in-memory store of banned user IDs.
 *
 * A banned user cannot create new posts. Their existing posts remain visible
 * (minimal, safe integration — no bulk data mutation on ban).
 *
 * Message.java is not modified — ban state is user-level, not message-level.
 * Separate from FrozenUserManager (which tracks report-freezes on messagereporters).
 */
public final class BanRepository {

    private static BanRepository instance;

    private final Set<UUID> bannedUsers = new HashSet<>();

    private BanRepository() {}

    public static BanRepository getInstance() {
        if (instance == null) instance = new BanRepository();
        return instance;
    }

    /** Mark {@code userId} as banned. Idempotent. */
    public void ban(UUID userId) {
        bannedUsers.add(userId);
    }

    /** Remove the ban on {@code userId}. Idempotent. */
    public void unban(UUID userId) {
        bannedUsers.remove(userId);
    }

    /** @return {@code true} if this user is currently banned. */
    public boolean isBanned(UUID userId) {
        return userId != null && bannedUsers.contains(userId);
    }

    /** @return unmodifiable snapshot of all banned user IDs. */
    public Set<UUID> getBannedUsers() {
        return Collections.unmodifiableSet(new HashSet<>(bannedUsers));
    }

    /** Reset — used in tests. */
    public void clear() {
        bannedUsers.clear();
    }
}
