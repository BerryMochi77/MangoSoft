package com.example.comp2100miniproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.example.comp2100miniproject.src.PostReportAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Post;
import dao.model.User;
import moderation.AdminModerationService;
import moderation.PostReport;

/**
 * Admin-only screen showing all pending post-level reports.
 *
 * Tapping a report opens a detail dialog with three admin actions:
 * Dismiss, Delete post, Ban author.  All business logic is delegated to
 * {@link AdminModerationService} — this Activity contains only UI code.
 *
 * Access: only users with {@link User.Role#Admin} can reach this screen.
 * Normal users see the Report Post button on post detail pages instead.
 *
 * Message.java is not modified — this is post-level moderation only.
 */
public class AdminPostReportsActivity extends AppCompatActivity {

    private AuthManager authManager;
    private User currentUser;

    private TextView textNoPendingReports;
    private RecyclerView recyclerPostReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_post_reports);

        authManager = new AuthManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null || currentUser.role() != User.Role.Admin) {
            finish();
            return;
        }

        textNoPendingReports = findViewById(R.id.textNoPendingReports);
        recyclerPostReports  = findViewById(R.id.recyclerPostReports);
        recyclerPostReports.setLayoutManager(new LinearLayoutManager(this));

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        loadReports();

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
        loadReports();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadReports() {
        List<PostReport> pending =
                new ArrayList<>(AdminModerationService.getInstance().getPendingReports());

        boolean empty = pending.isEmpty();
        textNoPendingReports.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerPostReports .setVisibility(empty ? View.GONE   : View.VISIBLE);

        recyclerPostReports.setAdapter(
                new PostReportAdapter(pending, this::showReportDetail));
    }

    // ── Detail dialog ─────────────────────────────────────────────────────────

    private void showReportDetail(PostReport report) {
        // Gather display values
        Post   post   = PostDAO.getInstance().get(new Post(report.getPostId()));
        String title  = (post != null && post.topic != null) ? post.topic : "—";
        String body   = (post != null) ? post.getBody() : "";
        String author = userName(report.getReportedAuthorId());
        String reporter = userName(report.getReporterId());
        String reason = report.getReason().isEmpty()
                ? getString(R.string.no_reason_provided) : report.getReason();
        String time = com.example.comp2100miniproject.AppTimeFormatter
                .format(report.getCreatedAt(), this);

        // Build detail view
        View detail = LayoutInflater.from(this)
                .inflate(android.R.layout.simple_list_item_2, null);
        // We build it programmatically for full control
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16 / 2, dp16, 0);

        container.addView(detailRow(getString(R.string.reported_post), title));
        if (!body.isEmpty()) {
            container.addView(detailRow("", body));
        }
        container.addView(detailRow(getString(R.string.reported_author), author));
        container.addView(detailRow(getString(R.string.report_detail_reporter), reporter));
        container.addView(detailRow(getString(R.string.report_reason), reason));
        container.addView(detailRow(getString(R.string.report_time), time));

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_report_detail)
                .setView(container)
                .setNeutralButton(R.string.dismiss_report, (d, w) ->
                        confirmAction(R.string.confirm_dismiss_report,
                                () -> performDismiss(report.getReportId())))
                .setNegativeButton(R.string.delete_post_action, (d, w) ->
                        confirmAction(R.string.confirm_delete_post_report,
                                () -> performDeletePost(report.getReportId())))
                .setPositiveButton(R.string.ban_author, (d, w) ->
                        confirmAction(R.string.confirm_ban_author,
                                () -> performBanAuthor(report.getReportId())))
                .show();
    }

    // ── Admin actions ─────────────────────────────────────────────────────────

    private void performDismiss(String reportId) {
        AdminModerationService.getInstance().dismissReport(reportId, currentUser.getUUID());
        Toast.makeText(this, R.string.report_dismissed, Toast.LENGTH_SHORT).show();
        loadReports();
    }

    private void performDeletePost(String reportId) {
        AdminModerationService.getInstance().deleteReportedPost(reportId, currentUser.getUUID());
        Toast.makeText(this, R.string.post_deleted_by_admin, Toast.LENGTH_SHORT).show();
        loadReports();
    }

    private void performBanAuthor(String reportId) {
        AdminModerationService.getInstance().banReportedAuthor(reportId, currentUser.getUUID());
        Toast.makeText(this, R.string.author_banned, Toast.LENGTH_SHORT).show();
        loadReports();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void confirmAction(int messageResId, Runnable action) {
        new AlertDialog.Builder(this)
                .setMessage(messageResId)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (d, w) -> action.run())
                .show();
    }

    /** Two-line label + value text view pair. */
    private View detailRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        int dp4  = (int) (4  * getResources().getDisplayMetrics().density);
        int dp8  = (int) (8  * getResources().getDisplayMetrics().density);
        row.setPadding(0, dp4, 0, dp8);

        if (!label.isEmpty()) {
            TextView labelView = new TextView(this);
            labelView.setText(label);
            labelView.setTextSize(12f);
            labelView.setTextColor(getColor(R.color.text_secondary));
            row.addView(labelView);
        }

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(15f);
        valueView.setTextColor(getColor(R.color.text_primary));
        row.addView(valueView);

        return row;
    }

    private String userName(UUID id) {
        if (id == null) return "?";
        User u = UserDAO.getInstance().getByUUID(id);
        return u != null ? u.username() : id.toString().substring(0, 8);
    }

    private UUID readCurrentUserId() {
        String v = getIntent().getStringExtra(AuthManager.EXTRA_USER_ID);
        if (v == null) return null;
        try { return UUID.fromString(v); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
