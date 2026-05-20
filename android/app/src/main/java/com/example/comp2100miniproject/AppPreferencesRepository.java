package com.example.comp2100miniproject;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single source of truth for user-controlled app preferences beyond theme.
 *
 * Shares the {@code "ui_preferences"} SharedPreferences file with
 * {@link ThemeModeManager} so all UI prefs live in one place and survive
 * app restarts automatically.
 *
 * Design principles demonstrated:
 *  - Repository pattern: all preference I/O is centralised here.
 *  - Encapsulation: callers never touch SharedPreferences directly.
 *  - Single Responsibility: this class knows only about storing/reading prefs.
 *  - Separation of concerns: Fragments/Activities contain no preference logic.
 *
 * Message.java is not modified — language and time zone are display-only
 * preferences, not per-message state.
 */
public final class AppPreferencesRepository {

    /** Same file as {@link ThemeModeManager} — one SharedPreferences for all UI prefs. */
    static final String PREFS_NAME = "ui_preferences";

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_TIMEZONE  = "timezone";

    // ── Language codes ──────────────────────────────────────────────────────
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";

    // ── Time zone identifiers ────────────────────────────────────────────────
    /** Use the device's current default time zone. */
    public static final String TZ_LOCAL    = "local";
    public static final String TZ_UTC      = "UTC";
    public static final String TZ_SYDNEY   = "Australia/Sydney";
    public static final String TZ_SHANGHAI = "Asia/Shanghai";

    private AppPreferencesRepository() {}

    // ── Language ─────────────────────────────────────────────────────────────

    /** Persist the user's chosen language code (e.g. {@link #LANG_EN}). */
    public static void saveLanguage(Context ctx, String languageCode) {
        prefs(ctx).edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    /** @return the saved language code; defaults to {@link #LANG_EN}. */
    public static String getLanguage(Context ctx) {
        return prefs(ctx).getString(KEY_LANGUAGE, LANG_EN);
    }

    // ── Time zone ─────────────────────────────────────────────────────────────

    /** Persist the user's chosen time zone id (e.g. {@link #TZ_SYDNEY}). */
    public static void saveTimeZone(Context ctx, String timeZoneId) {
        prefs(ctx).edit().putString(KEY_TIMEZONE, timeZoneId).apply();
    }

    /** @return the saved time zone id; defaults to {@link #TZ_LOCAL}. */
    public static String getTimeZone(Context ctx) {
        return prefs(ctx).getString(KEY_TIMEZONE, TZ_LOCAL);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
