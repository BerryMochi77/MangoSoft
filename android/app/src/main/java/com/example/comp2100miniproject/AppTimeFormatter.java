package com.example.comp2100miniproject;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Formats epoch-millisecond timestamps into human-readable strings using the
 * user's saved language and time-zone preferences.
 *
 * Design principles demonstrated:
 *  - Single Responsibility: all time-display logic lives here.
 *  - Separation of concerns: adapters and activities call one method and stay thin.
 *  - Service pattern: stateless helper whose behaviour is driven by the repository.
 *
 * The raw timestamp stored in {@code Message} (UTC epoch ms) is never modified.
 * Time zone and locale are display-only concerns.
 */
public final class AppTimeFormatter {

    private static final String PATTERN = "MMM d, HH:mm";

    private AppTimeFormatter() {}

    /**
     * Format {@code timestamp} using the language and time-zone preferences
     * currently stored for this app instance.
     *
     * @param timestamp epoch milliseconds (UTC)
     * @param context   any Context; the application context is used internally
     * @return formatted date-time string, e.g. "Jan 5, 14:30" or "1月 5, 14:30"
     */
    public static String format(long timestamp, Context context) {
        String tzId = AppPreferencesRepository.getTimeZone(context);
        String lang  = AppPreferencesRepository.getLanguage(context);
        return format(timestamp, tzId, lang);
    }

    /**
     * Format {@code timestamp} with explicit time zone and language code.
     * Use this overload in unit tests or when context is unavailable.
     *
     * @param timestamp  epoch milliseconds (UTC)
     * @param timeZoneId one of the {@code TZ_*} constants from
     *                   {@link AppPreferencesRepository}, or any valid Java
     *                   {@link TimeZone} ID
     * @param languageCode one of the {@code LANG_*} constants from
     *                     {@link AppPreferencesRepository}
     * @return formatted date-time string
     */
    public static String format(long timestamp, String timeZoneId, String languageCode) {
        Locale locale = AppPreferencesRepository.LANG_ZH.equals(languageCode)
                ? Locale.SIMPLIFIED_CHINESE
                : Locale.ENGLISH;
        TimeZone tz = AppPreferencesRepository.TZ_LOCAL.equals(timeZoneId)
                ? TimeZone.getDefault()
                : TimeZone.getTimeZone(timeZoneId);
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN, locale);
        sdf.setTimeZone(tz);
        return sdf.format(new Date(timestamp));
    }

    /**
     * Current wall-clock time formatted with the saved preferences.
     * Used for the live time preview in the Settings screen.
     */
    public static String previewNow(Context context) {
        return format(System.currentTimeMillis(), context);
    }
}
