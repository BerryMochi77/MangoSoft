package com.example.comp2100miniproject.ai;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Per-user, free-text preference string used to bias the AI curator
 * toward what the viewer actually cares about.
 *
 * <p>Kept tiny on purpose: it's exactly one string per user, stored in
 * {@link SharedPreferences} so it survives process death and every
 * teammate's build sees the same shape with zero extra setup.</p>
 *
 * <p>Per-user (not per-message) state, so the Hackathon "don't touch
 * the Message model" rule doesn't apply here — but we still keep this
 * outside the domain models because it's a UI / personalisation
 * concern, not core social-graph data.</p>
 */
public final class AiUserPreferences {

    private static final String PREFS_FILE = "ai_user_preferences";
    private static final String KEY_PREFIX = "prefs_for_";

    private AiUserPreferences() {
    }

    /** The empty string is the sentinel for "no preferences set yet". */
    public static String get(Context context, UUID userId) {
        if (context == null || userId == null) return "";
        return prefs(context).getString(KEY_PREFIX + userId, "");
    }

    /**
     * Save (or clear) the viewer's preferences. Empty / whitespace-only
     * input is treated as "no preferences" so {@link #get} won't return
     * stale whitespace.
     */
    public static void set(Context context, UUID userId, String preferences) {
        if (context == null || userId == null) return;
        String value = preferences == null ? "" : preferences.trim();
        SharedPreferences.Editor editor = prefs(context).edit();
        if (value.isEmpty()) {
            editor.remove(KEY_PREFIX + userId);
        } else {
            editor.putString(KEY_PREFIX + userId, value);
        }
        editor.apply();
    }

    public static boolean hasPreferences(Context context, UUID userId) {
        return !get(context, userId).isEmpty();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }
}
