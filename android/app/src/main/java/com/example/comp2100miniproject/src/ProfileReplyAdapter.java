package com.example.comp2100miniproject.src;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.AvatarManager;
import com.example.comp2100miniproject.R;
import com.example.comp2100miniproject.auth.AuthManager;

import java.util.List;

import dao.UserDAO;
import dao.model.Message;
import dao.model.User;

public class ProfileReplyAdapter extends RecyclerView.Adapter<ProfileReplyAdapter.VH> {
    public interface OnReplyClick {
        void onClick(Message message);
    }

    private final List<Message> replies;
    private final OnReplyClick listener;
    private final AvatarManager avatarManager;

    public ProfileReplyAdapter(android.content.Context context, List<Message> replies, OnReplyClick listener) {
        this.replies = replies;
        this.listener = listener;
        AuthManager authManager = new AuthManager(context);
        this.avatarManager = new AvatarManager(authManager);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ImageView avatar;
        private final TextView title;
        private final TextView meta;

        VH(View view) {
            super(view);
            avatar = view.findViewById(R.id.imagePostAvatar);
            title = view.findViewById(R.id.textPostItemTitle);
            meta = view.findViewById(R.id.textPostItemMeta);
        }

        void display(Message message, AvatarManager avatarManager) {
            User user = UserDAO.getInstance().getByUUID(message.poster());
            if (user != null) {
                avatarManager.displayAvatar(user, avatar);
            } else {
                avatar.setImageResource(R.drawable.avatar_default_1);
            }
            title.setText(preview(message.message()));
            String time = DateFormat.format("MMM d, HH:mm", message.timestamp()).toString();
            meta.setText(time);
        }

        private String preview(String value) {
            String clean = value == null ? "" : value.trim();
            if (clean.length() <= 90) return clean;
            return clean.substring(0, 87) + "...";
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
        Message reply = replies.get(position);
        holder.display(reply, avatarManager);
        holder.itemView.setOnClickListener(v -> listener.onClick(reply));
    }

    @Override
    public int getItemCount() {
        return replies.size();
    }
}
