package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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

import messagestate.MessageBookmarkRegistry;
import messagestate.MessageDeletionRegistry;
import messagestate.MessageEditRegistry;
import messagestate.MessageReactionRegistry;
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
    /** Held so reaction taps can refresh exactly one row instead of the whole adapter. */
    private MessageAdapter messageAdapter;
    private EditText inputReply;
    private EditText activeComposerInput;
    private UUID activeReplyParentId;
    private ActivityResultLauncher<PickVisualMediaRequest> composerImageLauncher;

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
        RandomContentGenerator.repairSeededData();

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
        inputReply.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && inputReply.getText().toString().trim().isEmpty()) {
                clearReplyTarget();
            }
        });

        reactionChipGroup = findViewById(R.id.reactionChipGroup);
        emojiReactionTray = findViewById(R.id.emojiReactionTray);

        updateReactionButtons();

        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        // Disable the default DefaultItemAnimator change cross-fade. When
        // notifyItemChanged fires (e.g. after a like tap) the old and new
        // ViewHolders are drawn semi-transparently on top of each other,
        // which on TextViews containing emoji visibly thins / blurs the
        // text. Keep add / remove animations, drop change cross-fade.
        androidx.recyclerview.widget.RecyclerView.ItemAnimator animator =
                recyclerMessages.getItemAnimator();
        if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator)
                    .setSupportsChangeAnimations(false);
        }
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
        buttonReplyMore.setOnClickListener(v -> {
            activeComposerInput = inputReply;
            chooseComposerImage();
        });

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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && inputReply != null && inputReply.hasFocus()) {
            View replyBar = findViewById(R.id.replyBar);
            Rect replyBarBounds = new Rect();
            replyBar.getGlobalVisibleRect(replyBarBounds);
            if (!replyBarBounds.contains((int) event.getRawX(), (int) event.getRawY())) {
                inputReply.clearFocus();
                hideKeyboard();
                if (inputReply.getText().toString().trim().isEmpty()) {
                    clearReplyTarget();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void updateReactionButtons() {
        ReactionManager manager = ReactionManager.getInstance();
        Set<String> options = manager.getReactionOptions(post.getUUID());
        reactionChipGroup.removeAllViews();

        for (String emoji : options) {
            Chip chip = reactionChip(emoji + " " + manager.getReactionCount(post.getUUID(), emoji));
            boolean selected = manager.hasUserReaction(post.getUUID(), currentUser.getUUID(), emoji);
            chip.setChecked(selected);
            chip.setAlpha(selected ? 1f : 0.82f);
            styleReactionChip(chip, selected);
            chip.setOnClickListener(v -> {
                manager.toggleReaction(post.getUUID(), currentUser.getUUID(), emoji);
                updateReactionButtons();
            });
            reactionChipGroup.addView(chip);
        }

        Chip addChip = reactionChip("+");
        styleReactionChip(addChip, false);
        addChip.setContentDescription(getString(R.string.custom_reaction_title));
        addChip.setOnClickListener(v -> showCustomReactionDialog());
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

    private void styleReactionChip(Chip chip, boolean selected) {
        float density = getResources().getDisplayMetrics().density;
        chip.setChipStrokeColor(ColorStateList.valueOf(
                getColor(selected ? R.color.accent : R.color.border)));
        chip.setChipStrokeWidth((selected ? 2f : 1f) * density);
    }

    private void showCustomReactionDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.custom_reaction_hint);
        input.setSingleLine(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.custom_reaction_title)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .create();

        input.addTextChangedListener(new TextWatcher() {
            private boolean handled;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (handled) return;
                String emoji = s == null ? "" : s.toString().trim();
                if (!emoji.isEmpty()) {
                    handled = true;
                    addCustomReaction(emoji);
                    dialog.dismiss();
                }
            }
        });

        dialog.setOnShowListener(d -> {
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        dialog.show();
    }

    private void addCustomReaction(String emoji) {
        if (emoji.isEmpty()) {
            Toast.makeText(this, R.string.empty_content, Toast.LENGTH_SHORT).show();
            return;
        }
        ReactionManager.getInstance()
                .addUserReaction(post.getUUID(), currentUser.getUUID(), emoji);
        if (emojiReactionTray != null) {
            emojiReactionTray.setVisibility(View.GONE);
        }
        updateReactionButtons();
    }

    private void renderPost() {
        // Strip #tags out of the title - they render as chips below.
        textPostTitle.setText(HashtagParser.stripTags(post.topic));
        User poster = UserDAO.getInstance().getByUUID(post.poster);
        if (poster != null) {
            avatarManager.displayAvatar(poster, imagePostAuthorAvatar);
            imagePostAuthorAvatar.setOnClickListener(v -> openUserProfile(poster));
            textPostAuthor.setOnClickListener(v -> openUserProfile(poster));
            imagePostAuthorAvatar.setClickable(true);
            textPostAuthor.setClickable(true);
        } else {
            imagePostAuthorAvatar.setImageResource(R.drawable.avatar_default_1);
            imagePostAuthorAvatar.setOnClickListener(null);
            textPostAuthor.setOnClickListener(null);
            imagePostAuthorAvatar.setClickable(false);
            textPostAuthor.setClickable(false);
        }
        textPostAuthor.setText(
                getString(R.string.posted_by, authorName(poster))
                        + " - "
                        + PostEngagement.formatCreatedAt(post));
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

        messageAdapter = new MessageAdapter(
                this,
                threadedMessages,
                currentUser.getUUID(),
                this::startReplyToMessage,
                this::handleMessageReaction,
                this::showMessageOverflow,
                this::openUserProfile
        );
        recyclerMessages.setAdapter(messageAdapter);
    }

    /**
     * Toggle the current user's like / dislike on a message and refresh
     * just that row. The reaction itself lives in
     * {@link MessageReactionRegistry} — Message stays untouched.
     */
    private void handleMessageReaction(Message message,
                                       MessageReactionRegistry.Direction direction) {
        MessageReactionRegistry.getInstance()
                .toggle(message.id(), currentUser.getUUID(), direction);
        int index = indexOfThreaded(message.id());
        if (index >= 0 && messageAdapter != null) {
            messageAdapter.notifyItemChanged(index);
        }
    }

    /**
     * Open the ⋮ overflow menu for a message: Save / Unsave, Report (non-
     * owner only), Edit / Delete (owner only). Reuses the existing flows
     * that previously sat on inline buttons.
     */
    private void showMessageOverflow(Message message, View anchor) {
        boolean mine = currentUser.getUUID().equals(message.poster());
        boolean bookmarked = MessageBookmarkRegistry.getInstance()
                .isBookmarked(currentUser.getUUID(), message.id());

        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.message_overflow_menu);
        popup.getMenu().findItem(R.id.menu_save_message).setVisible(!bookmarked);
        popup.getMenu().findItem(R.id.menu_unsave_message).setVisible(bookmarked);
        popup.getMenu().findItem(R.id.menu_report_message).setVisible(!mine);
        popup.getMenu().findItem(R.id.menu_edit_message).setVisible(mine);
        popup.getMenu().findItem(R.id.menu_delete_message).setVisible(mine);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_save_message || id == R.id.menu_unsave_message) {
                boolean nowSaved = MessageBookmarkRegistry.getInstance()
                        .toggle(currentUser.getUUID(), message.id());
                Toast.makeText(this,
                        nowSaved ? R.string.message_saved : R.string.message_unsaved,
                        Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_report_message) {
                showReportDialog(message);
                return true;
            } else if (id == R.id.menu_edit_message) {
                showEditReplyDialog(message);
                return true;
            } else if (id == R.id.menu_delete_message) {
                confirmDeleteReply(message);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private int indexOfThreaded(UUID messageId) {
        for (int i = 0; i < threadedMessages.size(); i++) {
            if (threadedMessages.get(i).id().equals(messageId)) return i;
        }
        return -1;
    }

    /** Lookup used by {@link ThreadConnectorDecoration}. */
    private UUID messageIdAt(int position) {
        if (position < 0 || position >= threadedMessages.size()) return null;
        return threadedMessages.get(position).id();
    }

    private void addReply() {
        if (addReplyMessage(inputReply.getText().toString(), activeReplyParentId)) {
            inputReply.setText("");
            clearReplyTarget();
        }
    }

    /**
     * Insert a reply. If {@code parentMessageId} is non-null, record the
     * parent/child relationship in {@link MessageThreadRegistry} so the
     * new message renders indented beneath the message it replies to.
     */
    private boolean addReplyMessage(String rawContent, java.util.UUID parentMessageId) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.empty_content, Toast.LENGTH_SHORT).show();
            return false;
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
        return true;
    }

    private void startReplyToMessage(Message parent) {
        activeReplyParentId = parent.id();
        User author = UserDAO.getInstance().getByUUID(parent.poster());
        String authorName = author == null ? "Unknown user" : authManager.getDisplayName(author);
        inputReply.setHint(getString(R.string.replying_to_hint, authorName));
        inputReply.requestFocus();
    }

    private void clearReplyTarget() {
        activeReplyParentId = null;
        inputReply.setHint(R.string.write_reply);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(inputReply.getWindowToken(), 0);
        }
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

    private void openUserProfile(User user) {
        if (user == null || currentUser.getUUID().equals(user.getUUID())) {
            return;
        }

        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_PROFILE_USER_ID, user.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_USER_ID, currentUser.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, currentUser.role() == User.Role.Admin);
        startActivity(intent);
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
