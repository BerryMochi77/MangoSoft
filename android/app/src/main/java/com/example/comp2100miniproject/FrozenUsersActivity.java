package com.example.comp2100miniproject;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.moderation.FrozenUserManager;
import com.example.comp2100miniproject.src.FrozenUserAdapter;

import java.util.ArrayList;
import java.util.UUID;

import dao.model.User;

public class FrozenUsersActivity extends AppCompatActivity {
    private FrozenUserManager frozenUserManager;
    private RecyclerView recyclerFrozenUsers;
    private TextView textNoFrozenUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frozen_users);

        AuthManager authManager = new AuthManager(this);
        User currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null || currentUser.role() != User.Role.Admin) {
            finish();
            return;
        }

        frozenUserManager = new FrozenUserManager(this);
        recyclerFrozenUsers = findViewById(R.id.recyclerFrozenUsers);
        recyclerFrozenUsers.setLayoutManager(new LinearLayoutManager(this));
        textNoFrozenUsers = findViewById(R.id.textNoFrozenUsers);

        Button buttonBack = findViewById(R.id.buttonFrozenBack);
        buttonBack.setOnClickListener(v -> finish());

        loadFrozenUsers();
    }

    private void loadFrozenUsers() {
        ArrayList<UUID> users = new ArrayList<>(frozenUserManager.getFrozenUserIds());
        textNoFrozenUsers.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerFrozenUsers.setVisibility(users.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerFrozenUsers.setAdapter(new FrozenUserAdapter(this, users, userId -> {
            frozenUserManager.unfreeze(userId);
            Toast.makeText(this, R.string.reporter_unfrozen, Toast.LENGTH_SHORT).show();
            loadFrozenUsers();
        }));
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
}
