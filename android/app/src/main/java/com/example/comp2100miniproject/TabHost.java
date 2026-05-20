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
}
