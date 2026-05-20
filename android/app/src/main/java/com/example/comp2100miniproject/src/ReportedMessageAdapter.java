package com.example.comp2100miniproject.src;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.R;
import com.example.comp2100miniproject.auth.AuthManager;

import java.util.List;

import dao.UserDAO;
import dao.model.Message;
import dao.model.User;

public class ReportedMessageAdapter extends RecyclerView.Adapter<ReportedMessageAdapter.VH> {
    public interface OnActionClick {
        void onAction(Message message);
    }

    public interface OnDetailsClick {
        void onDetails(Message message);
    }

    private final List<Message> messages;
    private final int actionLabel;
    private final OnActionClick onActionClick;
    private final OnDetailsClick onDetailsClick;
    private final AuthManager authManager;

    public ReportedMessageAdapter(
            Context context,
            List<Message> messages,
            int actionLabel,
            OnActionClick onActionClick,
            OnDetailsClick onDetailsClick
    ) {
        this.messages = messages;
        this.actionLabel = actionLabel;
        this.onActionClick = onActionClick;
        this.onDetailsClick = onDetailsClick;
        this.authManager = new AuthManager(context);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final TextView author;
        private final TextView content;
        private final Button actionButton;
        private final Button detailsButton;

        VH(View view) {
            super(view);
            author = view.findViewById(R.id.textReportedAuthor);
            content = view.findViewById(R.id.textReportedContent);
            actionButton = view.findViewById(R.id.buttonHideReported);
            detailsButton = view.findViewById(R.id.buttonDetailsReported);
        }

        void display(
                Message message,
                int actionLabel,
                OnActionClick onActionClick,
                OnDetailsClick onDetailsClick,
                AuthManager authManager
        ) {
            User user = UserDAO.getInstance().getByUUID(message.poster());
            author.setText(user == null ? "Unknown user" : authManager.getDisplayName(user));
            content.setText(message.message());
            actionButton.setText(actionLabel);
            actionButton.setEnabled(true);
            actionButton.setOnClickListener(v -> onActionClick.onAction(message));
            detailsButton.setOnClickListener(v -> onDetailsClick.onDetails(message));
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reported_message, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.display(messages.get(position), actionLabel, onActionClick, onDetailsClick, authManager);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
