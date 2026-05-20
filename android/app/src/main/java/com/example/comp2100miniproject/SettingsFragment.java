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
import androidx.fragment.app.Fragment;

/**
 * Settings tab: theme picker + log out. Profile editing now lives in the
 * Profile tab itself, so there is no redundant "Edit profile" row here.
 */
public class SettingsFragment extends Fragment {

    private TabHost host;
    private TextView textThemeCurrent;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textThemeCurrent = view.findViewById(R.id.textThemeCurrent);
        refreshThemeLabel();

        View rowTheme = view.findViewById(R.id.rowTheme);
        rowTheme.setOnClickListener(v -> {
            ThemeModeManager.showModeChooser((AppCompatActivity) requireActivity());
            // The chooser dismisses after a pick; refresh the label whenever
            // we get focus back.
            rowTheme.post(this::refreshThemeLabel);
        });

        view.findViewById(R.id.rowLogout).setOnClickListener(v -> confirmLogout());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (textThemeCurrent != null) {
            refreshThemeLabel();
        }
    }

    private void refreshThemeLabel() {
        textThemeCurrent.setText(ThemeModeManager.getSavedModeLabel(requireContext()));
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> host.requestLogout())
                .show();
    }
}
