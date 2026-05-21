package com.example.comp2100miniproject;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;

import java.util.Locale;

import com.example.comp2100miniproject.auth.AuthManager;

import dao.model.User;
import moderation.AdminModerationService;

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
    private TextView textFontFamilyCurrent;
    private TextView textFontSizeCurrent;

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
        textFontFamilyCurrent = view.findViewById(R.id.textFontFamilyCurrent);
        textFontSizeCurrent = view.findViewById(R.id.textFontSizeCurrent);
        view.findViewById(R.id.rowFontFamily).setOnClickListener(v -> showFontFamilyPicker());
        view.findViewById(R.id.rowFontSize).setOnClickListener(v -> showFontSizePicker());

        // Language
        textLanguageCurrent = view.findViewById(R.id.textLanguageCurrent);
        view.findViewById(R.id.rowLanguage).setOnClickListener(v -> showLanguagePicker());

        // Time zone
        textTimezoneCurrent = view.findViewById(R.id.textTimezoneCurrent);
        textTimezonePreview  = view.findViewById(R.id.textTimezonePreview);
        view.findViewById(R.id.rowTimezone).setOnClickListener(v -> showTimezonePicker());

        // Account
        view.findViewById(R.id.rowProfileVisibility).setOnClickListener(v -> showProfileVisibilityDialog());
        view.findViewById(R.id.rowLogout).setOnClickListener(v -> confirmLogout());

        // Admin section — visible only to admin users
        setupAdminSection(view);

        refreshLabels();
    }

    private void setupAdminSection(View view) {
        View sectionAdmin = view.findViewById(R.id.sectionAdmin);
        TextView textAdminPendingCount = view.findViewById(R.id.textAdminPendingCount);
        boolean isAdmin = host.currentUser().role() == User.Role.Admin;
        sectionAdmin.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (isAdmin) {
            view.findViewById(R.id.rowAdminDashboard).setOnClickListener(v -> openAdminDashboard());
        }
    }

    private void openAdminDashboard() {
        Intent intent = new Intent(requireContext(), AdminPostReportsActivity.class);
        intent.putExtra(AuthManager.EXTRA_USER_ID,
                host.currentUser().getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, true);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (textThemeCurrent != null) refreshLabels();
        // Refresh pending-report count badge for admins
        if (getView() != null && host.currentUser().role() == User.Role.Admin) {
            TextView badge = getView().findViewById(R.id.textAdminPendingCount);
            if (badge != null) {
                int count = AdminModerationService.getInstance().getPendingReports().size();
                badge.setText(count > 0
                        ? getString(R.string.admin_pending_count, count)
                        : "");
            }
        }
    }

    // ── Label refresh ─────────────────────────────────────────────────────────

    private void refreshLabels() {
        textThemeCurrent.setText(ThemeModeManager.getSavedModeLabel(requireContext()));
        textFontFamilyCurrent.setText(fontFamilyLabel(
                AppPreferencesRepository.getFontFamily(requireContext())));
        textFontSizeCurrent.setText(fontSizeLabel(
                AppPreferencesRepository.getFontSize(requireContext())));
        textLanguageCurrent.setText(languageLabel(
                AppPreferencesRepository.getLanguage(requireContext())));
        textTimezoneCurrent.setText(timezoneLabel(
                AppPreferencesRepository.getTimeZone(requireContext())));
        textTimezonePreview.setText(AppTimeFormatter.previewNow(requireContext()));
    }

    private void showFontFamilyPicker() {
        String[] values = {
                AppPreferencesRepository.FONT_SYSTEM,
                AppPreferencesRepository.FONT_SERIF,
                AppPreferencesRepository.FONT_MONOSPACE
        };
        String[] labels = {
                getString(R.string.font_family_system),
                getString(R.string.font_family_serif),
                getString(R.string.font_family_monospace)
        };
        String current = AppPreferencesRepository.getFontFamily(requireContext());
        int checkedItem = indexOf(values, current);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_single_choice,
                labels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                if (row instanceof CheckedTextView) {
                    ((CheckedTextView) row).setTypeface(fontPreviewTypeface(values[position]));
                }
                return row;
            }
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.font_family_title)
                .setSingleChoiceItems(adapter, checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    AppPreferencesRepository.saveFontFamily(requireContext(), values[which]);
                    applyFontPreferenceChange();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showFontSizePicker() {
        String[] values = {
                AppPreferencesRepository.FONT_SIZE_SMALL,
                AppPreferencesRepository.FONT_SIZE_DEFAULT,
                AppPreferencesRepository.FONT_SIZE_LARGE,
                AppPreferencesRepository.FONT_SIZE_EXTRA_LARGE
        };
        String[] labels = {
                getString(R.string.font_size_small),
                getString(R.string.font_size_default),
                getString(R.string.font_size_large),
                getString(R.string.font_size_extra_large)
        };
        String current = AppPreferencesRepository.getFontSize(requireContext());
        int checkedItem = indexOf(values, current);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.font_size_title)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    AppPreferencesRepository.saveFontSize(requireContext(), values[which]);
                    applyFontPreferenceChange();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyFontPreferenceChange() {
        refreshLabels();
        UiFontManager.applyToActivity(requireActivity());
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

    private void showProfileVisibilityDialog() {
        User currentUser = host.currentUser();
        AuthManager authManager = new AuthManager(requireContext());
        AuthManager.ProfileVisibility visibility = authManager.getProfileVisibility(currentUser);

        CheckBox posts = new CheckBox(requireContext());
        posts.setText(R.string.public_posts);
        posts.setChecked(visibility.publicPosts());

        CheckBox replies = new CheckBox(requireContext());
        replies.setText(R.string.public_replies);
        replies.setChecked(visibility.publicReplies());

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 0, padding, 0);
        container.addView(posts);
        container.addView(replies);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_visibility)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) -> {
                    boolean saved = authManager.updateProfileVisibility(
                            currentUser.getUUID(),
                            posts.isChecked(),
                            replies.isChecked()
                    );
                    int message = saved
                            ? R.string.profile_visibility_saved
                            : R.string.profile_save_failed;
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

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

    private String fontFamilyLabel(String value) {
        switch (value) {
            case AppPreferencesRepository.FONT_SERIF:
                return getString(R.string.font_family_serif);
            case AppPreferencesRepository.FONT_MONOSPACE:
                return getString(R.string.font_family_monospace);
            default:
                return getString(R.string.font_family_system);
        }
    }

    private String fontSizeLabel(String value) {
        switch (value) {
            case AppPreferencesRepository.FONT_SIZE_SMALL:
                return getString(R.string.font_size_small);
            case AppPreferencesRepository.FONT_SIZE_LARGE:
                return getString(R.string.font_size_large);
            case AppPreferencesRepository.FONT_SIZE_EXTRA_LARGE:
                return getString(R.string.font_size_extra_large);
            default:
                return getString(R.string.font_size_default);
        }
    }

    private int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(target)) return i;
        }
        return 0;
    }

    private Typeface fontPreviewTypeface(String value) {
        switch (value) {
            case AppPreferencesRepository.FONT_SERIF:
                return Typeface.SERIF;
            case AppPreferencesRepository.FONT_MONOSPACE:
                return Typeface.MONOSPACE;
            default:
                return Typeface.DEFAULT;
        }
    }
}
