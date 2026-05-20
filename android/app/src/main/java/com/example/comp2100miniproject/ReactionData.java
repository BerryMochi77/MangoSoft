package com.example.comp2100miniproject;

public class ReactionData {

    private int likes;
    private int hearts;
    private int laughs;
    private int angries;

    public void increment(ReactionType type) {
        switch (type) {
            case LIKE:
                likes++;
                break;

            case HEART:
                hearts++;
                break;

            case LAUGH:
                laughs++;
                break;

            case ANGRY:
                angries++;
                break;
        }
    }

    public int getLikes() {
        return likes;
    }

    public int getHearts() {
        return hearts;
    }

    public int getLaughs() {
        return laughs;
    }

    public int getAngries() {
        return angries;
    }
}