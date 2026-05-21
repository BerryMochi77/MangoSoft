package com.example.comp2100miniproject;

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

import dao.model.User;
import moderation.BanRepository;

/**
 * Admin management list of users.
 *
 * MODE_ALL     → alphabetical User Database.
 * MODE_RANKING → sorted by post count (Active Users Ranking).
 *
 * Tapping a user opens a detail dialog with ban/unban action.
 * All data comes from {@link AdminAnalyticsService} — no repository calls in this class.
 * Message.java is not modified.
 */
public class AdminUserListActivity extends AppCompatActivity {

    public static final String EXTRA_MODE   = "mode";
    public static final String MODE_ALL     = "all";
    public static final String MODE_RANKING = "ranking";

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
        title.setText(MODE_RANKING.equals(mode)
                ? R.string.dash_active_users_ranking
                : R.string.dash_user_database);

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
        List<UserAdminSummary> users = MODE_RANKING.equals(mode)
                ? AdminAnalyticsService.getActiveUsersRankingFull()
                : AdminAnalyticsService.getAllUsersForAdmin();

        if (users.isEmpty()) {
            container.addView(emptyLabel(R.string.dash_no_data));
            return;
        }
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) container.addView(divider());
            container.addView(userRow(i + 1, users.get(i)));
        }
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    private View userRow(int rank, UserAdminSummary u) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setClickable(true);
        row.setFocusable(true);

        if (MODE_RANKING.equals(mode)) {
            TextView rankTv = new TextView(this);
            rankTv.setText(String.valueOf(rank));
            rankTv.setTextSize(14f);
            rankTv.setTextColor(getColor(R.color.text_secondary));
            rankTv.setMinWidth(dp(28));
            row.addView(rankTv);
        }

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameTv = new TextView(this);
        String nameLabel = u.username + (u.banned
                ? " [" + getString(R.string.dash_banned) + "]" : "");
        nameTv.setText(nameLabel);
        nameTv.setTextSize(15f);
        nameTv.setTextColor(u.banned ? getColor(R.color.warning) : getColor(R.color.text_primary));
        nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
        info.addView(nameTv);

        TextView metaTv = new TextView(this);
        metaTv.setText(getString(R.string.dash_user_meta, u.role, u.postCount, u.replyCount));
        metaTv.setTextSize(13f);
        metaTv.setTextColor(getColor(R.color.text_secondary));
        info.addView(metaTv);

        row.addView(info);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(getColor(R.color.text_secondary));
        arrow.setTextSize(18f);
        row.addView(arrow);

        row.setOnClickListener(v -> showUserDetail(u));
        return row;
    }

    // ── User detail dialog ────────────────────────────────────────────────────

    private void showUserDetail(UserAdminSummary u) {
        boolean isBanned = BanRepository.getInstance().isBanned(u.userId);
        String details =
                getString(R.string.dash_detail_username) + ": " + u.username + "\n" +
                getString(R.string.dash_detail_role)     + ": " + u.role     + "\n" +
                getString(R.string.dash_total_posts)     + ": " + u.postCount + "\n" +
                getString(R.string.dash_total_replies)   + ": " + u.replyCount + "\n" +
                getString(R.string.dash_detail_status)   + ": " +
                        getString(isBanned ? R.string.dash_banned : R.string.dash_active);

        new AlertDialog.Builder(this)
                .setTitle(u.username)
                .setMessage(details)
                .setNeutralButton(R.string.cancel, null)
                .setPositiveButton(
                        isBanned ? R.string.dash_unban_user : R.string.ban_author,
                        (d, w) -> {
                            if (isBanned) {
                                BanRepository.getInstance().unban(u.userId);
                                Toast.makeText(this, R.string.dash_user_unbanned, Toast.LENGTH_SHORT).show();
                            } else {
                                BanRepository.getInstance().ban(u.userId);
                                Toast.makeText(this, R.string.author_banned, Toast.LENGTH_SHORT).show();
                            }
                            loadList();
                        })
                .show();
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
