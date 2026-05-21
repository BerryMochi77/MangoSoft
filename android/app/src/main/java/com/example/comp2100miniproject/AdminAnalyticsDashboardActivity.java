package com.example.comp2100miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.comp2100miniproject.auth.AuthManager;

import dao.model.User;

/**
 * Admin-only Data Analytics Dashboard — now a navigation hub.
 *
 * Each stat row and each section header is clickable and navigates to the
 * corresponding admin management list page. This Activity only binds data
 * and sets up listeners — all computation is in {@link AdminAnalyticsService}.
 *
 * Navigation map:
 *  Total Users     → {@link AdminUserListActivity}   (MODE_ALL)
 *  Total Posts     → {@link AdminPostListActivity}   (MODE_ALL)
 *  Total Replies   → {@link AdminReplyListActivity}
 *  Posts Today     → {@link AdminPostListActivity}   (MODE_TODAY)
 *  Pending Reports → {@link AdminPostReportsActivity}
 *  Most Viewed     → {@link AdminPostListActivity}   (MODE_POPULAR / sort by views)
 *  Most Liked      → {@link AdminPostListActivity}   (MODE_POPULAR / sort by reactions)
 *  Active Users    → {@link AdminUserListActivity}   (MODE_RANKING)
 *
 * Message.java is not modified — all data is read-only from existing singletons.
 */
public class AdminAnalyticsDashboardActivity extends AppCompatActivity {

    private AuthManager authManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_analytics_dashboard);

        authManager = new AuthManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null || currentUser.role() != User.Role.Admin) {
            finish();
            return;
        }

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        wireNavigation();
        renderDashboard();

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
        renderDashboard();
    }

    // ── Navigation wiring ─────────────────────────────────────────────────────

    private void wireNavigation() {
        // System Overview rows
        click(R.id.rowTotalUsers,     () -> openUserList(AdminUserListActivity.MODE_ALL));
        click(R.id.rowTotalPosts,     () -> openPostList(AdminPostListActivity.MODE_ALL));
        click(R.id.rowTotalReplies,   this::openReplyList);
        click(R.id.rowPostsToday,     () -> openPostList(AdminPostListActivity.MODE_TODAY));
        click(R.id.rowPendingReports, this::openReportManagement);

        // Section headers (Most Viewed, Most Reacted, Active Users)
        click(R.id.headerMostViewed,  () -> openPostList(AdminPostListActivity.MODE_POPULAR_VIEWS));
        click(R.id.headerMostReacted, () -> openPostList(AdminPostListActivity.MODE_POPULAR_REACTED));
        click(R.id.headerActiveUsers, () -> openUserList(AdminUserListActivity.MODE_RANKING));
    }

    private void click(int viewId, Runnable action) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(ignored -> action.run());
    }

    private void openUserList(String mode) {
        Intent intent = new Intent(this, AdminUserListActivity.class);
        intent.putExtra(AdminUserListActivity.EXTRA_MODE, mode);
        putAdminExtras(intent);
        startActivity(intent);
    }

    private void openPostList(String mode) {
        Intent intent = new Intent(this, AdminPostListActivity.class);
        intent.putExtra(AdminPostListActivity.EXTRA_MODE, mode);
        putAdminExtras(intent);
        startActivity(intent);
    }

    private void openReplyList() {
        Intent intent = new Intent(this, AdminReplyListActivity.class);
        putAdminExtras(intent);
        startActivity(intent);
    }

    private void openReportManagement() {
        Intent intent = new Intent(this, AdminPostReportsActivity.class);
        putAdminExtras(intent);
        startActivity(intent);
    }

    private void putAdminExtras(Intent intent) {
        intent.putExtra(AuthManager.EXTRA_USER_ID,  currentUser.getUUID().toString());
        intent.putExtra(AuthManager.EXTRA_IS_ADMIN, true);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderDashboard() {
        DashboardStats stats = AdminAnalyticsService.computeStats();

        setText(R.id.statTotalUsers,     String.valueOf(stats.totalUsers));
        setText(R.id.statTotalPosts,     String.valueOf(stats.totalPosts));
        setText(R.id.statTotalReplies,   String.valueOf(stats.totalReplies));
        setText(R.id.statPostsToday,     String.valueOf(stats.postsToday));
        setText(R.id.statPendingReports, String.valueOf(stats.pendingReports));

        bindPostCard(R.id.cardMostViewed,  stats.mostViewedPosts,  true);
        bindPostCard(R.id.cardMostReacted, stats.mostReactedPosts, false);
        bindUserCard(stats.activeUsers);
    }

    private void bindPostCard(int cardId, java.util.List<PostStat> posts, boolean showViews) {
        LinearLayout card = findViewById(cardId);
        card.removeAllViews();
        if (posts.isEmpty()) { card.addView(emptyRow()); return; }
        for (int i = 0; i < posts.size(); i++) {
            if (i > 0) card.addView(divider());
            PostStat ps = posts.get(i);
            LinearLayout row = hRow(
                    ps.title != null && ps.title.length() > 38
                            ? ps.title.substring(0, 35) + "…" : (ps.title != null ? ps.title : "—"),
                    showViews
                            ? getString(R.string.dash_views_format, ps.viewCount)
                            : getString(R.string.dash_reactions_format, ps.totalReactions));
            card.addView(row);
        }
    }

    private void bindUserCard(java.util.List<UserStat> users) {
        LinearLayout card = findViewById(R.id.cardActiveUsers);
        card.removeAllViews();
        if (users.isEmpty()) { card.addView(emptyRow()); return; }
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) card.addView(divider());
            card.addView(hRow(users.get(i).username,
                    getString(R.string.dash_posts_format, users.get(i).postCount)));
        }
    }

    // ── Row helpers ───────────────────────────────────────────────────────────

    private LinearLayout hRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView lv = new TextView(this);
        lv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        lv.setText(label);
        lv.setTextSize(14f);
        lv.setTextColor(getColor(R.color.text_primary));
        row.addView(lv);

        TextView rv = new TextView(this);
        rv.setText(value);
        rv.setTextSize(13f);
        rv.setTextColor(getColor(R.color.text_secondary));
        row.addView(rv);
        return row;
    }

    private View emptyRow() {
        TextView tv = new TextView(this);
        tv.setText(R.string.dash_no_data);
        tv.setTextSize(14f);
        tv.setTextColor(getColor(R.color.text_secondary));
        tv.setPadding(dp(12), dp(12), dp(12), dp(12));
        return tv;
    }

    private View divider() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(dp(12), 0, dp(12), 0);
        v.setLayoutParams(lp);
        v.setBackgroundColor(getColor(R.color.border));
        return v;
    }

    private void setText(int id, String value) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(value);
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private java.util.UUID readCurrentUserId() {
        String v = getIntent().getStringExtra(AuthManager.EXTRA_USER_ID);
        if (v == null) return null;
        try { return java.util.UUID.fromString(v); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
