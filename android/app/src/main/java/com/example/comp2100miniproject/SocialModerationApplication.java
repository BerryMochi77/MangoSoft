package com.example.comp2100miniproject;

import android.app.Application;

public class SocialModerationApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeModeManager.applySavedMode(this);
    }
}
