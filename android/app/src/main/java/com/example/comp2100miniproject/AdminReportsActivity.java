package com.example.comp2100miniproject;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.auth.AuthManager;
import com.example.comp2100miniproject.moderation.FrozenUserManager;
import com.example.comp2100miniproject.src.ReportedMessageAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import dao.model.Message;
import dao.model.User;
import dao.UserDAO;
import moderation.ModerationTools;
import moderation.Report;
import moderation.ReportRegistry;

public class AdminReportsActivity extends AppCompatActivity {
    private AuthManager authManager;
    private FrozenUserManager frozenUserManager;
    private User currentUser;
    private RecyclerView recyclerPendingReports;
    private RecyclerView recyclerProcessedReports;
    private TextView textNoPendingReports;
    private TextView textNoProcessedReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_reports);

        authManager = new AuthManager(this);
        frozenUserManager = new FrozenUserManager(this);
        currentUser = authManager.getUser(readCurrentUserId());
        if (currentUser == null || currentUser.role() != User.Role.Admin) {
            finish();
            return;
        }

        recyclerPendingReports = findViewById(R.id.recyclerPendingReports);
        recyclerProcessedReports = findViewById(R.id.recyclerProcessedReports);
        recyclerPendingReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerProcessedReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerPendingReports.setNestedScrollingEnabled(false);
        recyclerProcessedReports.setNestedScrollingEnabled(false);

        textNoPendingReports = findViewById(R.id.textNoPendingReports);
        textNoProcessedReports = findViewById(R.id.textNoProcessedReports);

        Button buttonBack = findViewById(R.id.buttonAdminBack);
        buttonBack.setOnClickListener(v -> finish());
        Button buttonFrozenUsers = findViewById(R.id.buttonFrozenUsers);
        buttonFrozenUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, FrozenUsersActivity.class);
            intent.putExtra(AuthManager.EXTRA_USER_ID, currentUser.getUUID().toString());
            intent.putExtra(AuthManager.EXTRA_IS_ADMIN, true);
            startActivity(intent);
        });

        loadReports();
    }

    private void loadReports() {
        ArrayList<Message> pending = new ArrayList<>();
        ArrayList<Message> processed = new ArrayList<>();
        Iterator<Message> iterator = ModerationTools.getReportedMessages("MOST", 100);
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (message.isDeleted()) continue;
            if (message.isHidden()) {
                processed.add(message);
            } else {
                pending.add(message);
            }
        }

        textNoPendingReports.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerPendingReports.setVisibility(pending.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerPendingReports.setAdapter(new ReportedMessageAdapter(
                this,
                pending,
                R.string.hide_message,
                message -> {
                    boolean hidden = ModerationTools.setHidden(message.id(), currentUser.getUUID(), true);
                    Toast.makeText(
                            AdminReportsActivity.this,
                            hidden ? R.string.message_hidden : R.string.report_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                    loadReports();
                },
                this::showReportDetails
        ));

        textNoProcessedReports.setVisibility(processed.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerProcessedReports.setVisibility(processed.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerProcessedReports.setAdapter(new ReportedMessageAdapter(
                this,
                processed,
                R.string.unhide_message,
                message -> {
                    boolean unhidden = ModerationTools.setHidden(message.id(), currentUser.getUUID(), false);
                    Toast.makeText(
                            AdminReportsActivity.this,
                            unhidden ? R.string.message_unhidden : R.string.report_failed,
                            Toast.LENGTH_SHORT
                    ).show();
                    loadReports();
                },
                this::showReportDetails
        ));
    }

    private void showReportDetails(Message message) {
        ArrayList<Report> reports = reportsFor(message);
        if (reports.isEmpty()) return;

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, 0, padding, 0);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Report report : reports) {
            View item = inflater.inflate(R.layout.dialog_report_detail_item, content, false);
            TextView reporterView = item.findViewById(R.id.textDetailReporter);
            TextView timeView = item.findViewById(R.id.textDetailTime);
            TextView reasonView = item.findViewById(R.id.textDetailReason);
            Button toggleButton = item.findViewById(R.id.buttonToggleFreeze);

            bindReportDetailItem(report, reporterView, timeView, reasonView, toggleButton);
            content.addView(item);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.report_details)
                .setView(content)
                .setPositiveButton(R.string.back, null)
                .show();
    }

    private void bindReportDetailItem(
            Report report,
            TextView reporterView,
            TextView timeView,
            TextView reasonView,
            Button toggleButton
    ) {
        User reporter = UserDAO.getInstance().getByUUID(report.user());
        String reporterName = reporter == null ? report.user().toString() : authManager.getDisplayName(reporter);
        boolean frozen = frozenUserManager.isFrozen(report.user());
        reporterView.setText(frozen ? reporterName + " (frozen)" : reporterName);
        timeView.setText(DateFormat.format("MMM d, HH:mm", report.timestamp()).toString());
        reasonView.setText(report.reason() == null || report.reason().isBlank()
                ? "No detail provided"
                : report.reason());
        toggleButton.setText(frozen ? R.string.unfreeze_reporter : R.string.freeze_reporter);
        toggleButton.setOnClickListener(v -> {
            toggleReporterFreeze(report.user());
            bindReportDetailItem(report, reporterView, timeView, reasonView, toggleButton);
        });
    }

    private ArrayList<Report> reportsFor(Message message) {
        ArrayList<Report> reports = new ArrayList<>();
        Iterator<Report> iterator = ReportRegistry.getInstance().getAllReports();
        while (iterator.hasNext()) {
            Report report = iterator.next();
            if (message.id().equals(report.message())) reports.add(report);
        }
        return reports;
    }

    private void toggleReporterFreeze(UUID reporterId) {
        boolean frozen = frozenUserManager.isFrozen(reporterId);
        if (frozen) {
            frozenUserManager.unfreeze(reporterId);
        } else {
            frozenUserManager.freeze(reporterId);
        }
        Toast.makeText(
                this,
                frozen ? R.string.reporter_unfrozen : R.string.reporter_frozen,
                Toast.LENGTH_SHORT
        ).show();
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
}
