package com.example.comp2100miniproject;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import userrelation.UserRelationshipRegistry;

public class RelationshipStore {
    private static final String PREFS = "user_relationships";
    private static final String KEY_FOLLOWS = "follows";
    private static final String KEY_FRIENDS = "friends";
    private static final String SEP = ">";

    private final SharedPreferences prefs;
    private final UserRelationshipRegistry registry = UserRelationshipRegistry.getInstance();

    public RelationshipStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public boolean isFollowing(UUID follower, UUID target) {
        return registry.isFollowing(follower, target);
    }

    public Set<UUID> followingOf(UUID userId) {
        return registry.followingOf(userId);
    }

    public boolean toggleFollow(UUID follower, UUID target) {
        boolean following = registry.toggleFollow(follower, target);
        persistFollow(follower, target);
        return following;
    }

    public boolean areFriends(UUID first, UUID second) {
        return registry.areFriends(first, second);
    }

    public Set<UUID> friendsOf(UUID userId) {
        return registry.friendsOf(userId);
    }

    public boolean toggleFriend(UUID first, UUID second) {
        boolean friends = registry.toggleFriend(first, second);
        persistFriend(first, second);
        return friends;
    }

    private void load() {
        registry.clearAll();
        for (String item : prefs.getStringSet(KEY_FOLLOWS, new LinkedHashSet<>())) {
            UUID[] pair = parseDirectedPair(item);
            if (pair != null) registry.setFollowing(pair[0], pair[1], true);
        }
        for (String item : prefs.getStringSet(KEY_FRIENDS, new LinkedHashSet<>())) {
            UUID[] pair = parseDirectedPair(item);
            if (pair != null) registry.setFriends(pair[0], pair[1], true);
        }
    }

    private void persistFollow(UUID follower, UUID target) {
        Set<String> follows = new LinkedHashSet<>(prefs.getStringSet(KEY_FOLLOWS, new LinkedHashSet<>()));
        String key = encode(follower, target);
        if (registry.isFollowing(follower, target)) {
            follows.add(key);
        } else {
            follows.remove(key);
        }
        prefs.edit().putStringSet(KEY_FOLLOWS, follows).apply();
    }

    private void persistFriend(UUID first, UUID second) {
        Set<String> friends = new LinkedHashSet<>(prefs.getStringSet(KEY_FRIENDS, new LinkedHashSet<>()));
        String key = encodeFriend(first, second);
        if (registry.areFriends(first, second)) {
            friends.add(key);
        } else {
            friends.remove(key);
        }
        prefs.edit().putStringSet(KEY_FRIENDS, friends).apply();
    }

    private String encodeFriend(UUID first, UUID second) {
        if (first.compareTo(second) <= 0) return encode(first, second);
        return encode(second, first);
    }

    private String encode(UUID first, UUID second) {
        return first + SEP + second;
    }

    private UUID[] parseDirectedPair(String value) {
        if (value == null) return null;
        String[] parts = value.split(SEP, 2);
        if (parts.length != 2) return null;
        try {
            return new UUID[]{UUID.fromString(parts[0]), UUID.fromString(parts[1])};
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
