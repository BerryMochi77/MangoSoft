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
import com.example.comp2100miniproject.UiFontManager;
import com.example.comp2100miniproject.auth.AuthManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    /**
     * How many of a message's direct replies are currently collapsed
     * (hidden) beneath it. 0 means none are hidden — the indicator stays off.
     */
    public interface HiddenReplyCount {
        int hiddenReplies(Message message);
    }

    private final List<Message> items;
    private final UUID currentUserId;
    private final OnMessageActionClick onReplyClick;
    private final OnReactionClick onReactionClick;
    private final OnOverflowClick onOverflowClick;
    private final OnUserClick onUserClick;
    private final OnMessageActionClick onContentClick;
    private final HiddenReplyCount hiddenReplyCount;
    private final AuthManager authManager;
    private final AvatarManager avatarManager;
    private final int accentColor;
    private final int neutralColor;

    public MessageAdapter(
            Context context,
            List<Message> items,
            UUID currentUserId,
            OnMessageActionClick onReplyClick,
            OnReactionClick onReactionClick,
            OnOverflowClick onOverflowClick,
            OnUserClick onUserClick,
            OnMessageActionClick onContentClick,
            HiddenReplyCount hiddenReplyCount
    ) {
        this.items = new ArrayList<>(items);
        this.currentUserId = currentUserId;
        this.onReplyClick = onReplyClick;
        this.onReactionClick = onReactionClick;
        this.onOverflowClick = onOverflowClick;
        this.onUserClick = onUserClick;
        this.onContentClick = onContentClick;
        this.hiddenReplyCount = hiddenReplyCount;
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
        private final TextView collapsedReplies;
        private final View divider;

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
            collapsedReplies = view.findViewById(R.id.textCollapsedReplies);
            divider = view.findViewById(R.id.messageDivider);
        }

        public void display(
                Message message,
                UUID currentUserId,
                OnMessageActionClick onReplyClick,
                OnReactionClick onReactionClick,
                OnOverflowClick onOverflowClick,
                OnUserClick onUserClick,
                OnMessageActionClick onContentClick,
                HiddenReplyCount hiddenReplyCount,
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
            if (onContentClick != null) {
                View.OnClickListener toggle = v -> onContentClick.onAction(message);
                content.setOnClickListener(toggle);
                attachment.setOnClickListener(toggle);
            } else {
                content.setOnClickListener(null);
                content.setClickable(false);
                attachment.setOnClickListener(null);
                attachment.setClickable(false);
            }

            int hidden = hiddenReplyCount == null ? 0 : hiddenReplyCount.hiddenReplies(message);
            if (hidden > 0) {
                collapsedReplies.setText(itemView.getResources()
                        .getQuantityString(R.plurals.collapsed_reply_count, hidden, hidden));
                collapsedReplies.setVisibility(View.VISIBLE);
                collapsedReplies.setOnClickListener(onContentClick == null
                        ? null : v -> onContentClick.onAction(message));
            } else {
                collapsedReplies.setVisibility(View.GONE);
                collapsedReplies.setOnClickListener(null);
            }

            divider.setVisibility(View.VISIBLE);
        }

        /** Sync the like / dislike icon tints and counts with the registry. */
        private void bindReactions(Message message, UUID currentUserId,
                                   int accentColor, int neutralColor) {
            MessageReactionRegistry reactions = MessageReactionRegistry.getInstance();
            int likes = reactions.likeCount(message.id());
            int dislikes = reactions.dislikeCount(message.id());
            likeCount.setText(likes == 0 ? "" : formatCount(likes));
            dislikeCount.setText(dislikes == 0 ? "" : formatCount(dislikes));

            MessageReactionRegistry.Direction mine =
                    reactions.reactionOf(message.id(), currentUserId);
            ImageViewCompat.setImageTintList(likeButton,
                    ColorStateList.valueOf(mine == MessageReactionRegistry.Direction.LIKE
                            ? accentColor : neutralColor));
            ImageViewCompat.setImageTintList(dislikeButton,
                    ColorStateList.valueOf(mine == MessageReactionRegistry.Direction.DISLIKE
                            ? accentColor : neutralColor));
        }

        /** Compact reaction counts: 1000 -> "1k", 1200 -> "1.2k", 1_000_000 -> "1m". */
        private static String formatCount(int count) {
            if (count < 1000) return String.valueOf(count);
            if (count < 1_000_000) return trimTrailingZero(count / 1000.0) + "k";
            return trimTrailingZero(count / 1_000_000.0) + "m";
        }

        private static String trimTrailingZero(double value) {
            String text = String.format(Locale.US, "%.1f", value);
            return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_message, viewGroup, false);
        UiFontManager.applyToViewTree(viewGroup.getContext(), view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.display(
                items.get(position),
                currentUserId,
                onReplyClick,
                onReactionClick,
                onOverflowClick,
                onUserClick,
                onContentClick,
                hiddenReplyCount,
                authManager,
                avatarManager,
                accentColor,
                neutralColor
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
