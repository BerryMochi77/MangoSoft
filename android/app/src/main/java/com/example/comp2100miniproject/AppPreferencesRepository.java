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
    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String KEY_FONT_SIZE = "font_size";

    // ── Language codes ──────────────────────────────────────────────────────
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";

    // ── Time zone identifiers ────────────────────────────────────────────────
    /** Use the device's current default time zone. */
    public static final String TZ_LOCAL    = "local";
    public static final String TZ_UTC      = "UTC";
    public static final String TZ_SYDNEY   = "Australia/Sydney";
    public static final String TZ_SHANGHAI = "Asia/Shanghai";

    public static final String FONT_SYSTEM = "system";
    public static final String FONT_SERIF = "serif";
    public static final String FONT_MONOSPACE = "monospace";

    public static final String FONT_SIZE_SMALL = "small";
    public static final String FONT_SIZE_DEFAULT = "default";
    public static final String FONT_SIZE_LARGE = "large";
    public static final String FONT_SIZE_EXTRA_LARGE = "extra_large";

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

    public static void saveFontFamily(Context ctx, String fontFamily) {
        prefs(ctx).edit().putString(KEY_FONT_FAMILY, validFontFamily(fontFamily)).apply();
    }

    public static String getFontFamily(Context ctx) {
        return validFontFamily(prefs(ctx).getString(KEY_FONT_FAMILY, FONT_SYSTEM));
    }

    public static void saveFontSize(Context ctx, String fontSize) {
        prefs(ctx).edit().putString(KEY_FONT_SIZE, validFontSize(fontSize)).apply();
    }

    public static String getFontSize(Context ctx) {
        return validFontSize(prefs(ctx).getString(KEY_FONT_SIZE, FONT_SIZE_DEFAULT));
    }

    public static float fontScale(Context ctx) {
        switch (getFontSize(ctx)) {
            case FONT_SIZE_SMALL:
                return 0.9f;
            case FONT_SIZE_LARGE:
                return 1.15f;
            case FONT_SIZE_EXTRA_LARGE:
                return 1.3f;
            default:
                return 1.0f;
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String validFontFamily(String fontFamily) {
        if (FONT_SERIF.equals(fontFamily) || FONT_MONOSPACE.equals(fontFamily)) {
            return fontFamily;
        }
        return FONT_SYSTEM;
    }

    private static String validFontSize(String fontSize) {
        if (FONT_SIZE_SMALL.equals(fontSize)
                || FONT_SIZE_LARGE.equals(fontSize)
                || FONT_SIZE_EXTRA_LARGE.equals(fontSize)) {
            return fontSize;
        }
        return FONT_SIZE_DEFAULT;
    }
}
