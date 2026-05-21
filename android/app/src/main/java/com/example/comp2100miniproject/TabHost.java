package com.example.comp2100miniproject;

import dao.model.User;

/**
 * Contract between MainActivity (the single Activity that hosts the bottom-nav
 * tabs) and the four tab fragments.
 *
 * Fragments use this to access the signed-in user and to request cross-tab
 * navigation. Keeping it as an interface avoids a hard dependency on
 * MainActivity from inside each Fragment.
 */
public interface TabHost {
    /** The signed-in user. Fragments treat this as read-only. */
    User currentUser();

    /** Switch to the Trends tab and pre-filter it to {@code tag}. */
    void showTrendsForTag(String tag);

    /** Sign out and return to the login screen, clearing the back stack. */
    void requestLogout();

    /**
     * Show {@link SettingsFragment} on top of the current tab. Used by the
     * gear icon in Profile because the bottom nav is capped at 5 items
     * and Settings was demoted from there to make room for the AI tab.
     * Tapping any bottom-nav item restores the normal tab view.
     */
    void openSettings();

    /** Dismiss SettingsFragment and return to the most recent tab. */
    void closeSettings();

    /** Refresh notification badges after a child page marks messages as read. */
    void refreshNotificationBadges();
}
