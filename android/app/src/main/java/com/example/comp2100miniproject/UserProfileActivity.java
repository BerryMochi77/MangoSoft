package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.src.PostAdapter;
import com.example.comp2100miniproject.src.ProfileReplyAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import messagestate.MessageDeletionRegistry;

/** Read-only public profile shown when a user taps another user's avatar. */
public class UserProfileActivity extends AppCompatActivity {
    public static final String EXTRA_PROFILE_USER_ID = "profile_user_id";

    private AuthManager authManager;
    private AvatarManager avatarManager;
    private ProfileBackgroundManager profileBackgroundManager;
    private User currentUser;
    private User profileUser;

    private ImageView imageAvatar;
    private ImageView imageProfileBackground;
    private TextView textProfileTitle;
    private TextView textUsername;
    private TextView textNoUserPosts;
    private TextView textNoUserReplies;
    private RecyclerView recyclerUserPosts;
    private RecyclerView recyclerUserReplies;

    private final ArrayList<Post> userPosts = new ArrayList<>();
    private final ArrayList<Message> userReplies = new ArrayList<>();
    private boolean postsPublic = true;
    private boolean repliesPublic = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        authManager = new AuthManager(this);
        avatarManager = new AvatarManager(authManager);
        profileBackgroundManager = new ProfileBackgroundManager(authManager);

        currentUser = authManager.getUser(readUuidExtra(AuthManager.EXTRA_USER_ID));
        profileUser = authManager.getUser(readUuidExtra(EXTRA_PROFILE_USER_ID));
        if (profileUser == null) {
            Toast.makeText(this, R.string.profile_user_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imageAvatar = findViewById(R.id.imageAvatar);
        imageProfileBackground = findViewById(R.id.imageProfileBackground);
        textProfileTitle = findViewById(R.id.textProfileTitle);
        textUsername = findViewById(R.id.textUsername);
        textNoUserPosts = findViewById(R.id.textNoUserPosts);
        textNoUserReplies = findViewById(R.id.textNoUserReplies);
        recyclerUserPosts = findViewById(R.id.recyclerUserPosts);
        recyclerUserReplies = findViewById(R.id.recyclerUserReplies);

        recyclerUserPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerUserReplies.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        renderProfile();
        refreshContent();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profileUser != null) {
            refreshContent();
        }
    }

    private void renderProfile() {
        textProfileTitle.setText(authManager.getDisplayName(profileUser));
        textUsername.setText(getString(R.string.username_value, profileUser.username()));
        avatarManager.displayAvatar(profileUser, imageAvatar);
        profileBackgroundManager.displayBackground(profileUser, imageProfileBackground);
    }

    private void refreshContent() {
        collectUserContent();
        renderPosts();
        renderReplies();
    }

    private void collectUserContent() {
        userPosts.clear();
        userReplies.clear();
        AuthManager.ProfileVisibility visibility = authManager.getProfileVisibility(profileUser);
        boolean ownerViewing = currentUser != null && currentUser.getUUID().equals(profileUser.getUUID());
        postsPublic = ownerViewing || visibility.publicPosts();
        repliesPublic = ownerViewing || visibility.publicReplies();

        if (postsPublic) {
            Iterator<Post> posts = PostDAO.getInstance().getAll();
            while (posts.hasNext()) {
                Post post = posts.next();
                if (!post.isDeleted() && profileUser.getUUID().equals(post.poster)) {
                    userPosts.add(post);
                }
            }
        }

        if (repliesPublic) {
            MessageDeletionRegistry deletions = MessageDeletionRegistry.getInstance();
            Iterator<Message> messages = PostDAO.getInstance().getAllMessages();
            while (messages.hasNext()) {
                Message message = messages.next();
                if (!deletions.isDeleted(message.id())
                        && profileUser.getUUID().equals(message.poster())
                        && isPostVisible(message.thread())) {
                    userReplies.add(message);
                }
            }
        }
    }

    private void renderPosts() {
        if (!postsPublic) {
            textNoUserPosts.setText(R.string.user_posts_private);
            textNoUserPosts.setVisibility(View.VISIBLE);
            recyclerUserPosts.setVisibility(View.GONE);
            recyclerUserPosts.setAdapter(null);
            return;
        }

        boolean empty = userPosts.isEmpty();
        textNoUserPosts.setText(R.string.no_user_posts);
        textNoUserPosts.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerUserPosts.setVisibility(empty ? View.GONE : View.VISIBLE);
        recyclerUserPosts.setAdapter(empty
                ? null
                : new PostAdapter(
                        this,
                        new ArrayList<>(userPosts),
                        (position, post) -> openPost(post.id),
                        null,
                        this::openUserProfile
                ));
    }

    private void renderReplies() {
        if (!repliesPublic) {
            textNoUserReplies.setText(R.string.user_replies_private);
            textNoUserReplies.setVisibility(View.VISIBLE);
            recyclerUserReplies.setVisibility(View.GONE);
            recyclerUserReplies.setAdapter(null);
            return;
        }

        boolean empty = userReplies.isEmpty();
        textNoUserReplies.setText(R.string.no_user_replies);
        textNoUserReplies.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerUserReplies.setVisibility(empty ? View.GONE : View.VISIBLE);
        recyclerUserReplies.setAdapter(empty
                ? null
                : new ProfileReplyAdapter(this, new ArrayList<>(userReplies), reply -> openPost(reply.thread())));
    }

    private void openUserProfile(User user) {
        if (user == null || user.getUUID().equals(profileUser.getUUID())) {
            return;
        }
        Intent intent = new Intent(this, UserProfileActivity.class);
        putCurrentUser(intent);
        intent.putExtra(EXTRA_PROFILE_USER_ID, user.getUUID().toString());
        startActivity(intent);
    }

    private void openPost(UUID postId) {
        int index = postIndex(postId);
        if (index < 0) {
            Toast.makeText(this, R.string.post_deleted_unavailable, Toast.LENGTH_SHORT).show();
            refreshContent();
            return;
        }

        Intent intent = new Intent(this, PostViewerActivity.class);
        intent.putExtra("post_index", index);
        putCurrentUser(intent);
        startActivity(intent);
    }

    private void putCurrentUser(Intent intent) {
        User user = currentUser == null ? profileUser : currentUser;
        intent.putExtra(AuthManager.EXTRA_USER_ID, user.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, user.role() == User.Role.Admin);
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

    private boolean isPostVisible(UUID postId) {
        Iterator<Post> posts = PostDAO.getInstance().getAll();
        while (posts.hasNext()) {
            Post post = posts.next();
            if (post.id.equals(postId)) return !post.isDeleted();
        }
        return false;
    }

    private UUID readUuidExtra(String key) {
        String value = getIntent().getStringExtra(key);
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
