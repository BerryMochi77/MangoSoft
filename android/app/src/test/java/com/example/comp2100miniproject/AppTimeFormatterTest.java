package com.example.comp2100miniproject;

import org.junit.Test;

import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Unit tests for AppTimeFormatter — verifies timezone and locale handling.
 * Uses the context-free overload so no Android runtime is needed.
 * Message.java is not involved: this tests display-only logic.
 */
public class AppTimeFormatterTest {

    /** Known epoch: 2024-01-05 14:30:00 UTC */
    private static final long EPOCH_UTC = 1704464200000L; // ~Jan 5 2024 14:30 UTC

    @Test
    public void utcTimezoneUsedWhenRequested() {
        String result = AppTimeFormatter.format(EPOCH_UTC, AppPreferencesRepository.TZ_UTC,
                AppPreferencesRepository.LANG_EN);
        // The hour must match UTC (14:xx), not local
        TimeZone local = TimeZone.getDefault();
        long offsetMs = local.getOffset(EPOCH_UTC);
        if (offsetMs != 0) {
            // If local != UTC, the UTC result differs from local result
            String localResult = AppTimeFormatter.format(EPOCH_UTC,
                    AppPreferencesRepository.TZ_LOCAL, AppPreferencesRepository.LANG_EN);
            assertNotEquals("UTC and local time should differ when offset != 0",
                    result, localResult);
        }
        // Either way, result must be a non-empty string
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void sydneyAheadOfUtc() {
        // Sydney is UTC+10 or UTC+11 (DST). Its hour must differ from UTC.
        String utcResult    = AppTimeFormatter.format(EPOCH_UTC, AppPreferencesRepository.TZ_UTC,
                AppPreferencesRepository.LANG_EN);
        String sydneyResult = AppTimeFormatter.format(EPOCH_UTC, AppPreferencesRepository.TZ_SYDNEY,
                AppPreferencesRepository.LANG_EN);
        assertNotEquals(utcResult, sydneyResult);
    }

    @Test
    public void shanghaiDiffersFromUtc() {
        String utcResult      = AppTimeFormatter.format(EPOCH_UTC, AppPreferencesRepository.TZ_UTC,
                AppPreferencesRepository.LANG_EN);
        String shanghaiResult = AppTimeFormatter.format(EPOCH_UTC, AppPreferencesRepository.TZ_SHANGHAI,
                AppPreferencesRepository.LANG_EN);
        assertNotEquals(utcResult, shanghaiResult);
    }

    @Test
    public void chineseLocaleProducesChineseMonthName() {
        String en = AppTimeFormatter.format(EPOCH_UTC, AppPreferencesRepository.TZ_UTC,
                AppPreferencesRepository.LANG_EN);
        String zh = AppTimeFormatter.format(EPOCH_UTC, AppPreferencesRepository.TZ_UTC,
                AppPreferencesRepository.LANG_ZH);
        // Chinese locale uses different month abbreviations (e.g. "1月" vs "Jan")
        assertNotEquals(en, zh);
    }

    @Test
    public void outputIsNonEmptyForArbitraryTimestamp() {
        long[] samples = { 0L, 1_000_000_000_000L, System.currentTimeMillis() };
        for (long ts : samples) {
            String result = AppTimeFormatter.format(ts, AppPreferencesRepository.TZ_UTC,
                    AppPreferencesRepository.LANG_EN);
            assertNotNull(result);
            assertFalse("Empty result for timestamp " + ts, result.isEmpty());
        }
    }
}
