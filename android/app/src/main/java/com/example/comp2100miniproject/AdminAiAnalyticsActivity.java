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

import com.example.comp2100miniproject.ai.AIAnalyticsSummary;
import com.example.comp2100miniproject.ai.AIAnalyticsService;
import com.example.comp2100miniproject.auth.AuthManager;

import java.util.Map;
import java.util.UUID;

import dao.model.User;

/**
 * Admin-only AI Filter Analytics page.
 *
 * Displays metrics computed by {@link AIAnalyticsService} from
 * {@link com.example.comp2100miniproject.ai.AIAnalyticsRepository}, which is
 * populated whenever {@link AiFragment} completes a curation batch.
 *
 * Design:
 *  - Activity is thin — it only calls the service and renders.
 *  - All computation lives in {@link AIAnalyticsService} (separation of concerns).
 *  - Uses {@link R.layout#activity_admin_list} (shared admin layout) for visual
 *    consistency with other admin management pages.
 *  - Message.java is NOT modified.
 *
 * Three analytics sections:
 *  1. AI Overview  — scored/filtered counts, average score
 *  2. Filtering Pattern — most common filter category + breakdown
 *  3. User Feedback — not-relevant and show-anyway click counts
 */
public class AdminAiAnalyticsActivity extends AppCompatActivity {

    private AuthManager authManager;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_list);

        authManager = new AuthManager(this);
        User user = authManager.getUser(readCurrentUserId());
        if (user == null || user.role() != User.Role.Admin) {
            finish(); return;
        }

        ImageButton back = findViewById(R.id.buttonBack);
        back.setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.textListTitle);
        title.setText(R.string.ai_analytics_title);

        container = findViewById(R.id.listContainer);
        renderAnalytics();

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
        if (container != null) renderAnalytics();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderAnalytics() {
        container.removeAllViews();
        AIAnalyticsSummary s = AIAnalyticsService.computeSummary();

        if (s.totalScoredPosts == 0) {
            container.addView(emptyLabel(R.string.ai_no_data));
            return;
        }

        // ── Section 1: AI Overview ────────────────────────────────────────────
        container.addView(sectionHeader(getString(R.string.ai_analytics_overview)));
        container.addView(statRow(
                getString(R.string.ai_total_scored), String.valueOf(s.totalScoredPosts)));
        container.addView(divider());
        container.addView(statRow(
                getString(R.string.ai_hidden_collapsed), String.valueOf(s.hiddenCollapsedPosts)));
        container.addView(divider());
        container.addView(statRow(
                getString(R.string.ai_avg_score),
                String.format("%.1f / 10", s.averageRelevanceScore)));

        // ── Section 2: Filtering Pattern ─────────────────────────────────────
        container.addView(sectionHeader(getString(R.string.ai_filtering_pattern)));
        String topCat = (s.mostFilteredCategory == null || s.mostFilteredCategory.equals("N/A"))
                ? getString(R.string.ai_no_category)
                : s.mostFilteredCategory;
        container.addView(statRow(getString(R.string.ai_most_filtered_category), topCat));

        if (!s.filteredCategoryCounts.isEmpty()) {
            container.addView(divider());
            for (Map.Entry<String, Integer> e :
                    s.filteredCategoryCounts.entrySet().stream()
                            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                            .collect(java.util.stream.Collectors.toList())) {
                container.addView(subRow("  " + e.getKey(), String.valueOf(e.getValue())));
            }
        }

        // ── Section 3: User Feedback ──────────────────────────────────────────
        container.addView(sectionHeader(getString(R.string.ai_user_feedback)));
        container.addView(statRow(
                getString(R.string.ai_feedback_count), String.valueOf(s.userFeedbackCount)));
        container.addView(divider());
        container.addView(statRow(
                getString(R.string.ai_not_relevant_count), String.valueOf(s.notRelevantClicks)));
        container.addView(divider());
        container.addView(statRow(
                getString(R.string.ai_show_anyway_count), String.valueOf(s.showAnywayClicks)));
    }

    // ── Row builders ──────────────────────────────────────────────────────────

    private View sectionHeader(String label) {
        TextView tv = new TextView(this);
        tv.setText(label.toUpperCase());
        tv.setTextSize(12f);
        tv.setTextColor(getColor(R.color.text_secondary));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        int dp = dp(1);
        tv.setPadding(dp * 14, dp * 14, dp * 14, dp * 6);
        return tv;
    }

    private View statRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(11), dp(14), dp(11));

        TextView lv = new TextView(this);
        lv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        lv.setText(label);
        lv.setTextSize(15f);
        lv.setTextColor(getColor(R.color.text_primary));
        row.addView(lv);

        TextView rv = new TextView(this);
        rv.setText(value);
        rv.setTextSize(15f);
        rv.setTextColor(getColor(R.color.accent));
        rv.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(rv);
        return row;
    }

    /** Smaller row for category breakdown. */
    private View subRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(7), dp(14), dp(7));

        TextView lv = new TextView(this);
        lv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        lv.setText(label);
        lv.setTextSize(13f);
        lv.setTextColor(getColor(R.color.text_secondary));
        row.addView(lv);

        TextView rv = new TextView(this);
        rv.setText(value);
        rv.setTextSize(13f);
        rv.setTextColor(getColor(R.color.text_secondary));
        row.addView(rv);
        return row;
    }

    private View emptyLabel(int resId) {
        TextView tv = new TextView(this);
        tv.setText(resId);
        tv.setTextSize(15f);
        tv.setTextColor(getColor(R.color.text_secondary));
        tv.setPadding(dp(16), dp(20), dp(16), dp(20));
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
