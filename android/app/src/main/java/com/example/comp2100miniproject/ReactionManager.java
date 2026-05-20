package com.example.comp2100miniproject;

import java.util.HashMap;
import java.util.UUID;

public class ReactionManager {

    private static ReactionManager instance;

    private final HashMap<UUID, ReactionData> reactions;

    private ReactionManager() {
        reactions = new HashMap<>();
    }

    public static ReactionManager getInstance() {

        if (instance == null) {
            instance = new ReactionManager();
        }

        return instance;
    }

    public ReactionData getReactionData(UUID messageId) {

        if (!reactions.containsKey(messageId)) {
            reactions.put(messageId, new ReactionData());
        }

        return reactions.get(messageId);
    }

    public void addReaction(UUID messageId, ReactionType type) {
        getReactionData(messageId).increment(type);
    }
}