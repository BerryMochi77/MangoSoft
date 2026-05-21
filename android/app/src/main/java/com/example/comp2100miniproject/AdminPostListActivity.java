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

import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagService;

/**
 * Admin post management list, supporting three filter modes:
 *
 *  MODE_ALL            → all posts (including deleted)
 *  MODE_TODAY          → posts created today
 *  MODE_POPULAR_VIEWS  → posts sorted by view count
 *  MODE_POPULAR_REACTED → posts sorted by reaction count
 *
 * Tapping a post opens a detail dialog with admin actions (delete).
 * For non-deleted posts the dialog also has a "View post" button that
 * opens the existing {@link PostViewerActivity}.
 *
 * Message.java is not modified — reply counts read raw message storage.
 */
public class AdminPostListActivity extends AppCompatActivity {

    public static final String EXTRA_MODE            = "mode";
    public static final String MODE_ALL              = "all";
    public static final String MODE_TODAY            = "today";
    public static final String MODE_POPULAR_VIEWS    = "popular_views";
    public static final String MODE_POPULAR_REACTED  = "popular_reacted";

    private AuthManager authManager;
    private User currentUser;
    private LinearLayout container;
    private String mode;

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

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_ALL;

        ImageButton back = findViewById(R.id.buttonBack);
        back.setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.textListTitle);
        title.setText(titleResForMode(mode));

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

    private int titleResForMode(String m) {
        switch (m) {
            case MODE_TODAY:           return R.string.dash_todays_posts;
            case MODE_POPULAR_VIEWS:   return R.string.dash_most_viewed_posts;
            case MODE_POPULAR_REACTED: return R.string.dash_most_liked_posts;
            default:                   return R.string.dash_post_database;
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadList() {
        container.removeAllViews();
        List<PostAdminSummary> posts;
        switch (mode) {
            case MODE_TODAY:           posts = AdminAnalyticsService.getTodaysPostsForAdmin();   break;
            case MODE_POPULAR_VIEWS:
            case MODE_POPULAR_REACTED: posts = AdminAnalyticsService.getPopularPostsForAdmin();  break;
            default:                   posts = AdminAnalyticsService.getAllPostsForAdmin();       break;
        }
        if (MODE_POPULAR_REACTED.equals(mode)) {
            // getPopularPostsForAdmin sorts by views+reactions; re-sort by reactions only
            java.util.List<PostAdminSummary> sorted = new java.util.ArrayList<>(posts);
            sorted.sort((a, b) -> Integer.compare(b.totalReactions, a.totalReactions));
            posts = sorted;
        }

        if (posts.isEmpty()) {
            container.addView(emptyLabel(R.string.dash_no_data)); return;
        }
        for (int i = 0; i < posts.size(); i++) {
            if (i > 0) container.addView(divider());
            container.addView(postRow(posts.get(i)));
        }
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    private View postRow(PostAdminSummary ps) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setClickable(true);
        row.setFocusable(true);

        // Title
        TextView titleTv = new TextView(this);
        String rawTitle = ps.title.isEmpty() ? "(no title)" : ps.title;
        titleTv.setText(rawTitle.length() > 60 ? rawTitle.substring(0, 57) + "…" : rawTitle);
        titleTv.setTextSize(14f);
        titleTv.setTextColor(ps.deleted
                ? getColor(R.color.text_secondary) : getColor(R.color.text_primary));
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        if (ps.deleted) titleTv.setPaintFlags(
                titleTv.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        row.addView(titleTv);

        // Meta: author + time
        TextView meta = new TextView(this);
        meta.setText(ps.authorUsername + "  ·  " +
                AppTimeFormatter.format(ps.createdAt, this));
        meta.setTextSize(12f);
        meta.setTextColor(getColor(R.color.text_secondary));
        meta.setPadding(0, dp(2), 0, dp(2));
        row.addView(meta);

        // Stats line
        TextView stats = new TextView(this);
        stats.setText(getString(R.string.dash_post_stats,
                ps.viewCount, ps.totalReactions, ps.replyCount, ps.reportCount));
        stats.setTextSize(12f);
        stats.setTextColor(getColor(R.color.text_secondary));
        row.addView(stats);

        // Status badge
        if (ps.deleted || ps.reportCount > 0) {
            TextView badge = new TextView(this);
            badge.setTextSize(11f);
            if (ps.deleted) {
                badge.setText(R.string.dash_status_deleted);
                badge.setTextColor(getColor(R.color.warning));
            } else {
                badge.setText(getString(R.string.dash_status_reported, ps.reportCount));
                badge.setTextColor(getColor(R.color.warning));
            }
            row.addView(badge);
        }

        row.setOnClickListener(v -> showPostDetail(ps));
        return row;
    }

    // ── Post detail dialog ────────────────────────────────────────────────────

    private void showPostDetail(PostAdminSummary ps) {
        String details =
                getString(R.string.dash_detail_author)  + ": " + ps.authorUsername    + "\n" +
                getString(R.string.dash_detail_created) + ": " +
                        AppTimeFormatter.format(ps.createdAt, this) + "\n" +
                getString(R.string.view_count_icon_desc)+ ": " + ps.viewCount          + "\n" +
                getString(R.string.dash_reactions_label)+ ": " + ps.totalReactions     + "\n" +
                getString(R.string.dash_total_replies)  + ": " + ps.replyCount         + "\n" +
                getString(R.string.dash_reports_label)  + ": " + ps.reportCount        + "\n" +
                getString(R.string.dash_detail_status)  + ": " +
                        getString(ps.deleted ? R.string.dash_status_deleted : R.string.dash_active);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(ps.title.isEmpty() ? "(no title)" : ps.title)
                .setMessage(details)
                .setNeutralButton(R.string.cancel, null);

        if (!ps.deleted) {
            builder.setNegativeButton(R.string.delete_post_action, (d, w) -> deletePost(ps));
            builder.setPositiveButton(R.string.dash_view_post, (d, w) -> openPost(ps));
        }

        builder.show();
    }

    private void deletePost(PostAdminSummary ps) {
        Post post = PostDAO.getInstance().get(new Post(ps.postId));
        if (post != null) {
            post.setDeleted(true);
            HashtagService.getInstance().removePost(post);
            AndroidPostStore.saveAll(this);
            Toast.makeText(this, R.string.post_deleted_by_admin, Toast.LENGTH_SHORT).show();
            loadList();
        }
    }

    private void openPost(PostAdminSummary ps) {
        if (ps.postIndex < 0) {
            Toast.makeText(this, R.string.post_deleted_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PostViewerActivity.class);
        intent.putExtra("post_index", ps.postIndex);
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
