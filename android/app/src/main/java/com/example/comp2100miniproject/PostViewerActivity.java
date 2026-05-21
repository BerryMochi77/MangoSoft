package com.example.comp2100miniproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.moderation.FrozenUserManager;
import com.example.comp2100miniproject.src.MessageAdapter;
import com.example.comp2100miniproject.src.ThreadConnectorDecoration;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import messagestate.MessageBookmarkRegistry;
import messagestate.MessageDeletionRegistry;
import messagestate.MessageEditRegistry;
import messagestate.MessageReactionRegistry;
import messagestate.MessageThreadRegistry;
import notification.MentionNotificationRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import moderation.AdminModerationService;
import moderation.BanRepository;
import moderation.ModerationTools;
import moderation.PostReportRepository;
import postview.PostViewService;

public class PostViewerActivity extends AppCompatActivity {
    public static final String EXTRA_TARGET_MESSAGE_ID = "target_message_id";
    private static final int HEADER_COLLAPSE_GESTURE_DP = 28;
    private static final int HEADER_EXPAND_GESTURE_DP = 112;
    private AuthManager authManager;
    private AvatarManager avatarManager;
    private FrozenUserManager frozenUserManager;
    private RelationshipStore relationshipStore;
    private User currentUser;
    private Post post;
    private ConstraintLayout rootLayout;
    private TextView textPostTitle;
    private TextView textPostAuthor;
    private TextView textPostEdited;
    private TextView textPostBody;
    private TextView textViewCount;
    private View viewCountBar;
    private ImageView imagePostAttachment;
    private ChipGroup chipGroupPostHashtags;
    private ImageView imagePostAuthorAvatar;
    private RecyclerView recyclerMessages;
    private LinearLayout reactionBar;
    private ImageButton buttonPostOverflow;
    private ImageButton buttonBack;
    /** Held so reaction taps can refresh exactly one row instead of the whole adapter. */
    private MessageAdapter messageAdapter;
    private EditText inputReply;
    private EditText activeComposerInput;
    private UUID activeReplyParentId;
    private ActivityResultLauncher<PickVisualMediaRequest> composerImageLauncher;
    private boolean postHeaderCollapsed;
    private float headerGestureStartY;
    private boolean headerGestureHandled;

    /**
     * Threaded (depth-first) list of currently-visible messages. Updated on
     * every {@link #loadMessages()}. Used both as the adapter's data source
     * and as the lookup for {@link ThreadConnectorDecoration}.
     */
    private ArrayList<Message> threadedMessages = new ArrayList<>();
    private ArrayList<UUID> adapterMessageIds = new ArrayList<>();
    /** Parent message id -> number of direct replies. Populated each {@link #loadMessages()}. */
    private final Map<UUID, Integer> messageChildCount = new HashMap<>();
    /**
     * Parent message id -> explicitly set visible-reply count. A missing
     * entry means "show all" — replies are expanded by default and the
     * user collapses by tapping the parent's body.
     */
    private final Map<UUID, Integer> expandedReplyLimits = new HashMap<>();

    private ChipGroup reactionChipGroup;

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
        relationshipStore = new RelationshipStore(this);
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

