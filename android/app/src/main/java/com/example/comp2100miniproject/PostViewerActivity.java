package com.example.comp2100miniproject;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.example.comp2100miniproject.moderation.FrozenUserManager;
import com.example.comp2100miniproject.src.MessageAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import dao.PostDAO;
import dao.RandomContentGenerator;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;
import hashtag.HashtagService;
import moderation.ModerationTools;

public class PostViewerActivity extends AppCompatActivity {
    private AuthManager authManager;
    private AvatarManager avatarManager;
    private FrozenUserManager frozenUserManager;
    private User currentUser;
    private Post post;
    private TextView textPostTitle;
    private TextView textPostAuthor;
    private TextView textPostEdited;
    private TextView textPostBody;
    private ImageView imagePostAuthorAvatar;
    private RecyclerView recyclerMessages;
    private EditText inputReply;

    private Button buttonLike;
    private Button buttonHeart;
    private Button buttonLaugh;
    private Button buttonAngry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_viewer);

        authManager = new AuthManager(this);
        avatarManager = new AvatarManager(authManager);
        frozenUserManager = new FrozenUserManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null) {
            finish();
            return;
        }

        if (!PostDAO.getInstance().getAll().hasNext()) {
            RandomContentGenerator.populateRandomData();
        }

        int postIndex = getIntent().getIntExtra("post_index", 0);
        post = PostDAO.getInstance().getAtIndex(postIndex);
        if (post == null || post.isDeleted()) {
            Toast.makeText(this, R.string.post_deleted_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textPostTitle = findViewById(R.id.textPostTitle);
        textPostAuthor = findViewById(R.id.textPostAuthor);
        textPostEdited = findViewById(R.id.textPostEdited);
        textPostBody = findViewById(R.id.textPostBody);
        imagePostAuthorAvatar = findViewById(R.id.imagePostAuthorAvatar);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        inputReply = findViewById(R.id.inputReply);

        buttonLike = findViewById(R.id.buttonLike);
        buttonHeart = findViewById(R.id.buttonHeart);
        buttonLaugh = findViewById(R.id.buttonLaugh);
        buttonAngry = findViewById(R.id.buttonAngry);

        setupReactionButtons();

        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        Button buttonSendReply = findViewById(R.id.buttonSendReply);
        buttonSendReply.setOnClickListener(v -> addReply());

        LinearLayout postOwnerActions = findViewById(R.id.postOwnerActions);
        boolean ownsPost = currentUser.getUUID().equals(post.poster);
        postOwnerActions.setVisibility(ownsPost ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonEditPost).setOnClickListener(v -> showEditPostDialog());
        findViewById(R.id.buttonDeletePost).setOnClickListener(v -> confirmDeletePost());

        renderPost();
        loadMessages();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupReactionButtons() {
        ReactionManager manager = ReactionManager.getInstance();

        updateReactionButtons();

        buttonLike.setOnClickListener(v -> {
            manager.addReaction(post.getUUID(), ReactionType.LIKE);
            updateReactionButtons();
        });

        buttonHeart.setOnClickListener(v -> {
            manager.addReaction(post.getUUID(), ReactionType.HEART);
            updateReactionButtons();
        });

        buttonLaugh.setOnClickListener(v -> {
            manager.addReaction(post.getUUID(), ReactionType.LAUGH);
            updateReactionButtons();
        });

        buttonAngry.setOnClickListener(v -> {
            manager.addReaction(post.getUUID(), ReactionType.ANGRY);
            updateReactionButtons();
        });
    }

    private void updateReactionButtons() {
        ReactionData data = ReactionManager.getInstance().getReactionData(post.getUUID());

        buttonLike.setText("👍 " + data.getLikes());
        buttonHeart.setText("❤️ " + data.getHearts());
        buttonLaugh.setText("😂 " + data.getLaughs());
        buttonAngry.setText("😡 " + data.getAngries());
    }

    private void renderPost() {
        textPostTitle.setText(post.topic);
        User poster = UserDAO.getInstance().getByUUID(post.poster);
        if (poster != null) {
            avatarManager.displayAvatar(poster, imagePostAuthorAvatar);
        } else {
            imagePostAuthorAvatar.setImageResource(R.drawable.avatar_default_1);
        }
        textPostAuthor.setText("Posted by " + authorName(poster));
        textPostEdited.setVisibility(post.isEdited() ? View.VISIBLE : View.GONE);
        String body = post.getBody();
        if (body.isEmpty()) {
            textPostBody.setVisibility(View.GONE);
        } else {
            textPostBody.setVisibility(View.VISIBLE);
            textPostBody.setText(body);
        }
    }

    private void loadMessages() {
        ArrayList<Message> messages = new ArrayList<>();
        Iterator<Message> it = post.getVisibleMessages(currentUser.role() == User.Role.Admin).getAll();
        while (it.hasNext()) {
            messages.add(it.next());
        }

        recyclerMessages.setAdapter(new MessageAdapter(
                this,
                messages,
                currentUser.getUUID(),
                this::showReportDialog,
                this::showEditReplyDialog,
                this::confirmDeleteReply
        ));
    }

    private void addReply() {
        String content = inputReply.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }

        post.messages.insert(new Message(
                UUID.randomUUID(),
                currentUser.getUUID(),
                post.getUUID(),
                System.currentTimeMillis(),
                content
        ));
        inputReply.setText("");
        Toast.makeText(this, R.string.reply_sent, Toast.LENGTH_SHORT).show();
        loadMessages();
    }

    private void showEditPostDialog() {
        EditText input = new EditText(this);
        input.setText(post.topic);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_post)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) -> updatePost(input.getText().toString()))
                .show();
    }

    private void updatePost(String topic) {
        String cleanTopic = topic == null ? "" : topic.trim();
        if (cleanTopic.isEmpty()) {
            Toast.makeText(this, R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }

        HashtagService.getInstance().removePost(post);
        post.topic = cleanTopic;
        // Re-extract hashtags from title + existing body when editing.
        post.setHashtags(HashtagParser.extract(cleanTopic + " " + post.getBody()));
        post.setEdited(true);
        HashtagService.getInstance().indexPost(post);
        renderPost();
        Toast.makeText(this, R.string.post_updated, Toast.LENGTH_SHORT).show();
    }

    private void confirmDeletePost() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_post_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    post.setDeleted(true);
                    Toast.makeText(this, R.string.post_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }

    private void showEditReplyDialog(Message message) {
        EditText input = new EditText(this);
        input.setText(message.message());
        input.setSelection(input.getText().length());
        input.setMinLines(3);

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_reply)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) -> updateReply(message, input.getText().toString()))
                .show();
    }

    private void updateReply(Message message, String content) {
        String cleanContent = content == null ? "" : content.trim();
        if (cleanContent.isEmpty()) {
            Toast.makeText(this, R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }

        message.setMessage(cleanContent);
        message.setEdited(true);
        Toast.makeText(this, R.string.reply_updated, Toast.LENGTH_SHORT).show();
        loadMessages();
    }

    private void confirmDeleteReply(Message message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_reply_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    message.setDeleted(true);
                    Toast.makeText(this, R.string.reply_deleted, Toast.LENGTH_SHORT).show();
                    loadMessages();
                })
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

    private String authorName(User user) {
        return user == null ? "Unknown author" : authManager.getDisplayName(user);
    }

    private void showReportDialog(Message message) {
        if (frozenUserManager.isFrozen(currentUser.getUUID())) {
            Toast.makeText(this, R.string.report_frozen, Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint(R.string.report_reason_hint);
        input.setMinLines(3);

        new AlertDialog.Builder(this)
                .setTitle(R.string.report_reason_title)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) -> submitReport(message, input.getText().toString()))
                .show();
    }

    private void submitReport(Message message, String reason) {
        boolean reported = ModerationTools.addReport(
                message.id(),
                currentUser.getUUID(),
                System.currentTimeMillis(),
                reason == null ? "" : reason.trim()
        );
        int messageId = reported ? R.string.report_sent : R.string.report_failed;
        Toast.makeText(PostViewerActivity.this, messageId, Toast.LENGTH_SHORT).show();
    }
}
