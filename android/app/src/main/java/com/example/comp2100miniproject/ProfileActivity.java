package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.PostAdapter;
import com.example.comp2100miniproject.src.ProfileReplyAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.RandomContentGenerator;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;

public class ProfileActivity extends AppCompatActivity {
    private static final int PAGE_SIZE = 3;

    private AuthManager authManager;
    private User currentUser;
    private EditText inputDisplayName;
    private EditText inputNewPassword;
    private TextView textUsername;
    private TextView textNoMyPosts;
    private TextView textNoMyReplies;
    private TextView textPostsPage;
    private TextView textRepliesPage;
    private RecyclerView recyclerMyPosts;
    private RecyclerView recyclerMyReplies;
    private Button buttonPrevPosts;
    private Button buttonNextPosts;
    private Button buttonPrevReplies;
    private Button buttonNextReplies;
    private final ArrayList<Post> myPosts = new ArrayList<>();
    private final ArrayList<Message> myReplies = new ArrayList<>();
    private int postsPage = 0;
    private int repliesPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        authManager = new AuthManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null) {
            openLogin();
            return;
        }

        if (!PostDAO.getInstance().getAll().hasNext()) {
            RandomContentGenerator.populateRandomData();
        }

        bindViews();
        recyclerMyPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerMyReplies.setLayoutManager(new LinearLayoutManager(this));
        collectUserContent();
        renderProfile();
        renderContentPages();

        findViewById(R.id.buttonSaveProfile).setOnClickListener(v -> saveProfile());
        buttonPrevPosts.setOnClickListener(v -> {
            postsPage--;
            renderPostsPage();
        });
        buttonNextPosts.setOnClickListener(v -> {
            postsPage++;
            renderPostsPage();
        });
        buttonPrevReplies.setOnClickListener(v -> {
            repliesPage--;
            renderRepliesPage();
        });
        buttonNextReplies.setOnClickListener(v -> {
            repliesPage++;
            renderRepliesPage();
        });

        Button navFeed = findViewById(R.id.navFeed);
        Button navProfile = findViewById(R.id.navProfile);
        Button navLogout = findViewById(R.id.navLogout);
        navProfile.setEnabled(false);
        navFeed.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            putCurrentUser(intent);
            startActivity(intent);
            finish();
        });
        navLogout.setOnClickListener(v -> openLogin());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authManager != null && currentUser != null) {
            refreshContent();
        }
    }

    private void bindViews() {
        textUsername = findViewById(R.id.textUsername);
        inputDisplayName = findViewById(R.id.inputDisplayName);
        inputNewPassword = findViewById(R.id.inputNewPassword);
        textNoMyPosts = findViewById(R.id.textNoMyPosts);
        textNoMyReplies = findViewById(R.id.textNoMyReplies);
        textPostsPage = findViewById(R.id.textPostsPage);
        textRepliesPage = findViewById(R.id.textRepliesPage);
        recyclerMyPosts = findViewById(R.id.recyclerMyPosts);
        recyclerMyReplies = findViewById(R.id.recyclerMyReplies);
        buttonPrevPosts = findViewById(R.id.buttonPrevPosts);
        buttonNextPosts = findViewById(R.id.buttonNextPosts);
        buttonPrevReplies = findViewById(R.id.buttonPrevReplies);
        buttonNextReplies = findViewById(R.id.buttonNextReplies);
    }

    private void renderProfile() {
        textUsername.setText(getString(R.string.username_value, currentUser.username()));
        inputDisplayName.setText(authManager.getDisplayName(currentUser));
    }

    private void collectUserContent() {
        myPosts.clear();
        myReplies.clear();

        Iterator<Post> posts = PostDAO.getInstance().getAll();
        while (posts.hasNext()) {
            Post post = posts.next();
            if (!post.isDeleted() && currentUser.getUUID().equals(post.poster)) {
                myPosts.add(post);
            }
        }

        Iterator<Message> messages = PostDAO.getInstance().getAllMessages();
        while (messages.hasNext()) {
            Message message = messages.next();
            if (!message.isDeleted() && currentUser.getUUID().equals(message.poster()) && isPostVisible(message.thread())) {
                myReplies.add(message);
            }
        }
    }

    private void renderContentPages() {
        postsPage = clampPage(postsPage, myPosts.size());
        repliesPage = clampPage(repliesPage, myReplies.size());
        renderPostsPage();
        renderRepliesPage();
    }

    private void renderPostsPage() {
        if (myPosts.isEmpty()) {
            textNoMyPosts.setVisibility(View.VISIBLE);
            recyclerMyPosts.setVisibility(View.GONE);
            recyclerMyPosts.setAdapter(null);
            textPostsPage.setText(getString(R.string.page_format, 0, 0));
            buttonPrevPosts.setEnabled(false);
            buttonNextPosts.setEnabled(false);
            return;
        }

        postsPage = clampPage(postsPage, myPosts.size());
        textNoMyPosts.setVisibility(View.GONE);
        recyclerMyPosts.setVisibility(View.VISIBLE);
        recyclerMyPosts.setAdapter(new PostAdapter(this, pagePosts(), (position, post) -> openPost(post.id), null));
        updatePager(postsPage, myPosts.size(), textPostsPage, buttonPrevPosts, buttonNextPosts);
    }

    private void renderRepliesPage() {
        if (myReplies.isEmpty()) {
            textNoMyReplies.setVisibility(View.VISIBLE);
            recyclerMyReplies.setVisibility(View.GONE);
            recyclerMyReplies.setAdapter(null);
            textRepliesPage.setText(getString(R.string.page_format, 0, 0));
            buttonPrevReplies.setEnabled(false);
            buttonNextReplies.setEnabled(false);
            return;
        }

        repliesPage = clampPage(repliesPage, myReplies.size());
        textNoMyReplies.setVisibility(View.GONE);
        recyclerMyReplies.setVisibility(View.VISIBLE);
        recyclerMyReplies.setAdapter(new ProfileReplyAdapter(pageReplies(), reply -> openPost(reply.thread())));
        updatePager(repliesPage, myReplies.size(), textRepliesPage, buttonPrevReplies, buttonNextReplies);
    }

    private List<Post> pagePosts() {
        int start = postsPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, myPosts.size());
        return new ArrayList<>(myPosts.subList(start, end));
    }

    private List<Message> pageReplies() {
        int start = repliesPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, myReplies.size());
        return new ArrayList<>(myReplies.subList(start, end));
    }

    private void updatePager(int page, int itemCount, TextView label, Button prev, Button next) {
        int totalPages = totalPages(itemCount);
        label.setText(getString(R.string.page_format, page + 1, totalPages));
        prev.setEnabled(page > 0);
        next.setEnabled(page + 1 < totalPages);
    }

    private int clampPage(int page, int itemCount) {
        int totalPages = totalPages(itemCount);
        if (totalPages == 0) return 0;
        return Math.max(0, Math.min(page, totalPages - 1));
    }

    private int totalPages(int itemCount) {
        if (itemCount == 0) return 0;
        return (itemCount + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private void saveProfile() {
        boolean saved = authManager.updateProfile(
                currentUser.getUUID(),
                inputDisplayName.getText().toString(),
                inputNewPassword.getText().toString()
        );
        if (!saved) {
            Toast.makeText(this, R.string.profile_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser = authManager.getUser(currentUser.getUUID());
        inputNewPassword.setText("");
        renderProfile();
        Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
    }

    private void openPost(UUID postId) {
        int index = postIndex(postId);
        if (index < 0) {
            Toast.makeText(this, R.string.post_deleted_unavailable, Toast.LENGTH_SHORT).show();
            refreshContent();
            return;
        }

        Intent intent = new Intent(ProfileActivity.this, PostViewerActivity.class);
        intent.putExtra("post_index", index);
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

    private void refreshContent() {
        collectUserContent();
        renderContentPages();
    }

    private boolean isPostVisible(UUID postId) {
        Iterator<Post> posts = PostDAO.getInstance().getAll();
        while (posts.hasNext()) {
            Post post = posts.next();
            if (post.id.equals(postId)) return !post.isDeleted();
        }
        return false;
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