        rootLayout = findViewById(R.id.rootLayout);
        textPostTitle = findViewById(R.id.textPostTitle);
        textPostAuthor = findViewById(R.id.textPostAuthor);
        textPostEdited = findViewById(R.id.textPostEdited);
        textPostBody = findViewById(R.id.textPostBody);
        textViewCount = findViewById(R.id.textViewCount);
        viewCountBar = findViewById(R.id.viewCountBar);
        imagePostAttachment = findViewById(R.id.imagePostAttachment);
        chipGroupPostHashtags = findViewById(R.id.chipGroupPostHashtags);
        imagePostAuthorAvatar = findViewById(R.id.imagePostAuthorAvatar);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        inputReply = findViewById(R.id.inputReply);
        inputReply.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard();
            } else if (inputReply.getText().toString().trim().isEmpty()) {
                clearReplyTarget();
            }
        });
        inputReply.setOnClickListener(v -> showKeyboard());

        reactionChipGroup = findViewById(R.id.reactionChipGroup);

        setupReactionButtons();

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
        recyclerMessages.setOnTouchListener((view, event) -> {
            handleHeaderGesture(event);
            return false;
        });

        buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        Button buttonSendReply = findViewById(R.id.buttonSendReply);
        buttonSendReply.setOnClickListener(v -> addReply());
        findViewById(R.id.buttonMentionUser).setOnClickListener(v -> showMentionChooser());
        findViewById(R.id.buttonAttachImage).setOnClickListener(v -> {
            activeComposerInput = inputReply;
            chooseComposerImage();
        });
        findViewById(R.id.buttonReplyEmoji).setOnClickListener(v -> showEmojiChooser(inputReply));
        findViewById(R.id.buttonMoreFormats).setOnClickListener(v -> ComposerActionSheet.showMoreFormats(this));

        reactionBar = findViewById(R.id.reactionBar);
        buttonPostOverflow = findViewById(R.id.buttonPostOverflow);
        buttonPostOverflow.setOnClickListener(this::showPostOverflow);

        // Record one view per fresh open. savedInstanceState != null means the OS
        // recreated the activity, so do not count that again.
        if (savedInstanceState == null) {
            PostViewService.getInstance().recordView(post.id);
        }

        renderPost();
        loadMessages();
        scrollToTargetMessage(getIntent().getStringExtra(EXTRA_TARGET_MESSAGE_ID));

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Leave the top unpadded so the orange title bar draws behind the
            // status bar (Reddit-style continuous band). The header content is
            // pushed below the status icons via the back button's top margin.
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            ViewGroup.MarginLayoutParams backParams =
                    (ViewGroup.MarginLayoutParams) buttonBack.getLayoutParams();
            backParams.topMargin = systemBars.top + dp(12);
            buttonBack.setLayoutParams(backParams);
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

    private void setupReactionButtons() {
        updateReactionButtons();
    }

    private void handleHeaderGesture(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            headerGestureStartY = event.getRawY();
            headerGestureHandled = false;
            return;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            headerGestureHandled = false;
            return;
        }
        if (action != MotionEvent.ACTION_MOVE || headerGestureHandled) return;

        float drag = event.getRawY() - headerGestureStartY;
        if (!postHeaderCollapsed && -drag >= dp(HEADER_COLLAPSE_GESTURE_DP)) {
            setPostHeaderCollapsed(true, true);
            headerGestureHandled = true;
        } else if (postHeaderCollapsed && drag >= dp(HEADER_EXPAND_GESTURE_DP)) {
            setPostHeaderCollapsed(false, true);
            headerGestureHandled = true;
        }
    }

    private void setPostHeaderCollapsed(boolean collapsed, boolean animate) {
        if (postHeaderCollapsed == collapsed) return;
        postHeaderCollapsed = collapsed;
        applyPostHeaderCollapsedState(collapsed, animate);
    }

    private void applyPostHeaderCollapsedState(boolean collapsed, boolean animate) {
        if (animate) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(180);
            TransitionManager.beginDelayedTransition(rootLayout, transition);
        }

        int detailVisibility = collapsed ? View.GONE : View.VISIBLE;
        textPostAuthor.setVisibility(detailVisibility);
        viewCountBar.setVisibility(detailVisibility);
        textPostEdited.setVisibility(!collapsed && post.isEdited() ? View.VISIBLE : View.GONE);
        chipGroupPostHashtags.setVisibility(!collapsed && hasPostHashtags() ? View.VISIBLE : View.GONE);
        textPostBody.setVisibility(!collapsed && hasPostBodyText() ? View.VISIBLE : View.GONE);
        imagePostAttachment.setVisibility(!collapsed && ComposerFormatManager.hasImage(post.getBody())
                ? View.VISIBLE : View.GONE);
        reactionBar.setVisibility(detailVisibility);
        buttonPostOverflow.setVisibility(detailVisibility);

        textPostTitle.setMaxLines(collapsed ? 1 : 2);
        textPostTitle.setTextSize(collapsed ? 18f : 22f);
        alignBackButtonToTitleFirstLine();

        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(rootLayout);
        if (collapsed) {
            constraints.clear(R.id.imagePostAuthorAvatar, ConstraintSet.TOP);
            constraints.clear(R.id.textPostTitle, ConstraintSet.START);
            constraints.clear(R.id.textPostTitle, ConstraintSet.TOP);
            constraints.clear(R.id.textPostTitle, ConstraintSet.BOTTOM);
            constraints.clear(R.id.postScrollView, ConstraintSet.TOP);
            constraints.clear(R.id.recyclerMessages, ConstraintSet.TOP);

            constraints.connect(R.id.imagePostAuthorAvatar, ConstraintSet.TOP,
                    R.id.buttonBack, ConstraintSet.TOP);
            constraints.connect(R.id.textPostTitle, ConstraintSet.START,
                    R.id.imagePostAuthorAvatar, ConstraintSet.END, dp(10));
            constraints.connect(R.id.textPostTitle, ConstraintSet.TOP,
                    R.id.imagePostAuthorAvatar, ConstraintSet.TOP);
            constraints.connect(R.id.textPostTitle, ConstraintSet.BOTTOM,
                    R.id.imagePostAuthorAvatar, ConstraintSet.BOTTOM);
            constraints.connect(R.id.postScrollView, ConstraintSet.TOP,
                    R.id.textPostTitle, ConstraintSet.BOTTOM);
            constraints.connect(R.id.recyclerMessages, ConstraintSet.TOP,
                    R.id.imagePostAuthorAvatar, ConstraintSet.BOTTOM, dp(10));
        } else {
            constraints.clear(R.id.imagePostAuthorAvatar, ConstraintSet.TOP);
            constraints.clear(R.id.textPostTitle, ConstraintSet.START);
            constraints.clear(R.id.textPostTitle, ConstraintSet.TOP);
            constraints.clear(R.id.textPostTitle, ConstraintSet.BOTTOM);
            constraints.clear(R.id.postScrollView, ConstraintSet.TOP);
            constraints.clear(R.id.recyclerMessages, ConstraintSet.TOP);

            constraints.connect(R.id.imagePostAuthorAvatar, ConstraintSet.TOP,
                    R.id.textPostTitle, ConstraintSet.BOTTOM, dp(8));
            constraints.connect(R.id.textPostTitle, ConstraintSet.START,
                    R.id.buttonBack, ConstraintSet.END, dp(4));
            constraints.connect(R.id.textPostTitle, ConstraintSet.TOP,
                    R.id.buttonBack, ConstraintSet.TOP);
            constraints.connect(R.id.postScrollView, ConstraintSet.TOP,
                    R.id.textPostTitle, ConstraintSet.BOTTOM);
            constraints.connect(R.id.recyclerMessages, ConstraintSet.TOP,
                    R.id.reactionBar, ConstraintSet.BOTTOM, dp(14));
        }
        constraints.applyTo(rootLayout);
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
            chip.setOnClickListener(v -> {
                manager.toggleReaction(post.getUUID(), currentUser.getUUID(), emoji);
                updateReactionButtons();
            });
            reactionChipGroup.addView(chip);
        }

        Chip addChip = reactionChip("+");
        addChip.setContentDescription(getString(R.string.custom_reaction_title));
        addChip.setOnClickListener(v -> showPostReactionEmojiChooser());
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

    private void showPostReactionEmojiChooser() {
        ComposerFormatManager.showEmojiChooser(this, emoji -> {
            ReactionManager.getInstance().toggleReaction(
                    post.getUUID(), currentUser.getUUID(), emoji);
            updateReactionButtons();
        });
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
        textPostAuthor.setText(authorName(poster));
        textPostEdited.setVisibility(post.isEdited() ? View.VISIBLE : View.GONE);
        // View count is delegated entirely to PostViewService.
        int views = PostViewService.getInstance().getViewCount(post.id);
        textViewCount.setText(getString(R.string.view_count, views));
        renderHashtagChips();
        String body = post.getBody();
        ComposerFormatManager.bindContent(body, textPostBody, imagePostAttachment);
        alignBackButtonToTitleFirstLine();
        if (postHeaderCollapsed) {
            applyPostHeaderCollapsedState(true, false);
        }
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

        List<Message> displayItems = buildThreadDisplayItems(timeSorted);

        messageAdapter = new MessageAdapter(
                this,
                displayItems,
                currentUser.getUUID(),
                this::startReplyToMessage,
                this::handleMessageReaction,
                this::showMessageOverflow,
                this::openUserProfile,
                this::toggleReplyThread
        );
        recyclerMessages.setAdapter(messageAdapter);
    }

    private List<Message> buildThreadDisplayItems(List<Message> timeSorted) {
        MessageThreadRegistry threads = MessageThreadRegistry.getInstance();
        Set<UUID> present = new LinkedHashSet<>();
        for (Message message : timeSorted) {
            present.add(message.id());
        }

        Map<UUID, List<Message>> childrenByParent = new LinkedHashMap<>();
        ArrayList<Message> roots = new ArrayList<>();
        for (Message message : timeSorted) {
            UUID parent = threads.parentOf(message.id());
            if (parent == null || !present.contains(parent)) {
                roots.add(message);
            } else {
                childrenByParent.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(message);
            }
        }

        Comparator<Message> byPopularity = Comparator
                .comparingInt((Message message) ->
                        MessageReactionRegistry.getInstance().likeCount(message.id()))
                .reversed()
                .thenComparingLong(Message::timestamp);
        for (List<Message> children : childrenByParent.values()) {
            children.sort(byPopularity);
        }

        messageChildCount.clear();
        for (Map.Entry<UUID, List<Message>> entry : childrenByParent.entrySet()) {
            messageChildCount.put(entry.getKey(), entry.getValue().size());
        }

        ArrayList<Message> items = new ArrayList<>();
        threadedMessages = new ArrayList<>();
        adapterMessageIds = new ArrayList<>();
        for (Message root : roots) {
            appendThreadItems(root, childrenByParent, items);
        }
        return items;
    }

    private void appendThreadItems(Message message,
                                   Map<UUID, List<Message>> childrenByParent,
                                   List<Message> items) {
        items.add(message);
        threadedMessages.add(message);
        adapterMessageIds.add(message.id());

        List<Message> children = childrenByParent.get(message.id());
        if (children == null || children.isEmpty()) return;

        int visible = Math.min(
                expandedReplyLimits.getOrDefault(message.id(), children.size()),
                children.size());
        for (int i = 0; i < visible; i++) {
            appendThreadItems(children.get(i), childrenByParent, items);
        }
    }

    /**
     * Tap on a parent comment's body collapses its visible replies, or
     * re-expands them if already collapsed. Leaf comments do nothing.
     */
    private void toggleReplyThread(Message message) {
        Integer total = messageChildCount.get(message.id());
        if (total == null || total == 0) return;
        int visible = expandedReplyLimits.getOrDefault(message.id(), total);
        if (visible > 0) {
            expandedReplyLimits.put(message.id(), 0);
        } else {
            expandedReplyLimits.remove(message.id());
        }
        loadMessages();
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

    /**
     * Top-right ⋮ menu for the whole post: Save / Unsave (everyone),
     * Report (non-owner, non-admin), Edit / Delete (owner). Mirrors the
     * per-comment overflow in {@link #showMessageOverflow}.
     */
    private void showPostOverflow(View anchor) {
        boolean owner = currentUser.getUUID().equals(post.poster);
        boolean admin = currentUser.role() == User.Role.Admin;
        boolean canReport = !owner && !admin;
        boolean saved = MessageBookmarkRegistry.getInstance()
                .isBookmarked(currentUser.getUUID(), post.getUUID());

        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.post_overflow_menu);
        popup.getMenu().findItem(R.id.menu_save_post).setVisible(!saved);
        popup.getMenu().findItem(R.id.menu_unsave_post).setVisible(saved);
        popup.getMenu().findItem(R.id.menu_report_post).setVisible(canReport);
        popup.getMenu().findItem(R.id.menu_edit_post).setVisible(owner);
        popup.getMenu().findItem(R.id.menu_delete_post).setVisible(owner);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_save_post || id == R.id.menu_unsave_post) {
                boolean nowSaved = MessageBookmarkRegistry.getInstance()
                        .toggle(currentUser.getUUID(), post.getUUID());
                Toast.makeText(this,
                        nowSaved ? R.string.message_saved : R.string.message_unsaved,
                        Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_report_post) {
                showReportPostDialog();
                return true;
            } else if (id == R.id.menu_edit_post) {
                showEditPostDialog();
                return true;
            } else if (id == R.id.menu_delete_post) {
                confirmDeletePost();
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Vertically center the back arrow on the title's first text line. The
     * title shares its top edge with the back button, but the arrow is
     * centered inside a 44dp touch target and so reads as sitting below a
     * single line of title text — this nudges it up to align.
     */
    private void alignBackButtonToTitleFirstLine() {
        textPostTitle.post(() -> {
            android.text.Layout layout = textPostTitle.getLayout();
            if (layout == null) return;
            int firstLineCenter = textPostTitle.getPaddingTop()
                    + (layout.getLineTop(0) + layout.getLineBottom(0)) / 2;
            buttonBack.setTranslationY(firstLineCenter - buttonBack.getHeight() / 2f);
        });
    }

    private int indexOfThreaded(UUID messageId) {
        for (int i = 0; i < adapterMessageIds.size(); i++) {
            if (messageId.equals(adapterMessageIds.get(i))) return i;
        }
        return -1;
    }

    /** Lookup used by {@link ThreadConnectorDecoration}. */
    private UUID messageIdAt(int position) {
        if (position < 0 || position >= adapterMessageIds.size()) return null;
        return adapterMessageIds.get(position);
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
        long timestamp = System.currentTimeMillis();
        post.messages.insert(new Message(
                newId,
                currentUser.getUUID(),
                post.getUUID(),
                timestamp,
                content
        ));
        if (parentMessageId != null) {
            MessageThreadRegistry.getInstance().setParent(newId, parentMessageId);
        }
        AndroidPostStore.saveAll(this);
        notifyMentionedUsers(content, newId, timestamp);
        Toast.makeText(this, R.string.reply_sent, Toast.LENGTH_SHORT).show();
        loadMessages();
        return true;
    }

    private void notifyMentionedUsers(String content, UUID messageId, long timestamp) {
        String preview = ComposerFormatManager.previewText(content);
        for (UUID recipient : mentionedRecipients(content)) {
            MentionNotificationRegistry.getInstance().addMention(
                    recipient,
                    currentUser.getUUID(),
                    post.getUUID(),
                    messageId,
                    timestamp,
                    preview
            );
        }
    }

    private Set<UUID> mentionedRecipients(String content) {
        Set<UUID> recipients = new LinkedHashSet<>();
        if (content == null || content.isEmpty()) return recipients;

        String lower = content.toLowerCase(Locale.ROOT);
        Iterator<User> users = UserDAO.getInstance().getAll();
        while (users.hasNext()) {
            User user = users.next();
            if (user == null || user.getUUID().equals(currentUser.getUUID())) continue;

            for (String alias : mentionAliases(user)) {
                if (!alias.isEmpty() && lower.contains("@" + alias.toLowerCase(Locale.ROOT))) {
                    recipients.add(user.getUUID());
                    break;
                }
            }
        }
        return recipients;
    }

    private ArrayList<String> mentionAliases(User user) {
        ArrayList<String> aliases = new ArrayList<>();
        aliases.add(authManager.getDisplayName(user).replaceAll("\\s+", ""));
        if (user.username() != null) {
            aliases.add(user.username().replaceAll("\\s+", ""));
        }
        return aliases;
    }

    private void scrollToTargetMessage(String messageIdText) {
        if (messageIdText == null || messageIdText.isEmpty()) return;
        try {
            UUID messageId = UUID.fromString(messageIdText);
            recyclerMessages.postDelayed(() -> {
                int index = indexOfThreaded(messageId);
                if (index >= 0) {
                    recyclerMessages.smoothScrollToPosition(index);
                } else {
                    Toast.makeText(this, R.string.message_not_found, Toast.LENGTH_SHORT).show();
                }
            }, 250);
        } catch (IllegalArgumentException ignored) {
            Toast.makeText(this, R.string.message_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void startReplyToMessage(Message parent) {
        activeReplyParentId = parent.id();
        User author = UserDAO.getInstance().getByUUID(parent.poster());
        String authorName = author == null ? "Unknown user" : authManager.getDisplayName(author);
        inputReply.setHint(getString(R.string.replying_to_hint, authorName));
        inputReply.requestFocus();
        showKeyboard();
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

    private void showKeyboard() {
        inputReply.post(() -> {
            inputReply.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(inputReply, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void showComposerMenu(EditText input) {
        activeComposerInput = input;
        ComposerActionSheet.show(
                this,
                this::chooseComposerImage,
                () -> showEmojiChooser(input)
        );
    }

    private void showMentionChooser() {
        Map<UUID, User> candidates = mentionCandidates();
        if (candidates.isEmpty()) {
            Toast.makeText(this, R.string.no_mention_targets, Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(24));
        root.setBackgroundColor(getColor(R.color.surface));

        TextView title = new TextView(this);
        title.setText(R.string.mention_people);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(18f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        for (User user : candidates.values()) {
            TextView row = new TextView(this);
            row.setText("@" + authManager.getDisplayName(user));
            row.setTextColor(getColor(R.color.text_primary));
            row.setTextSize(16f);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), 0, dp(4), 0);
            row.setOnClickListener(v -> {
                insertMention(user);
                dialog.dismiss();
            });
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            );
            rowParams.topMargin = dp(8);
            root.addView(row, rowParams);
        }

        dialog.setContentView(root);
        dialog.show();
    }

    private Map<UUID, User> mentionCandidates() {
        Map<UUID, User> candidates = new LinkedHashMap<>();
        addMentionCandidates(candidates, relationshipStore.friendsOf(currentUser.getUUID()));
        addMentionCandidates(candidates, relationshipStore.followingOf(currentUser.getUUID()));
        return candidates;
    }

    private void addMentionCandidates(Map<UUID, User> candidates, Set<UUID> ids) {
        for (UUID id : ids) {
            User user = UserDAO.getInstance().getByUUID(id);
            if (user != null && !user.getUUID().equals(currentUser.getUUID())) {
                candidates.put(user.getUUID(), user);
            }
        }
    }

    private void insertMention(User user) {
        String name = authManager.getDisplayName(user).replaceAll("\\s+", "");
        insertAtCursor(inputReply, "@" + name + " ");
        inputReply.requestFocus();
        showKeyboard();
    }

    private void insertAtCursor(EditText input, String value) {
        int start = Math.max(input.getSelectionStart(), 0);
        int end = Math.max(input.getSelectionEnd(), 0);
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        input.getText().replace(min, max, value);
    }

    private boolean hasPostHashtags() {
        java.util.List<String> tags = post.getHashtags();
        return tags != null && !tags.isEmpty();
    }

    private boolean hasPostBodyText() {
        return !ComposerFormatManager.textOnly(post.getBody()).trim().isEmpty();
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
        ComposerFormatManager.showEmojiChooser(this, input);
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
        AndroidPostStore.saveAll(this);
        renderPost();
        Toast.makeText(this, R.string.post_updated, Toast.LENGTH_SHORT).show();
    }

    private void confirmDeletePost() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_post_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    post.setDeleted(true);
                    HashtagService.getInstance().removePost(post);
                    AndroidPostStore.saveAll(this);
                    Toast.makeText(this, R.string.post_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }

    // ── Post-level reporting ──────────────────────────────────────────────────

    /**
     * Show a dialog letting the current user report the post with a typed reason.
     * Delegates entirely to {@link AdminModerationService} — no report state in Activity.
     */
    private void showReportPostDialog() {
        if (PostReportRepository.getInstance().hasReported(post.id, currentUser.getUUID())) {
            Toast.makeText(this, R.string.report_already_submitted, Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint(R.string.report_reason_hint);
        input.setMinLines(2);
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16 / 2, dp16, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.report_post)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.submit_report, (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    boolean ok = AdminModerationService.getInstance().submitReport(
                            post.id, currentUser.getUUID(), post.poster, reason);
                    Toast.makeText(this,
                            ok ? R.string.report_submitted : R.string.report_already_submitted,
                            Toast.LENGTH_SHORT).show();
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
        AndroidPostStore.saveAll(this);
        Toast.makeText(this, R.string.reply_updated, Toast.LENGTH_SHORT).show();
        loadMessages();
    }

    private void confirmDeleteReply(Message message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_reply_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    MessageDeletionRegistry.getInstance().markDeleted(message.id());
                    AndroidPostStore.saveAll(this);
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
