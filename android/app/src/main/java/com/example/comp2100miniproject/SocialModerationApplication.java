package com.example.comp2100miniproject;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class SocialModerationApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeModeManager.applySavedMode(this);
        // Re-apply the saved language locale so it survives process restarts.
        // AppCompatDelegate handles Activity recreation and pre-33 compatibility.
        applySavedLocale();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                UiFontManager.applyToActivity(activity);
            }
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {
                UiFontManager.applyToActivity(activity);
            }
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    static void applySavedLocale(android.content.Context ctx) {
        String lang = AppPreferencesRepository.getLanguage(ctx);
        Locale locale = AppPreferencesRepository.LANG_ZH.equals(lang)
                ? Locale.SIMPLIFIED_CHINESE
                : Locale.ENGLISH;
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.create(locale));
    }

    private void applySavedLocale() {
        applySavedLocale(this);
    }
}
