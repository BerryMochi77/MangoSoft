package com.example.comp2100miniproject;

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
 * Admin-only Data Analytics Dashboard.
 *
 * This Activity is intentionally thin: it only requests a {@link DashboardStats}
 * snapshot from {@link AdminAnalyticsService} and renders it. No business logic
 * or data access lives here.
 *
 * Demonstrates separation of concerns:
 *  - {@link AdminAnalyticsService} owns all calculation.
 *  - {@link DashboardStats} / {@link PostStat} / {@link UserStat} carry the data.
 *  - This class contains only UI binding code.
 *
 * Message.java is not modified — all data is read-only from existing singletons.
 */
public class AdminAnalyticsDashboardActivity extends AppCompatActivity {

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_analytics_dashboard);

        authManager = new AuthManager(this);
        User currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null || currentUser.role() != User.Role.Admin) {
            finish();
            return;
        }

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

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

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderDashboard() {
        DashboardStats stats = AdminAnalyticsService.computeStats();
        bindOverview(stats);
        bindPostList(findViewById(R.id.cardMostViewed),  stats.mostViewedPosts,  true);
        bindPostList(findViewById(R.id.cardMostReacted), stats.mostReactedPosts, false);
        bindUserList(stats.activeUsers);
    }

    /** Fill the five summary stat rows with numbers from {@link DashboardStats}. */
    private void bindOverview(DashboardStats stats) {
        setText(R.id.statTotalUsers,     String.valueOf(stats.totalUsers));
        setText(R.id.statTotalPosts,     String.valueOf(stats.totalPosts));
        setText(R.id.statTotalReplies,   String.valueOf(stats.totalReplies));
        setText(R.id.statPostsToday,     String.valueOf(stats.postsToday));
        setText(R.id.statPendingReports, String.valueOf(stats.pendingReports));
    }

    /** Populate a post-list card with {@link PostStat} rows. */
    private void bindPostList(LinearLayout card, java.util.List<PostStat> posts,
                               boolean showViews) {
        card.removeAllViews();
        if (posts.isEmpty()) {
            card.addView(emptyRow());
            return;
        }
        for (int i = 0; i < posts.size(); i++) {
            PostStat ps = posts.get(i);
            if (i > 0) card.addView(divider());
            card.addView(postRow(ps, showViews));
        }
    }

    /** Populate the active-users card with {@link UserStat} rows. */
    private void bindUserList(java.util.List<UserStat> users) {
        LinearLayout card = findViewById(R.id.cardActiveUsers);
        card.removeAllViews();
        if (users.isEmpty()) {
            card.addView(emptyRow());
            return;
        }
        for (int i = 0; i < users.size(); i++) {
            UserStat us = users.get(i);
            if (i > 0) card.addView(divider());
            card.addView(userRow(us));
        }
    }

    // ── Row builders ──────────────────────────────────────────────────────────

    private View postRow(PostStat ps, boolean showViews) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int dp12 = dp(12);
        row.setPadding(dp12, dp(10), dp12, dp(10));

        // Title (truncated)
        TextView title = new TextView(this);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        String rawTitle = ps.title != null ? ps.title : "—";
        title.setText(rawTitle.length() > 40 ? rawTitle.substring(0, 37) + "…" : rawTitle);
        title.setTextSize(14f);
        title.setTextColor(getColor(R.color.text_primary));
        row.addView(title);

        // Metric value
        TextView metric = new TextView(this);
        metric.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        if (showViews) {
            metric.setText(getString(R.string.dash_views_format, ps.viewCount));
        } else {
            metric.setText(getString(R.string.dash_reactions_format, ps.totalReactions));
        }
        metric.setTextSize(13f);
        metric.setTextColor(getColor(R.color.text_secondary));
        row.addView(metric);

        return row;
    }

    private View userRow(UserStat us) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int dp12 = dp(12);
        row.setPadding(dp12, dp(10), dp12, dp(10));

        TextView name = new TextView(this);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        name.setText(us.username);
        name.setTextSize(14f);
        name.setTextColor(getColor(R.color.text_primary));
        row.addView(name);

        TextView count = new TextView(this);
        count.setText(getString(R.string.dash_posts_format, us.postCount));
        count.setTextSize(13f);
        count.setTextColor(getColor(R.color.text_secondary));
        row.addView(count);

        return row;
    }

    private View emptyRow() {
        TextView tv = new TextView(this);
        tv.setText(R.string.dash_no_data);
        tv.setTextSize(14f);
        tv.setTextColor(getColor(R.color.text_secondary));
        int dp = dp(12);
        tv.setPadding(dp, dp, dp, dp);
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setText(int viewId, String value) {
        TextView tv = findViewById(viewId);
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
