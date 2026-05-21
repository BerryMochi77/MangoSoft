package com.example.comp2100miniproject.src;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.AppTimeFormatter;
import com.example.comp2100miniproject.AvatarManager;
import com.example.comp2100miniproject.ComposerFormatManager;
import com.example.comp2100miniproject.R;
import com.example.comp2100miniproject.ThreadIndentView;
import com.example.comp2100miniproject.auth.AuthManager;

import java.util.ArrayList;
import java.util.UUID;

import dao.UserDAO;
import dao.model.Message;
import dao.model.User;
import messagestate.MessageEditRegistry;
import messagestate.MessageReactionRegistry;
import messagestate.MessageThreadRegistry;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    /** Cap visual nesting so deep threads do not slide off the screen on a phone. */
    private static final int MAX_VISUAL_DEPTH = 3;

    public interface OnMessageActionClick {
        void onAction(Message message);
    }

    public interface OnUserClick {
        void onUserClick(User user);
    }

    /** Thumbs-up / thumbs-down tap. Direction is the one the user pressed. */
    public interface OnReactionClick {
        void onReaction(Message message, MessageReactionRegistry.Direction direction);
    }

    /**
     * Overflow ⋮ tap. The host (PostViewerActivity) opens a PopupMenu
     * anchored at {@code anchor}; the host owns the menu items because
     * Save / Report / Edit / Delete each dispatch to existing flows.
     */
    public interface OnOverflowClick {
        void onOverflow(Message message, View anchor);
    }

    private final Message[] messages;
    private final UUID currentUserId;
    private final OnMessageActionClick onReplyClick;
    private final OnReactionClick onReactionClick;
    private final OnOverflowClick onOverflowClick;
    private final OnUserClick onUserClick;
    private final AuthManager authManager;
    private final AvatarManager avatarManager;
    private final int accentColor;
    private final int neutralColor;

    public MessageAdapter(
            Context context,
            ArrayList<Message> dataSet,
            UUID currentUserId,
            OnMessageActionClick onReplyClick,
            OnReactionClick onReactionClick,
            OnOverflowClick onOverflowClick,
            OnUserClick onUserClick
    ) {
        messages = dataSet.toArray(new Message[0]);
        this.currentUserId = currentUserId;
        this.onReplyClick = onReplyClick;
        this.onReactionClick = onReactionClick;
        this.onOverflowClick = onOverflowClick;
        this.onUserClick = onUserClick;
        this.authManager = new AuthManager(context);
        this.avatarManager = new AvatarManager(authManager);
        this.accentColor = ContextCompat.getColor(context, R.color.accent);
        this.neutralColor = ContextCompat.getColor(context, R.color.text_secondary);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ThreadIndentView indent;
        private final ImageView avatar;
        private final TextView author;
        private final TextView timestamp;
        private final TextView content;
        private final ImageView attachment;
        private final ImageButton overflowButton;
        private final LinearLayout actionRow;
        private final ImageButton likeButton;
        private final TextView likeCount;
        private final ImageButton dislikeButton;
        private final TextView dislikeCount;
        private final ImageButton commentButton;

        public ViewHolder(View view) {
            super(view);
            indent = view.findViewById(R.id.threadIndent);
            avatar = view.findViewById(R.id.imageMessageAvatar);
            author = view.findViewById(R.id.textMessageAuthor);
            timestamp = view.findViewById(R.id.textMessageTimestamp);
            content = view.findViewById(R.id.textMessageContent);
            attachment = view.findViewById(R.id.imageMessageAttachment);
            overflowButton = view.findViewById(R.id.buttonMessageOverflow);
            actionRow = view.findViewById(R.id.messageActionRow);
            likeButton = view.findViewById(R.id.buttonLikeMessage);
            likeCount = view.findViewById(R.id.textLikeCount);
            dislikeButton = view.findViewById(R.id.buttonDislikeMessage);
            dislikeCount = view.findViewById(R.id.textDislikeCount);
            commentButton = view.findViewById(R.id.buttonCommentMessage);
        }

        public void display(
                Message message,
                UUID currentUserId,
                OnMessageActionClick onReplyClick,
                OnReactionClick onReactionClick,
                OnOverflowClick onOverflowClick,
                OnUserClick onUserClick,
                AuthManager authManager,
                AvatarManager avatarManager,
                int accentColor,
                int neutralColor
        ) {
            // Thread lines on the left mark nesting depth.
            int depth = MessageThreadRegistry.getInstance().depthOf(message.id());
            indent.setDepth(Math.min(depth, MAX_VISUAL_DEPTH));

            User user = UserDAO.getInstance().getByUUID(message.poster());
            if (user != null) {
                avatarManager.displayAvatar(user, avatar);
                avatar.setOnClickListener(v -> {
                    if (onUserClick != null) onUserClick.onUserClick(user);
                });
                avatar.setClickable(onUserClick != null);
            } else {
                avatar.setImageResource(R.drawable.avatar_default_1);
                avatar.setOnClickListener(null);
                avatar.setClickable(false);
            }
            author.setText(user == null ? "Unknown user" : authManager.getDisplayName(user));

            MessageEditRegistry edits = MessageEditRegistry.getInstance();
            String time = AppTimeFormatter.format(message.timestamp(), itemView.getContext());
            if (edits.isEdited(message.id())) {
                time += " " + itemView.getContext().getString(R.string.edited_label);
            }
            if (message.isHidden()) time += " - hidden from members";
            timestamp.setText(time);

            ComposerFormatManager.bindContent(
                    edits.currentContent(message.id(), message.message()),
                    content,
                    attachment
            );

            bindReactions(message, currentUserId, accentColor, neutralColor);

            likeButton.setOnClickListener(v ->
                    onReactionClick.onReaction(message, MessageReactionRegistry.Direction.LIKE));
            dislikeButton.setOnClickListener(v ->
                    onReactionClick.onReaction(message, MessageReactionRegistry.Direction.DISLIKE));
            commentButton.setOnClickListener(v -> onReplyClick.onAction(message));
            overflowButton.setOnClickListener(v -> onOverflowClick.onOverflow(message, v));
        }

        /** Sync the like / dislike icon tints and counts with the registry. */
        private void bindReactions(Message message, UUID currentUserId,
                                   int accentColor, int neutralColor) {
            MessageReactionRegistry reactions = MessageReactionRegistry.getInstance();
            int likes = reactions.likeCount(message.id());
            int dislikes = reactions.dislikeCount(message.id());
            likeCount.setText(likes == 0 ? "" : String.valueOf(likes));
            dislikeCount.setText(dislikes == 0 ? "" : String.valueOf(dislikes));

            MessageReactionRegistry.Direction mine =
                    reactions.reactionOf(message.id(), currentUserId);
            ImageViewCompat.setImageTintList(likeButton,
                    ColorStateList.valueOf(mine == MessageReactionRegistry.Direction.LIKE
                            ? accentColor : neutralColor));
            ImageViewCompat.setImageTintList(dislikeButton,
                    ColorStateList.valueOf(mine == MessageReactionRegistry.Direction.DISLIKE
                            ? accentColor : neutralColor));
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
                onReplyClick,
                onReactionClick,
                onOverflowClick,
                onUserClick,
                authManager,
                avatarManager,
                accentColor,
                neutralColor
        );
    }

    @Override
    public int getItemCount() {
        return messages.length;
    }
}
