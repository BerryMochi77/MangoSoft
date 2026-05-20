package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.PostAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import dao.PostDAO;
import dao.RandomContentGenerator;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;
import hashtag.HashtagService;

public class MainActivity extends AppCompatActivity {
    private AuthManager authManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null) {
            openLogin();
            return;
        }

        if (!PostDAO.getInstance().getAll().hasNext()) {
            RandomContentGenerator.populateRandomData();
        }

        TextView textCurrentUser = findViewById(R.id.textCurrentUser);
        textCurrentUser.setText(getString(R.string.signed_in_as, authManager.getDisplayName(currentUser)));

        Button buttonAdminReports = findViewById(R.id.buttonAdminReports);
        buttonAdminReports.setVisibility(currentUser.role() == User.Role.Admin ? View.VISIBLE : View.GONE);
        buttonAdminReports.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AdminReportsActivity.class);
            putCurrentUser(intent);
            startActivity(intent);
        });

        Button buttonNewPost = findViewById(R.id.buttonNewPost);
        buttonNewPost.setOnClickListener(v -> showCreatePostDialog());

        Button navFeed = findViewById(R.id.navFeed);
        Button navTrending = findViewById(R.id.navTrending);
        Button navProfile = findViewById(R.id.navProfile);
        Button navLogout = findViewById(R.id.navLogout);
        navFeed.setEnabled(false);
        navTrending.setOnClickListener(v -> openHashtagSearch(null));
        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            putCurrentUser(intent);
            startActivity(intent);
        });
        navLogout.setOnClickListener(v -> openLogin());

        loadPosts();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authManager != null && currentUser != null) {
            loadPosts();
        }
    }

    private void loadPosts() {
        ArrayList<Post> posts = new ArrayList<>();
        Iterator<Post> it = PostDAO.getInstance().getAll();
        while (it.hasNext()) {
            Post post = it.next();
            if (!post.isDeleted()) posts.add(post);
        }

        RecyclerView recycler = findViewById(R.id.recyclerPosts);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new PostAdapter(this, posts,
                (position, post) -> openPost(post.id),
                tag -> openHashtagSearch(tag)));
    }

    private void showCreatePostDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.post_title_hint);
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(R.string.create_post)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) -> createPost(input.getText().toString()))
                .show();
    }

    private void createPost(String topic) {
        String cleanTopic = topic == null ? "" : topic.trim();
        if (cleanTopic.isEmpty()) {
            Toast.makeText(this, R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }

        Post post = new Post(UUID.randomUUID(), currentUser.getUUID(), cleanTopic);
        post.setHashtags(HashtagParser.extract(cleanTopic));
        PostDAO.getInstance().add(post);
        HashtagService.getInstance().indexPost(post);
        Toast.makeText(this, R.string.post_created, Toast.LENGTH_SHORT).show();
        loadPosts();
    }

    private void openHashtagSearch(String tag) {
        Intent intent = new Intent(this, HashtagSearchActivity.class);
        if (tag != null) intent.putExtra(HashtagSearchActivity.EXTRA_HASHTAG, tag);
        putCurrentUser(intent);
        startActivity(intent);
    }

    private int postIndex(UUID postId) {
        int index = 0;
        Iterator<Post> posts = PostDAO.getInstance().getAll();
        while (posts.hasNext()) {
            Post post = posts.next();
            if (post.id.equals(postId)) return post.isDeleted() ? -1 : index;
            index++;
        }
        return -1;
    }

    private void openPost(UUID postId) {
        int index = postIndex(postId);
        if (index < 0) {
            Toast.makeText(this, R.string.post_deleted_unavailable, Toast.LENGTH_SHORT).show();
            loadPosts();
            return;
        }

        Intent intent = new Intent(MainActivity.this, PostViewerActivity.class);
        intent.putExtra("post_index", index);
        putCurrentUser(intent);
        startActivity(intent);
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
