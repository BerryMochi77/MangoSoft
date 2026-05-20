package com.example.comp2100miniproject.src;

import android.content.Context;

import com.example.comp2100miniproject.AppTimeFormatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.R;
import com.example.comp2100miniproject.AvatarManager;
import com.example.comp2100miniproject.ComposerFormatManager;
import com.example.comp2100miniproject.ThreadIndentView;
import com.example.comp2100miniproject.auth.AuthManager;

import java.util.ArrayList;
import java.util.UUID;

import dao.UserDAO;
import dao.model.Message;
import dao.model.User;
import messagestate.MessageEditRegistry;
import messagestate.MessageThreadRegistry;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    /** Cap visual nesting so deep threads do not slide off the screen on a phone. */
    private static final int MAX_VISUAL_DEPTH = 5;

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
    private final OnMessageActionClick onReplyClick;
    private final AuthManager authManager;
    private final AvatarManager avatarManager;

    public MessageAdapter(
            Context context,
            ArrayList<Message> dataSet,
            UUID currentUserId,
            OnReportClick onReportClick,
            OnMessageActionClick onEditClick,
            OnMessageActionClick onDeleteClick,
            OnMessageActionClick onReplyClick
    ) {
        messages = dataSet.toArray(new Message[0]);
        this.currentUserId = currentUserId;
        this.onReportClick = onReportClick;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
        this.onReplyClick = onReplyClick;
        this.authManager = new AuthManager(context);
        this.avatarManager = new AvatarManager(authManager);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ThreadIndentView indent;
        private final ImageView avatar;
        private final TextView author;
        private final TextView timestamp;
        private final TextView content;
        private final ImageView attachment;
        private final ImageButton replyButton;
        private final ImageButton reportButton;
        private final LinearLayout ownerActions;
        private final Button editButton;
        private final Button deleteButton;

        public ViewHolder(View view) {
            super(view);
            indent = view.findViewById(R.id.threadIndent);
            avatar = view.findViewById(R.id.imageMessageAvatar);
            author = view.findViewById(R.id.textMessageAuthor);
            timestamp = view.findViewById(R.id.textMessageTimestamp);
            content = view.findViewById(R.id.textMessageContent);
            attachment = view.findViewById(R.id.imageMessageAttachment);
            replyButton = view.findViewById(R.id.buttonReplyMessage);
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
                OnMessageActionClick onReplyClick,
                AuthManager authManager,
                AvatarManager avatarManager
        ) {
            // Thread lines on the left mark nesting depth, the way Reddit
            // visualises a comment tree. Cap so deep threads still fit on
            // a phone.
            int depth = MessageThreadRegistry.getInstance().depthOf(message.id());
            indent.setDepth(Math.min(depth, MAX_VISUAL_DEPTH));

            User user = UserDAO.getInstance().getByUUID(message.poster());
            if (user != null) {
                avatarManager.displayAvatar(user, avatar);
            } else {
                avatar.setImageResource(R.drawable.avatar_default_1);
            }
            author.setText(user == null ? "Unknown user" : authManager.getDisplayName(user));

            MessageEditRegistry edits = MessageEditRegistry.getInstance();
            String time = AppTimeFormatter.format(message.timestamp(), itemView.getContext());
            if (edits.isEdited(message.id())) time += " " + itemView.getContext().getString(R.string.edited_label);
            if (message.isHidden()) time += " - hidden from members";
            timestamp.setText(time);

            ComposerFormatManager.bindContent(
                    edits.currentContent(message.id(), message.message()),
                    content,
                    attachment
            );
            boolean mine = currentUserId != null && currentUserId.equals(message.poster());
            // Reply is available to everyone, including the author.
            replyButton.setVisibility(View.VISIBLE);
            replyButton.setOnClickListener(v -> onReplyClick.onAction(message));
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
                onReplyClick,
                authManager,
                avatarManager
        );
    }

    @Override
    public int getItemCount() {
        return messages.length;
    }
}
