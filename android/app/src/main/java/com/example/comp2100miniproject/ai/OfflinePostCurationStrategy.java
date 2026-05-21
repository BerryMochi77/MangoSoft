package com.example.comp2100miniproject.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dao.model.Post;

public class OfflinePostCurationStrategy implements PostCurationStrategy {

    @Override
    public void curate(
            List<Post> posts,
            String viewerHint,
            String preferences,
            Callback callback
    ) {
        ArrayList<CuratedPost> result = new ArrayList<>();

        String pref = preferences == null
                ? ""
                : preferences.toLowerCase(Locale.ROOT);

        for (Post post : posts) {
            String text = (
                    post.topic + " " + post.getBody() + " " + post.getHashtags()
            ).toLowerCase(Locale.ROOT);

            int score = score(text, pref);
            boolean worthReading = score > 0;

            String summary = worthReading
                    ? "Matches your interest: " + preferences
                    : "Less relevant to your current interests";

            result.add(new CuratedPost(
                    post,
                    worthReading,
                    summary,
                    score
            ));
        }

        callback.onResult(result);
    }

    private int score(String text, String pref) {
        int score = 0;

        if (pref.contains("game") && text.contains("game")) score += 5;
        if (pref.contains("android") && text.contains("android")) score += 5;
        if (pref.contains("study") && text.contains("study")) score += 5;
        if (pref.contains("moderation") && text.contains("moderation")) score += 5;
        if (pref.contains("report") && text.contains("report")) score += 5;
        if (pref.contains("layout") && text.contains("layout")) score += 5;
        if (pref.contains("profile") && text.contains("profile")) score += 5;
        if (pref.contains("test") && text.contains("test")) score += 5;
        if (pref.contains("help") && text.contains("help")) score += 5;

        if (text.contains("#help")) score += 1;
        if (text.contains("#android")) score += 1;
        if (text.contains("#moderation")) score += 1;
        if (text.contains("#study")) score += 1;

        return score;
    }
}