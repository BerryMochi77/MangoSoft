package com.example.comp2100miniproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.comp2100miniproject.src.ThreadConnectorDecoration;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import messagestate.MessageDeletionRegistry;
import messagestate.MessageEditRegistry;
import messagestate.MessageThreadRegistry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
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
import postview.PostViewService;

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
    private TextView textViewCount;
    private ImageView imagePostAttachment;
    private ChipGroup chipGroupPostHashtags;
    private ImageView imagePostAuthorAvatar;
    private RecyclerView recyclerMessages;
    private EditText inputReply;
    private EditText activeComposerInput;
    private ActivityResultLauncher<PickVisualMediaRequest> composerImageLauncher;
    private static final String[] QUICK_REACTION_EMOJIS = {
            "\uD83D\uDE00",
            "\uD83E\uDD73",
            "\uD83D\uDE05",
            "\uD83D\uDE22",
            "\uD83D\uDE2E",
            "\uD83D\uDE4C",
            "\uD83D\uDD25",
            "\uD83C\uDF89"
    };

    /**
     * Threaded (depth-first) list of currently-visible messages. Updated on
     * every {@link #loadMessages()}. Used both as the adapter's data source
     * and as the lookup for {@link ThreadConnectorDecoration}.
     */
    private ArrayList<Message> threadedMessages = new ArrayList<>();

    private ChipGroup reactionChipGroup;
    private ChipGroup emojiReactionTray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        composerImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                this::insertSelectedImage
        );
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
        textViewCount = findViewById(R.id.textViewCount);
        imagePostAttachment = findViewById(R.id.imagePostAttachment);
        chipGroupPostHashtags = findViewById(R.id.chipGroupPostHashtags);
        imagePostAuthorAvatar = findViewById(R.id.imagePostAuthorAvatar);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        inputReply = findViewById(R.id.inputReply);

        reactionChipGroup = findViewById(R.id.reactionChipGroup);
        emojiReactionTray = findViewById(R.id.emojiReactionTray);

        setupReactionButtons();

        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        // ItemDecoration paints the parent-to-child L connectors. It reads
        // the current threaded list via a method reference so it stays in
        // sync as loadMessages() rebuilds the list after edits/replies.
        recyclerMessages.addItemDecoration(
                new ThreadConnectorDecoration(this, this::messageIdAt));

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        Button buttonSendReply = findViewById(R.id.buttonSendReply);
        buttonSendReply.setOnClickListener(v -> addReply());
        ImageButton buttonReplyMore = findViewById(R.id.buttonReplyMore);
        buttonReplyMore.setOnClickListener(v -> showComposerMenu(inputReply));

        LinearLayout postOwnerActions = findViewById(R.id.postOwnerActions);
        boolean ownsPost = currentUser.getUUID().equals(post.poster);
        postOwnerActions.setVisibility(ownsPost ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonEditPost).setOnClickListener(v -> showEditPostDialog());
        findViewById(R.id.buttonDeletePost).setOnClickListener(v -> confirmDeletePost());

        // Record one view per fresh open. savedInstanceState != null means the OS
        // recreated the activity, so do not count that again.
        if (savedInstanceState == null) {
            PostViewService.getInstance().recordView(post.id);
        }

        renderPost();
        loadMessages();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupReactionButtons() {
        setupEmojiReactionTray();
        updateReactionButtons();
    }

    private void updateReactionButtons() {
        ReactionManager manager = ReactionManager.getInstance();
        Set<String> options = manager.getReactionOptions(post.getUUID());
        String selected = manager.getUserReaction(post.getUUID(), currentUser.getUUID());
        reactionChipGroup.removeAllViews();

        for (String emoji : options) {
            Chip chip = reactionChip(emoji + " " + manager.getReactionCount(post.getUUID(), emoji));
            chip.setChecked(emoji.equals(selected));
            chip.setAlpha(emoji.equals(selected) ? 1f : 0.82f);
            chip.setOnClickListener(v -> {
                manager.toggleReaction(post.getUUID(), currentUser.getUUID(), emoji);
                updateReactionButtons();
            });
            reactionChipGroup.addView(chip);
        }

        Chip addChip = reactionChip("+");
        addChip.setContentDescription(getString(R.string.custom_reaction_title));
        addChip.setOnClickListener(v -> toggleEmojiReactionTray());
        reactionChipGroup.addView(addChip);
    }

    private Chip reactionChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setMinHeight((int) (36 * getResources().getDisplayMetrics().density));
        chip.setChipBackgroundColorResource(R.color.chip_hashtag_bg);
        chip.setTextColor(getColor(R.color.text_primary));
        chip.setTextSize(14f);
        return chip;
    }

    private void setupEmojiReactionTray() {
        emojiReactionTray.removeAllViews();
        for (String emoji : QUICK_REACTION_EMOJIS) {
            Chip chip = reactionChip(emoji);
            chip.setOnClickListener(v -> {
                ReactionManager.getInstance().toggleReaction(
                        post.getUUID(), currentUser.getUUID(), emoji);
                emojiReactionTray.setVisibility(View.GONE);
                updateReactionButtons();
            });
            emojiReactionTray.addView(chip);
        }
    }

    private void toggleEmojiReactionTray() {
        int nextVisibility = emojiReactionTray.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        emojiReactionTray.setVisibility(nextVisibility);
    }

    private void renderPost() {
        // Strip #tags out of the title - they render as chips below.
        textPostTitle.setText(HashtagParser.stripTags(post.topic));
        User poster = UserDAO.getInstance().getByUUID(post.poster);
        if (poster != null) {
            avatarManager.displayAvatar(poster, imagePostAuthorAvatar);
        } else {
            imagePostAuthorAvatar.setImageResource(R.drawable.avatar_default_1);
        }
        textPostAuthor.setText("Posted by " + authorName(poster));
        textPostEdited.setVisibility(post.isEdited() ? View.VISIBLE : View.GONE);
        // View count is delegated entirely to PostViewService.
        int views = PostViewService.getInstance().getViewCount(post.id);
        textViewCount.setText(getString(R.string.view_count, views));
        renderHashtagChips();
        String body = post.getBody();
        ComposerFormatManager.bindContent(body, textPostBody, imagePostAttachment);
    }

    private void renderHashtagChips() {
        chipGroupPostHashtags.removeAllViews();
        java.util.List<String> tags = post.getHashtags();
        if (tags == null || tags.isEmpty()) {
            chipGroupPostHashtags.setVisibility(View.GONE);
            return;
        }
        chipGroupPostHashtags.setVisibility(View.VISIBLE);
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText("#" + tag);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setChipBackgroundColorResource(R.color.chip_hashtag_bg);
            chip.setTextColor(getColor(R.color.accent));
            chip.setTextSize(12f);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setOnClickListener(v -> openTrendsForTag(tag));
            chipGroupPostHashtags.addView(chip);
        }
    }

    /**
     * Tapping a hashtag chip in a post detail returns to MainActivity and
     * switches it to the Trends tab filtered by {@code tag}. CLEAR_TOP +
     * SINGLE_TOP pops PostViewerActivity (and anything above MainActivity)
     * and delivers the intent via onNewIntent rather than recreating
     * MainActivity, so the rest of the tab state is preserved.
     */
    private void openTrendsForTag(String tag) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_TRENDS_TAG, tag);
        intent.putExtra(AuthManager.EXTRA_USER_ID, currentUser.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, currentUser.role() == User.Role.Admin);
        startActivity(intent);
        finish();
    }

    private void loadMessages() {
        ArrayList<Message> timeSorted = new ArrayList<>();
        Iterator<Message> it = post.getVisibleMessages(currentUser.role() == User.Role.Admin).getAll();
        while (it.hasNext()) {
            timeSorted.add(it.next());
        }

        // Reorder time-sorted messages into Reddit-style depth-first order so
        // every reply sits directly under its parent. MessageAdapter picks
        // depth back out of MessageThreadRegistry when computing indent, and
        // ThreadConnectorDecoration uses threadedMessages to find each
        // child's parent View when painting the L connector.
        threadedMessages = new ArrayList<>(
                MessageThreadRegistry.getInstance().flatten(timeSorted, Message::id));

        recyclerMessages.setAdapter(new MessageAdapter(
                this,
                threadedMessages,
                currentUser.getUUID(),
                this::showReportDialog,
                this::showEditReplyDialog,
                this::confirmDeleteReply,
                this::showReplyDialog
        ));
    }

    /** Lookup used by {@link ThreadConnectorDecoration}. */
    private UUID messageIdAt(int position) {
        if (position < 0 || position >= threadedMessages.size()) return null;
        return threadedMessages.get(position).id();
    }

    private void addReply() {
        addReplyMessage(inputReply.getText().toString(), null);
        inputReply.setText("");
    }

    /**
     * Insert a reply. If {@code parentMessageId} is non-null, record the
     * parent/child relationship in {@link MessageThreadRegistry} so the
     * new message renders indented beneath the message it replies to.
     */
    private void addReplyMessage(String rawContent, java.util.UUID parentMessageId) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }

        UUID newId = UUID.randomUUID();
        post.messages.insert(new Message(
                newId,
                currentUser.getUUID(),
                post.getUUID(),
                System.currentTimeMillis(),
                content
        ));
        if (parentMessageId != null) {
            MessageThreadRegistry.getInstance().setParent(newId, parentMessageId);
        }
        Toast.makeText(this, R.string.reply_sent, Toast.LENGTH_SHORT).show();
        loadMessages();
    }

    private void showReplyDialog(Message parent) {
        EditText input = new EditText(this);
        input.setHint(R.string.write_reply_hint);
        input.setMinLines(3);

        int dp8 = (int) (8 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageButton moreButton = composerMoreButton();
        LinearLayout toolRow = new LinearLayout(this);
        toolRow.setGravity(android.view.Gravity.END);
        toolRow.setPadding(0, dp8, 0, 0);
        toolRow.addView(moreButton);
        container.addView(toolRow);
        moreButton.setOnClickListener(v -> showComposerMenu(input));

        User author = UserDAO.getInstance().getByUUID(parent.poster());
        String authorName = author == null ? "Unknown user" : authManager.getDisplayName(author);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reply_to, authorName))
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.send, (dialog, which) ->
                        addReplyMessage(input.getText().toString(), parent.id()))
                .show();
    }

    private ImageButton composerMoreButton() {
        ImageButton button = new ImageButton(this);
        int size = (int) (44 * getResources().getDisplayMetrics().density);
        button.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        button.setBackgroundResource(R.drawable.bg_fab_circle);
        button.setImageResource(R.drawable.ic_add_format);
        button.setContentDescription(getString(R.string.more_composer_options));
        button.setPadding(10, 10, 10, 10);
        return button;
    }

    private void showComposerMenu(EditText input) {
        activeComposerInput = input;
        String[] options = {
                getString(R.string.add_image),
                getString(R.string.add_emoji)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.more_composer_options)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        chooseComposerImage();
                    } else if (which == 1) {
                        showEmojiChooser(input);
                    }
                })
                .show();
    }

    private void chooseComposerImage() {
        composerImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void insertSelectedImage(Uri uri) {
        if (activeComposerInput == null || uri == null) return;

        Uri copied = ComposerFormatManager.copyImage(this, uri);
        if (copied == null) {
            Toast.makeText(this, R.string.image_attach_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        ComposerFormatManager.insertImage(activeComposerInput, copied);
        Toast.makeText(this, R.string.image_attached, Toast.LENGTH_SHORT).show();
    }

    private void showEmojiChooser(EditText input) {
        String[] emojis = {
                "\uD83D\uDE42",
                "\uD83D\uDE02",
                "\uD83D\uDE0D",
                "\uD83D\uDC4D",
                "\uD83D\uDD25",
                "\uD83C\uDF80"
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_emoji)
                .setItems(emojis, (dialog, which) ->
                        ComposerFormatManager.insertEmoji(input, emojis[which]))
                .show();
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
        // Show the latest edited content if any, otherwise the original.
        input.setText(MessageEditRegistry.getInstance()
                .currentContent(message.id(), message.message()));
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

        // Per-message state lives in sidecars, not on Message itself.
        MessageEditRegistry.getInstance().recordEdit(message.id(), cleanContent);
        Toast.makeText(this, R.string.reply_updated, Toast.LENGTH_SHORT).show();
        loadMessages();
    }

    private void confirmDeleteReply(Message message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_reply_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    MessageDeletionRegistry.getInstance().markDeleted(message.id());
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
