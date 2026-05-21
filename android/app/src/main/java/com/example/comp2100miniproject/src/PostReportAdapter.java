package com.example.comp2100miniproject.src;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.AppTimeFormatter;
import com.example.comp2100miniproject.R;

import java.util.List;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Post;
import dao.model.User;
import moderation.PostReport;

/**
 * RecyclerView adapter for the Admin Post Reports list.
 * Displays each pending post report as a card; tapping calls {@link OnReportClick}.
 */
public class PostReportAdapter extends RecyclerView.Adapter<PostReportAdapter.VH> {

    public interface OnReportClick {
        void onClick(PostReport report);
    }

    private final List<PostReport> reports;
    private final OnReportClick listener;

    public PostReportAdapter(List<PostReport> reports, OnReportClick listener) {
        this.reports  = reports;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textMeta;
        final TextView textReason;
        final TextView textTime;
        final TextView textStatus;

        VH(View v) {
            super(v);
            textTitle  = v.findViewById(R.id.textReportPostTitle);
            textMeta   = v.findViewById(R.id.textReportMeta);
            textReason = v.findViewById(R.id.textReportReason);
            textTime   = v.findViewById(R.id.textReportTime);
            textStatus = v.findViewById(R.id.textReportStatus);
        }

        void bind(PostReport report, OnReportClick listener) {
            // Post title preview
            Post post = PostDAO.getInstance().get(new dao.model.Post(report.getPostId()));
            String title = (post != null && post.topic != null)
                    ? post.topic : report.getPostId().toString();
            textTitle.setText(title);

            // Reporter → Author meta line
            String reporterName = userName(report.getReporterId());
            String authorName   = userName(report.getReportedAuthorId());
            textMeta.setText(
                    itemView.getContext().getString(
                            R.string.report_meta_format, reporterName, authorName));

            // Reason
            String reason = report.getReason();
            textReason.setText(reason.isEmpty()
                    ? itemView.getContext().getString(R.string.no_reason_provided)
                    : reason);

            // Time
            textTime.setText(AppTimeFormatter.format(
                    report.getCreatedAt(), itemView.getContext()));

            // Status badge
            textStatus.setText(itemView.getContext().getString(R.string.status_pending));

            itemView.setOnClickListener(v -> listener.onClick(report));
        }

        private String userName(java.util.UUID id) {
            if (id == null) return "?";
            User u = UserDAO.getInstance().getByUUID(id);
            return u != null ? u.username() : id.toString().substring(0, 8);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_report, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(reports.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }
}
