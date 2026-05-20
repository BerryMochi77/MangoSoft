package com.example.comp2100miniproject.src;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.R;
import com.example.comp2100miniproject.auth.AuthManager;

import java.util.Iterator;
import java.util.List;

import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.VH> {
    public interface OnPostClick {
        void onClick(int position, Post post);
    }

    private final List<Post> posts;
    private final OnPostClick listener;
    private final AuthManager authManager;

    public PostAdapter(Context context, List<Post> posts, OnPostClick listener) {
        this.posts = posts;
        this.listener = listener;
        this.authManager = new AuthManager(context);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView meta;
        private final TextView edited;

        VH(View view) {
            super(view);
            title = view.findViewById(R.id.textPostItemTitle);
            meta = view.findViewById(R.id.textPostItemMeta);
            edited = view.findViewById(R.id.textPostEdited);
        }

        void display(Post post, AuthManager authManager) {
            title.setText(post.topic);
            meta.setText("%s - %d messages".formatted(authorName(post, authManager), messageCount(post)));
            edited.setVisibility(post.isEdited() ? View.VISIBLE : View.GONE);
        }

        private String authorName(Post post, AuthManager authManager) {
            User user = UserDAO.getInstance().getByUUID(post.poster);
            return user == null ? "Unknown author" : authManager.getDisplayName(user);
        }

        private int messageCount(Post post) {
            int count = 0;
            Iterator<Message> messages = post.getVisibleMessages(false).getAll();
            while (messages.hasNext()) {
                Message message = messages.next();
                if (!message.isDeleted()) count++;
            }
            return count;
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Post post = posts.get(position);
        holder.display(post, authManager);
        holder.itemView.setOnClickListener(v -> listener.onClick(position, post));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
