package com.example.comp2100miniproject;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

/**
 * Settings tab: theme, language, time zone, and log out.
 *
 * Language and time zone are handled via {@link AppPreferencesRepository}
 * (repository pattern) and formatted through {@link AppTimeFormatter}
 * (single-responsibility). This Fragment contains only UI logic.
 */
public class SettingsFragment extends Fragment {

    private TabHost host;

    // Theme
    private TextView textThemeCurrent;

    // Localisation
    private TextView textLanguageCurrent;
    private TextView textTimezoneCurrent;
    private TextView textTimezonePreview;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TabHost) {
            host = (TabHost) context;
        } else {
            throw new IllegalStateException("SettingsFragment requires a TabHost activity.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Theme
        textThemeCurrent = view.findViewById(R.id.textThemeCurrent);
        View rowTheme = view.findViewById(R.id.rowTheme);
        rowTheme.setOnClickListener(v -> {
            ThemeModeManager.showModeChooser((AppCompatActivity) requireActivity());
            rowTheme.post(this::refreshLabels);
        });

        // Language
        textLanguageCurrent = view.findViewById(R.id.textLanguageCurrent);
        view.findViewById(R.id.rowLanguage).setOnClickListener(v -> showLanguagePicker());

        // Time zone
        textTimezoneCurrent = view.findViewById(R.id.textTimezoneCurrent);
        textTimezonePreview  = view.findViewById(R.id.textTimezonePreview);
        view.findViewById(R.id.rowTimezone).setOnClickListener(v -> showTimezonePicker());

        // Log out
        view.findViewById(R.id.rowLogout).setOnClickListener(v -> confirmLogout());

        refreshLabels();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (textThemeCurrent != null) refreshLabels();
    }

    // ── Label refresh ─────────────────────────────────────────────────────────

    private void refreshLabels() {
        textThemeCurrent.setText(ThemeModeManager.getSavedModeLabel(requireContext()));
        textLanguageCurrent.setText(languageLabel(
                AppPreferencesRepository.getLanguage(requireContext())));
        textTimezoneCurrent.setText(timezoneLabel(
                AppPreferencesRepository.getTimeZone(requireContext())));
        textTimezonePreview.setText(AppTimeFormatter.previewNow(requireContext()));
    }

    // ── Language picker ────────────────────────────────────────────────────────

    private void showLanguagePicker() {
        String[] codes  = { AppPreferencesRepository.LANG_EN, AppPreferencesRepository.LANG_ZH };
        String[] labels = { getString(R.string.lang_english), getString(R.string.lang_chinese) };
        String   current = AppPreferencesRepository.getLanguage(requireContext());
        int checkedItem = current.equals(AppPreferencesRepository.LANG_ZH) ? 1 : 0;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.language_title)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    applyLanguage(codes[which]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyLanguage(String langCode) {
        AppPreferencesRepository.saveLanguage(requireContext(), langCode);
        // AppCompatDelegate re-creates the activity so all strings reload in the new locale.
        Locale locale = AppPreferencesRepository.LANG_ZH.equals(langCode)
                ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale));
        // Activity will be re-created automatically; no need to call recreate() manually.
    }

    // ── Time zone picker ───────────────────────────────────────────────────────

    private void showTimezonePicker() {
        String[] ids = {
                AppPreferencesRepository.TZ_LOCAL,
                AppPreferencesRepository.TZ_UTC,
                AppPreferencesRepository.TZ_SYDNEY,
                AppPreferencesRepository.TZ_SHANGHAI
        };
        String[] labels = {
                getString(R.string.tz_local),
                getString(R.string.tz_utc),
                getString(R.string.tz_sydney),
                getString(R.string.tz_shanghai)
        };
        String current = AppPreferencesRepository.getTimeZone(requireContext());
        int checkedItem = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(current)) { checkedItem = i; break; }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.timezone_title)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    AppPreferencesRepository.saveTimeZone(requireContext(), ids[which]);
                    refreshLabels();      // preview updates immediately, no restart needed
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── Log out ───────────────────────────────────────────────────────────────

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> host.requestLogout())
                .show();
    }

    // ── Label helpers ─────────────────────────────────────────────────────────

    /** Human-readable label for a language code. */
    private String languageLabel(String code) {
        return AppPreferencesRepository.LANG_ZH.equals(code)
                ? getString(R.string.lang_chinese)
                : getString(R.string.lang_english);
    }

    /** Human-readable label for a time zone id. */
    private String timezoneLabel(String id) {
        switch (id) {
            case AppPreferencesRepository.TZ_UTC:      return getString(R.string.tz_utc);
            case AppPreferencesRepository.TZ_SYDNEY:   return getString(R.string.tz_sydney);
            case AppPreferencesRepository.TZ_SHANGHAI: return getString(R.string.tz_shanghai);
            default:                                    return getString(R.string.tz_local);
        }
    }
}
