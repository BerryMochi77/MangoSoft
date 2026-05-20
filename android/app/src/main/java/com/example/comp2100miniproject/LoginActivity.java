package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.comp2100miniproject.auth.AuthManager;

import dao.model.User;

public class LoginActivity extends AppCompatActivity {
    private AuthManager authManager;
    private EditText inputUsername;
    private EditText inputPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        buttonLogin.setOnClickListener(v -> login());
        buttonRegister.setOnClickListener(v -> register());
    }

    private void login() {
        User user = authManager.login(username(), password());
        if (user == null) {
            Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        openMain(user);
    }

    private void register() {
        User user = authManager.register(username(), password());
        if (user == null) {
            Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
        openMain(user);
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
