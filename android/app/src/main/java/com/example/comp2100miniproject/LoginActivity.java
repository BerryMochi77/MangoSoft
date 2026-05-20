package com.example.comp2100miniproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.example.comp2100miniproject.auth.AuthManager;

import dao.model.User;

public class LoginActivity extends AppCompatActivity {
    private static final String PREF_LOGIN = "login_preferences";
    private static final String KEY_REMEMBER = "remember_credentials";
    private static final String KEY_USERNAME = "remembered_username";
    private static final String KEY_PASSWORD = "remembered_password";

    private AuthManager authManager;
    private AvatarManager avatarManager;
    private EditText inputUsername;
    private EditText inputPassword;
    private CheckBox checkboxRememberCredentials;
    private ImageView imageLoginAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);
        avatarManager = new AvatarManager(authManager);
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        checkboxRememberCredentials = findViewById(R.id.checkboxRememberCredentials);
        imageLoginAvatar = findViewById(R.id.imageLoginAvatar);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        buttonLogin.setOnClickListener(v -> login());
        buttonRegister.setOnClickListener(v -> register());
        checkboxRememberCredentials.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) clearRememberedCredentials();
        });

        // Preview the avatar of whichever account matches the username being
        // typed. Unknown / empty username falls back to a neutral icon so
        // there is always something visible above the form.
        inputUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                refreshLoginAvatar(s == null ? "" : s.toString());
            }
        });
        loadRememberedCredentials();
    }

    private void refreshLoginAvatar(String username) {
        User user = username.trim().isEmpty() ? null : authManager.findUserByUsername(username);
        if (user == null) {
            // Neutral placeholder: generic person icon, secondary tint.
            imageLoginAvatar.setImageResource(R.drawable.ic_tab_profile);
            ImageViewCompat.setImageTintList(
                    imageLoginAvatar,
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary)));
        } else {
            // Real account: drop the tint and render the user's avatar
            // (default drawable or the file copied from gallery).
            ImageViewCompat.setImageTintList(imageLoginAvatar, null);
            avatarManager.displayAvatar(user, imageLoginAvatar);
        }
    }

    private void login() {
        User user = authManager.login(username(), password());
        if (user == null) {
            Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        updateRememberedCredentials();
        openMain(user);
    }

    private void register() {
        User user = authManager.register(username(), password());
        if (user == null) {
            Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
        updateRememberedCredentials();
        openMain(user);
    }

    private void loadRememberedCredentials() {
        SharedPreferences preferences = getSharedPreferences(PREF_LOGIN, MODE_PRIVATE);
        boolean remember = preferences.getBoolean(KEY_REMEMBER, false);
        checkboxRememberCredentials.setChecked(remember);
        if (remember) {
            inputUsername.setText(preferences.getString(KEY_USERNAME, ""));
            inputPassword.setText(preferences.getString(KEY_PASSWORD, ""));
        }
        refreshLoginAvatar(username());
    }

    private void updateRememberedCredentials() {
        if (!checkboxRememberCredentials.isChecked()) {
            clearRememberedCredentials();
            return;
        }

        getSharedPreferences(PREF_LOGIN, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_REMEMBER, true)
                .putString(KEY_USERNAME, username())
                .putString(KEY_PASSWORD, password())
                .apply();
    }

    private void clearRememberedCredentials() {
        getSharedPreferences(PREF_LOGIN, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private String username() {
        return inputUsername.getText().toString().trim();
    }

    private String password() {
        return inputPassword.getText().toString();
    }

    private void openMain(User user) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(AuthManager.EXTRA_USER_ID, user.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, user.role() == User.Role.Admin);
        startActivity(intent);
        finish();
    }
}
