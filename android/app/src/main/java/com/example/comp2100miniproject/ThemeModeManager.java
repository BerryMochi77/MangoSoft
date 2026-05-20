package com.example.comp2100miniproject;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeModeManager {
    private static final String PREFERENCES_NAME = "ui_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";

    private ThemeModeManager() {
    }

    public enum Mode {
        SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.theme_mode_system),
        LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO, R.string.theme_mode_light),
        DARK("dark", AppCompatDelegate.MODE_NIGHT_YES, R.string.theme_mode_dark);

        private final String preferenceValue;
        private final int delegateMode;
        @StringRes
        private final int labelResId;

        Mode(String preferenceValue, int delegateMode, @StringRes int labelResId) {
            this.preferenceValue = preferenceValue;
            this.delegateMode = delegateMode;
            this.labelResId = labelResId;
        }
    }

    public static void applySavedMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(getSavedMode(context).delegateMode);
    }

    public static Mode getSavedMode(Context context) {
        String savedValue = preferences(context).getString(KEY_THEME_MODE, Mode.SYSTEM.preferenceValue);
        for (Mode mode : Mode.values()) {
            if (mode.preferenceValue.equals(savedValue)) {
                return mode;
            }
        }
        return Mode.SYSTEM;
    }

    public static String getSavedModeLabel(Context context) {
        return context.getString(getSavedMode(context).labelResId);
    }

    public static void showModeChooser(AppCompatActivity activity) {
        Mode[] modes = Mode.values();
        String[] labels = new String[modes.length];
        Mode currentMode = getSavedMode(activity);
        int checkedItem = 0;

        for (int i = 0; i < modes.length; i++) {
            labels[i] = activity.getString(modes[i].labelResId);
            if (modes[i] == currentMode) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle(R.string.theme_mode_title)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    saveMode(activity, modes[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void saveMode(Context context, Mode mode) {
        preferences(context)
                .edit()
                .putString(KEY_THEME_MODE, mode.preferenceValue)
                .apply();
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
