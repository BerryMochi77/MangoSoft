package userrelation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sidecar registry for user-to-user social relationships.
 *
 * <p>Following is directional: A can follow B without B following A.
 * Friendship is mutual and is stored as a normalized unordered pair.</p>
 */
public final class UserRelationshipRegistry {
    private static final UserRelationshipRegistry INSTANCE = new UserRelationshipRegistry();

    private final Map<UUID, Set<UUID>> following = new HashMap<>();
    private final Set<FriendPair> friends = new HashSet<>();

    private UserRelationshipRegistry() {}

    public static UserRelationshipRegistry getInstance() {
        return INSTANCE;
    }

    public boolean isFollowing(UUID follower, UUID target) {
        if (!validPair(follower, target)) return false;
        Set<UUID> targets = following.get(follower);
        return targets != null && targets.contains(target);
    }

    public boolean toggleFollow(UUID follower, UUID target) {
        if (!validPair(follower, target)) return false;
        Set<UUID> targets = following.computeIfAbsent(follower, ignored -> new HashSet<>());
        if (targets.contains(target)) {
            targets.remove(target);
            return false;
        }
        targets.add(target);
        return true;
    }

    public void setFollowing(UUID follower, UUID target, boolean value) {
        if (!validPair(follower, target)) return;
        Set<UUID> targets = following.computeIfAbsent(follower, ignored -> new HashSet<>());
        if (value) {
            targets.add(target);
        } else {
            targets.remove(target);
        }
    }

    public Set<UUID> followingOf(UUID userId) {
        if (userId == null) return Collections.emptySet();
        Set<UUID> targets = following.get(userId);
        return targets == null ? Collections.emptySet() : Collections.unmodifiableSet(targets);
    }

    public boolean areFriends(UUID first, UUID second) {
        if (!validPair(first, second)) return false;
        return friends.contains(new FriendPair(first, second));
    }

    public boolean toggleFriend(UUID first, UUID second) {
        if (!validPair(first, second)) return false;
        FriendPair pair = new FriendPair(first, second);
        if (friends.contains(pair)) {
            friends.remove(pair);
            return false;
        }
        friends.add(pair);
        return true;
    }

    public void setFriends(UUID first, UUID second, boolean value) {
        if (!validPair(first, second)) return;
        FriendPair pair = new FriendPair(first, second);
        if (value) {
            friends.add(pair);
        } else {
            friends.remove(pair);
        }
    }

    public Set<UUID> friendsOf(UUID userId) {
        if (userId == null) return Collections.emptySet();
        Set<UUID> result = new HashSet<>();
        for (FriendPair pair : friends) {
            UUID other = pair.other(userId);
            if (other != null) result.add(other);
        }
        return Collections.unmodifiableSet(result);
    }

    public void clearAll() {
        following.clear();
        friends.clear();
    }

    private boolean validPair(UUID first, UUID second) {
        return first != null && second != null && !first.equals(second);
    }

    private static final class FriendPair {
        private final UUID low;
        private final UUID high;

        FriendPair(UUID first, UUID second) {
            if (first.compareTo(second) <= 0) {
                low = first;
                high = second;
            } else {
                low = second;
                high = first;
            }
        }

        UUID other(UUID userId) {
            if (low.equals(userId)) return high;
            if (high.equals(userId)) return low;
            return null;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof FriendPair)) return false;
            FriendPair pair = (FriendPair) object;
            return low.equals(pair.low) && high.equals(pair.high);
        }

        @Override
        public int hashCode() {
            return 31 * low.hashCode() + high.hashCode();
        }
    }
}
