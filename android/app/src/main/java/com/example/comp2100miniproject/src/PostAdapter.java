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

    private final List<Post> posts;
    private final OnPostClick listener;
    private final OnHashtagClick hashtagListener;
    private final AuthManager authManager;
    private final AvatarManager avatarManager;

    public PostAdapter(Context context, List<Post> posts, OnPostClick listener, OnHashtagClick hashtagListener) {
        this.posts = posts;
        this.listener = listener;
        this.hashtagListener = hashtagListener;
        this.authManager = new AuthManager(context);
        this.avatarManager = new AvatarManager(authManager);
    }

    static class VH extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView meta;
        private final TextView edited;
        private final ImageView avatar;
        private final ChipGroup chipGroupHashtags;

        VH(View view) {
            super(view);
            avatar = view.findViewById(R.id.imagePostAvatar);
            title = view.findViewById(R.id.textPostItemTitle);
            meta = view.findViewById(R.id.textPostItemMeta);
            edited = view.findViewById(R.id.textPostEdited);
            chipGroupHashtags = view.findViewById(R.id.chipGroupHashtags);
        }

        void display(Post post, AuthManager authManager, AvatarManager avatarManager, OnHashtagClick hashtagListener) {
            // The hashtags already render as chips below — keep them out of
            // the title to make it readable.
            title.setText(HashtagParser.stripTags(post.topic));
            User user = UserDAO.getInstance().getByUUID(post.poster);
            if (user != null) {
                avatarManager.displayAvatar(user, avatar);
            } else {
                avatar.setImageResource(R.drawable.avatar_default_1);
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
        holder.display(post, authManager, avatarManager, hashtagListener);
        holder.itemView.setOnClickListener(
                v -> listener.onClick(position, post)
        );
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
