package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.comp2100miniproject.auth.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.UUID;

import dao.model.User;

public class SettingsActivity extends AppCompatActivity {
    private AuthManager authManager;
    private User currentUser;
    private TextView textThemeCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        authManager = new AuthManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null) {
            openLogin();
            return;
        }

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        textThemeCurrent = findViewById(R.id.textThemeCurrent);
        refreshThemeLabel();

        View rowTheme = findViewById(R.id.rowTheme);
        rowTheme.setOnClickListener(v -> {
            ThemeModeManager.showModeChooser(this);
            // The chooser dismisses after a pick; refresh the label whenever
            // the user returns to this screen.
            rowTheme.post(this::refreshThemeLabel);
        });

        findViewById(R.id.rowEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
            putCurrentUser(intent);
            startActivity(intent);
        });

        findViewById(R.id.rowLogout).setOnClickListener(v -> confirmLogout());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navSettings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navSettings) {
                return true;
            } else if (id == R.id.navFeed) {
                finish();
                return false;
            } else if (id == R.id.navTrending) {
                Intent intent = new Intent(SettingsActivity.this, HashtagSearchActivity.class);
                putCurrentUser(intent);
                startActivity(intent);
                return false;
            } else if (id == R.id.navProfile) {
                Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
                putCurrentUser(intent);
                startActivity(intent);
                return false;
            }
            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (textThemeCurrent != null) {
            refreshThemeLabel();
        }
    }

    private void refreshThemeLabel() {
        textThemeCurrent.setText(ThemeModeManager.getSavedModeLabel(this));
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> openLogin())
                .show();
    }

    private UUID readCurrentUserId() {
        String value = getIntent().getStringExtra(AuthManager.EXTRA_USER_ID);
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void putCurrentUser(Intent intent) {
        intent.putExtra(AuthManager.EXTRA_USER_ID, currentUser.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, currentUser.role() == User.Role.Admin);
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
