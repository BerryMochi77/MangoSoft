package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

import com.example.comp2100miniproject.auth.AuthManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import messagestate.MessageDeletionRegistry;
import messagestate.MessageEditRegistry;

/**
 * Admin view of all replies/messages across all posts.
 *
 * Admin actions:
 *  - Hide / unhide a reply   → uses existing {@link Message#setHidden(boolean)}
 *    (already in Message.java; NOT a new field)
 *  - Delete a reply          → uses {@link MessageDeletionRegistry} sidecar
 *    (already exists; NOT a modification to Message.java)
 *
 * Tapping a reply shows a detail dialog. The dialog also offers "View post"
 * (opens {@link PostViewerActivity}) and "View author" (opens
 * {@link AdminUserListActivity} filtered on that user — future improvement;
 * for now the user's username is shown in the dialog).
 *
 * Message.java is NOT modified — both hide and delete use existing mechanisms.
 */
public class AdminReplyListActivity extends AppCompatActivity {

    private AuthManager authManager;
    private User currentUser;
    private LinearLayout container;

    // Lightweight carry of what each displayed row needs for actions
    private static class ReplySummary {
        final Message message;
        final String content;       // current edited content
        final String authorUsername;
        final UUID   authorId;
        final String parentPostTitle;
        final UUID   parentPostId;
        final int    parentPostIndex;

        ReplySummary(Message m, String content, String authorUsername,
                     UUID authorId, String parentPostTitle, UUID parentPostId, int parentPostIndex) {
            this.message = m;
            this.content = content;
            this.authorUsername = authorUsername;
            this.authorId = authorId;
            this.parentPostTitle = parentPostTitle;
            this.parentPostId = parentPostId;
            this.parentPostIndex = parentPostIndex;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_list);

        authManager = new AuthManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null || currentUser.role() != User.Role.Admin) {
            finish(); return;
        }

