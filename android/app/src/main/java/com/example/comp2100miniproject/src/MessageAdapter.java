package com.example.comp2100miniproject.src;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.R;
import com.example.comp2100miniproject.AvatarManager;
import com.example.comp2100miniproject.auth.AuthManager;

import java.util.ArrayList;
import java.util.UUID;

import dao.UserDAO;
import dao.model.Message;
import dao.model.User;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    public interface OnReportClick {
        void onReport(Message message);
    }

    public interface OnMessageActionClick {
        void onAction(Message message);
    }

    private final Message[] messages;
    private final UUID currentUserId;
    private final OnReportClick onReportClick;
    private final OnMessageActionClick onEditClick;
    private final OnMessageActionClick onDeleteClick;
    private final AuthManager authManager;
    private final AvatarManager avatarManager;

    public MessageAdapter(
            Context context,
            ArrayList<Message> dataSet,
            UUID currentUserId,
            OnReportClick onReportClick,
            OnMessageActionClick onEditClick,
            OnMessageActionClick onDeleteClick
    ) {
        messages = dataSet.toArray(new Message[0]);
        this.currentUserId = currentUserId;
        this.onReportClick = onReportClick;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
        this.authManager = new AuthManager(context);
        this.avatarManager = new AvatarManager(authManager);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatar;
        private final TextView author;
        private final TextView timestamp;
        private final TextView content;
        private final Button reportButton;
        private final LinearLayout ownerActions;
        private final Button editButton;
        private final Button deleteButton;

        public ViewHolder(View view) {
            super(view);
            avatar = view.findViewById(R.id.imageMessageAvatar);
            author = view.findViewById(R.id.textMessageAuthor);
            timestamp = view.findViewById(R.id.textMessageTimestamp);
            content = view.findViewById(R.id.textMessageContent);
            reportButton = view.findViewById(R.id.buttonReportMessage);
            ownerActions = view.findViewById(R.id.messageOwnerActions);
            editButton = view.findViewById(R.id.buttonEditMessage);
            deleteButton = view.findViewById(R.id.buttonDeleteMessage);
        }

        public void display(
                Message message,
                UUID currentUserId,
                OnReportClick onReportClick,
                OnMessageActionClick onEditClick,
                OnMessageActionClick onDeleteClick,
                AuthManager authManager,
                AvatarManager avatarManager
        ) {
            User user = UserDAO.getInstance().getByUUID(message.poster());
            if (user != null) {
                avatarManager.displayAvatar(user, avatar);
            } else {
                avatar.setImageResource(R.drawable.avatar_default_1);
            }
            author.setText(user == null ? "Unknown user" : authManager.getDisplayName(user));

            String time = DateFormat.format("MMM d, HH:mm", message.timestamp()).toString();
            if (message.isEdited()) time += " " + itemView.getContext().getString(R.string.edited_label);
            if (message.isHidden()) time += " - hidden from members";
            timestamp.setText(time);

            content.setText(message.message());
            boolean mine = currentUserId != null && currentUserId.equals(message.poster());
            reportButton.setVisibility(mine ? View.GONE : View.VISIBLE);
            ownerActions.setVisibility(mine ? View.VISIBLE : View.GONE);
            reportButton.setOnClickListener(v -> onReportClick.onReport(message));
            editButton.setOnClickListener(v -> onEditClick.onAction(message));
            deleteButton.setOnClickListener(v -> onDeleteClick.onAction(message));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_message, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.display(
                messages[position],
                currentUserId,
                onReportClick,
                onEditClick,
                onDeleteClick,
                authManager,
                avatarManager
        );
    }

    @Override
    public int getItemCount() {
        return messages.length;
    }
}
