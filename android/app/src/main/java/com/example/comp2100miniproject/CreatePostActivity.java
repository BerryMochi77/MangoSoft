package com.example.comp2100miniproject;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.comp2100miniproject.auth.AuthManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;
import hashtag.HashtagService;

public class CreatePostActivity extends AppCompatActivity {
    private AuthManager authManager;
    private User currentUser;
    private EditText inputTitle;
    private EditText inputBody;
    private EditText inputTags;
    private ChipGroup chipGroupTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_post);

        authManager = new AuthManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null) {
            finish();
            return;
        }

        inputTitle = findViewById(R.id.inputPostTitle);
        inputBody = findViewById(R.id.inputPostBody);
        inputTags = findViewById(R.id.inputPostTags);
        chipGroupTags = findViewById(R.id.chipGroupPostTags);
        inputBody.setGravity(Gravity.TOP | Gravity.START);
        inputTags.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                renderTagPreview();
            }
        });

        ImageButton buttonBack = findViewById(R.id.buttonBackCreatePost);
        Button buttonPublish = findViewById(R.id.buttonPublishPost);
        buttonBack.setOnClickListener(v -> finish());
        buttonPublish.setOnClickListener(v -> publishPost());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void publishPost() {
        String cleanTitle = inputTitle.getText().toString().trim();
        if (cleanTitle.isEmpty()) {
            Toast.makeText(this, R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanBody = inputBody.getText().toString().trim();
        List<String> tags = normalizedTags(inputTags.getText().toString(), cleanTitle, cleanBody);
        Post post = new Post(UUID.randomUUID(), currentUser.getUUID(), cleanTitle);
        post.setBody(cleanBody);
        post.setHashtags(tags);
        PostDAO.getInstance().add(post);
        HashtagService.getInstance().indexPost(post);
        Toast.makeText(this, R.string.post_created, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void renderTagPreview() {
        chipGroupTags.removeAllViews();
        for (String tag : normalizedTags(inputTags.getText().toString(), "", "")) {
            Chip chip = new Chip(this);
            chip.setText("#" + tag);
            chip.setChipBackgroundColorResource(R.color.chip_hashtag_bg);
            chip.setTextColor(getColor(R.color.accent));
            chip.setTextSize(12f);
            chip.setEnsureMinTouchTargetSize(false);
            chipGroupTags.addView(chip);
        }
    }

    private List<String> normalizedTags(String rawTags, String title, String body) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();

        if (rawTags != null) {
            String[] parts = rawTags.split("[,\\s]+");
            for (String part : parts) {
                String clean = normalizeTag(part);
                if (!clean.isEmpty()) tags.add(clean);
            }
        }

        tags.addAll(HashtagParser.extract(title + " " + body));
        return new ArrayList<>(tags);
    }

    private String normalizeTag(String value) {
        if (value == null) return "";
        String clean = value.trim();
        while (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        clean = clean.replaceAll("[^A-Za-z0-9_]", "").toLowerCase();
        return clean;
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
