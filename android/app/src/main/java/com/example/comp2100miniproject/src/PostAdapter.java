package com.example.comp2100miniproject.src;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.R;
import com.example.comp2100miniproject.AvatarManager;
import com.example.comp2100miniproject.ComposerFormatManager;
import com.example.comp2100miniproject.auth.AuthManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Iterator;
import java.util.List;

import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.VH> {

    public interface OnPostClick {
        void onClick(int position, Post post);
    }

    /** Called when the user taps a hashtag chip on a post card. */
    public interface OnHashtagClick {
        void onHashtagClick(String tag);
    }

    /** Called when the user taps the author avatar on a post card. */
    public interface OnUserClick {
        void onUserClick(User user);
    }

    private final List<Post> posts;
    private final OnPostClick listener;
    private final OnHashtagClick hashtagListener;
    private final OnUserClick userClickListener;
    private final AuthManager authManager;
    private final AvatarManager avatarManager;

    public PostAdapter(Context context, List<Post> posts, OnPostClick listener, OnHashtagClick hashtagListener) {
        this(context, posts, listener, hashtagListener, null);
    }

    public PostAdapter(
            Context context,
            List<Post> posts,
            OnPostClick listener,
            OnHashtagClick hashtagListener,
            OnUserClick userClickListener
    ) {
        this.posts = posts;
        this.listener = listener;
        this.hashtagListener = hashtagListener;
        this.userClickListener = userClickListener;
        this.authManager = new AuthManager(context);
        this.avatarManager = new AvatarManager(authManager);
    }

    static class VH extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView meta;
        private final TextView edited;
        private final TextView body;
        private final ImageView avatar;
        private final ImageView attachment;
        private final ChipGroup chipGroupHashtags;

        VH(View view) {
            super(view);
            avatar = view.findViewById(R.id.imagePostAvatar);
            title = view.findViewById(R.id.textPostItemTitle);
            meta = view.findViewById(R.id.textPostItemMeta);
            edited = view.findViewById(R.id.textPostEdited);
            body = view.findViewById(R.id.textPostItemBody);
            chipGroupHashtags = view.findViewById(R.id.chipGroupHashtags);
            attachment = view.findViewById(R.id.imagePostItemAttachment);
        }

        void display(
                Post post,
                AuthManager authManager,
                AvatarManager avatarManager,
                OnHashtagClick hashtagListener,
                OnUserClick userClickListener
        ) {
            // The hashtags already render as chips below — keep them out of
            // the title to make it readable.
            title.setText(HashtagParser.stripTags(post.topic));
            User user = UserDAO.getInstance().getByUUID(post.poster);
            if (user != null) {
                avatarManager.displayAvatar(user, avatar);
                avatar.setOnClickListener(v -> {
                    if (userClickListener != null) {
                        userClickListener.onUserClick(user);
                    }
                });
                avatar.setClickable(userClickListener != null);
            } else {
                avatar.setImageResource(R.drawable.avatar_default_1);
                avatar.setOnClickListener(null);
                avatar.setClickable(false);
            }

            meta.setText(
                    String.format(
                            "%s - %d messages",
                            authorName(user, authManager),
                            messageCount(post)
                    )
            );

            edited.setVisibility(
                    post.isEdited()
                            ? View.VISIBLE
                            : View.GONE
            );
            ComposerFormatManager.bindContent(post.getBody(), body, attachment);

            bindHashtags(post, hashtagListener);
        }

        private void bindHashtags(Post post, OnHashtagClick hashtagListener) {
            chipGroupHashtags.removeAllViews();
            List<String> tags = post.getHashtags();
            if (tags.isEmpty()) {
                chipGroupHashtags.setVisibility(View.GONE);
                return;
            }
            chipGroupHashtags.setVisibility(View.VISIBLE);
            Context ctx = chipGroupHashtags.getContext();
            for (String tag : tags) {
                Chip chip = new Chip(ctx);
                chip.setText("#" + tag);
                chip.setClickable(true);
                chip.setFocusable(true);
                chip.setChipBackgroundColorResource(R.color.chip_hashtag_bg);
                chip.setTextColor(ctx.getColor(R.color.accent));
                chip.setTextSize(11f);
                chip.setEnsureMinTouchTargetSize(false);
                if (hashtagListener != null) {
                    chip.setOnClickListener(v -> hashtagListener.onHashtagClick(tag));
                }
                chipGroupHashtags.addView(chip);
            }
        }

        private String authorName(User user, AuthManager authManager) {
            return user == null
                    ? "Unknown author"
                    : authManager.getDisplayName(user);
        }

        private int messageCount(Post post) {
            // getVisibleMessages already consults MessageDeletionRegistry
            // and filters hidden/deleted, so we just count what it returns.
            int count = 0;
            Iterator<Message> messages = post.getVisibleMessages(false).getAll();
            while (messages.hasNext()) {
                messages.next();
                count++;
            }
            return count;
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_post,
                                parent,
                                false
                        );
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull VH holder,
            int position
    ) {
        Post post = posts.get(position);
        holder.display(post, authManager, avatarManager, hashtagListener, userClickListener);
        holder.itemView.setOnClickListener(
                v -> listener.onClick(position, post)
        );
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