        ImageButton back = findViewById(R.id.buttonBack);
        back.setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.textListTitle);
        title.setText(R.string.dash_reply_database);

        container = findViewById(R.id.listContainer);
        loadList();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.rootLayout), (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (container != null) loadList();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadList() {
        container.removeAllViews();
        List<ReplySummary> replies = buildReplySummaries();

        if (replies.isEmpty()) {
            container.addView(emptyLabel(R.string.dash_no_data)); return;
        }
        for (int i = 0; i < replies.size(); i++) {
            if (i > 0) container.addView(divider());
            container.addView(replyRow(replies.get(i)));
        }
    }

    private List<ReplySummary> buildReplySummaries() {
        List<ReplySummary> result = new ArrayList<>();
        MessageEditRegistry edits = MessageEditRegistry.getInstance();

        int postIdx = 0;
        for (Iterator<Post> pi = PostDAO.getInstance().getAll(); pi.hasNext(); postIdx++) {
            Post post = pi.next();
            String postTitle = post.topic != null ? post.topic : "(no title)";
            final int finalPostIdx = post.isDeleted() ? -1 : postIdx;

            for (Iterator<Message> mi = post.messages.getAll(); mi.hasNext(); ) {
                Message m = mi.next();
                String current = edits.currentContent(m.id(), m.message());
                User author = UserDAO.getInstance().getByUUID(m.poster());
                String authorName = author != null ? author.username() : "?";
                result.add(new ReplySummary(m, current, authorName,
                        m.poster(), postTitle, post.id, finalPostIdx));
            }
        }
        // Sort newest first by timestamp
        result.sort((a, b) -> Long.compare(b.message.timestamp(), a.message.timestamp()));
        return result;
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    private View replyRow(ReplySummary rs) {
        boolean deleted = MessageDeletionRegistry.getInstance().isDeleted(rs.message.id());
        boolean hidden  = rs.message.isHidden();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setClickable(true);
        row.setFocusable(true);

        // Content preview
        TextView contentTv = new TextView(this);
        String preview = rs.content.length() > 80 ? rs.content.substring(0, 77) + "…" : rs.content;
        contentTv.setText(preview);
        contentTv.setTextSize(14f);
        contentTv.setTextColor(deleted
                ? getColor(R.color.text_secondary) : getColor(R.color.text_primary));
        if (deleted) contentTv.setPaintFlags(
                contentTv.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        row.addView(contentTv);

        // Author + parent post
        TextView metaTv = new TextView(this);
        String postLabel = rs.parentPostTitle.length() > 30
                ? rs.parentPostTitle.substring(0, 27) + "…" : rs.parentPostTitle;
        metaTv.setText(rs.authorUsername + "  ·  " + postLabel);
        metaTv.setTextSize(12f);
        metaTv.setTextColor(getColor(R.color.text_secondary));
        metaTv.setPadding(0, dp(2), 0, 0);
        row.addView(metaTv);

        // Status tags
        if (deleted || hidden) {
            TextView tag = new TextView(this);
            tag.setText(deleted ? getString(R.string.dash_status_deleted)
                    : getString(R.string.hidden));
            tag.setTextSize(11f);
            tag.setTextColor(getColor(R.color.warning));
            row.addView(tag);
        }

        row.setOnClickListener(v -> showReplyDetail(rs));
        return row;
    }

    // ── Reply detail dialog ───────────────────────────────────────────────────

    private void showReplyDetail(ReplySummary rs) {
        boolean deleted = MessageDeletionRegistry.getInstance().isDeleted(rs.message.id());
        boolean hidden  = rs.message.isHidden();

        String details =
                getString(R.string.dash_detail_author)  + ": " + rs.authorUsername + "\n" +
                getString(R.string.dash_detail_post)    + ": " + rs.parentPostTitle + "\n" +
                getString(R.string.dash_detail_time)    + ": " +
                        AppTimeFormatter.format(rs.message.timestamp(), this) + "\n" +
                getString(R.string.dash_detail_status)  + ": " +
                        (deleted ? getString(R.string.dash_status_deleted)
                                : hidden ? getString(R.string.hidden)
                                : getString(R.string.dash_active)) + "\n\n" +
                rs.content;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dash_reply_detail))
                .setMessage(details)
                .setNeutralButton(R.string.cancel, null);

        if (!deleted) {
            // Toggle hide / unhide
            builder.setNegativeButton(
                    hidden ? R.string.unhide_message : R.string.hide_message,
                    (d, w) -> {
                        rs.message.setHidden(!hidden); // existing mechanism — no Message.java change
                        AndroidPostStore.saveAll(this);
                        Toast.makeText(this,
                                hidden ? R.string.message_unhidden : R.string.message_hidden,
                                Toast.LENGTH_SHORT).show();
                        loadList();
                    });
            // Delete
            builder.setPositiveButton(R.string.dash_delete_reply, (d, w) -> {
                MessageDeletionRegistry.getInstance().markDeleted(rs.message.id());
                AndroidPostStore.saveAll(this);
                Toast.makeText(this, R.string.reply_deleted, Toast.LENGTH_SHORT).show();
                loadList();
            });
        }

        // "View post" button — opens PostViewerActivity for parent post
        if (rs.parentPostIndex >= 0) {
            builder.setOnDismissListener(d -> {}); // keep builder reference
            AlertDialog dialog = builder.create();
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.cancel),
                    (d, w) -> {});
            // We add view-post via the layout of the AlertDialog after show() — simplest:
            // override via a custom title + message and add extra button via setButton
            // Actually, AlertDialog only supports 3 buttons. View Post reuses NEUTRAL.
            // Re-build with NEUTRAL = "View post" instead.
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dash_reply_detail))
                    .setMessage(details)
                    .setNeutralButton(R.string.dash_view_post, (d, w) -> openParentPost(rs))
                    .setNegativeButton(
                            hidden ? R.string.unhide_message : R.string.hide_message,
                            deleted ? null : (d, w) -> {
                                rs.message.setHidden(!hidden);
                                AndroidPostStore.saveAll(this);
                                loadList();
                            })
                    .setPositiveButton(R.string.dash_delete_reply, deleted ? null : (d, w) -> {
                        MessageDeletionRegistry.getInstance().markDeleted(rs.message.id());
                        AndroidPostStore.saveAll(this);
                        Toast.makeText(this, R.string.reply_deleted, Toast.LENGTH_SHORT).show();
                        loadList();
                    })
                    .show();
        } else {
            builder.show();
        }
    }

    private void openParentPost(ReplySummary rs) {
        if (rs.parentPostIndex < 0) return;
        Intent intent = new Intent(this, PostViewerActivity.class);
        intent.putExtra("post_index", rs.parentPostIndex);
        intent.putExtra(AuthManager.EXTRA_USER_ID,  currentUser.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, true);
        startActivity(intent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private View emptyLabel(int resId) {
        TextView tv = new TextView(this);
        tv.setText(resId);
        tv.setTextSize(15f);
        tv.setTextColor(getColor(R.color.text_secondary));
        tv.setPadding(dp(16), dp(16), dp(16), dp(16));
        return tv;
    }

    private View divider() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(getColor(R.color.border));
        return v;
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private UUID readCurrentUserId() {
        String v = getIntent().getStringExtra(AuthManager.EXTRA_USER_ID);
        if (v == null) return null;
        try { return UUID.fromString(v); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
