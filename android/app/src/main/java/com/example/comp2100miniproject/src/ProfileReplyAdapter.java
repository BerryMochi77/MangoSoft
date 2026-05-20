package com.example.comp2100miniproject.src;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.R;

import java.util.List;

import dao.model.Message;

public class ProfileReplyAdapter extends RecyclerView.Adapter<ProfileReplyAdapter.VH> {
    public interface OnReplyClick {
        void onClick(Message message);
    }

    private final List<Message> replies;
    private final OnReplyClick listener;

    public ProfileReplyAdapter(List<Message> replies, OnReplyClick listener) {
        this.replies = replies;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView meta;

        VH(View view) {
            super(view);
            title = view.findViewById(R.id.textPostItemTitle);
            meta = view.findViewById(R.id.textPostItemMeta);
        }

        void display(Message message) {
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
        holder.display(reply);
        holder.itemView.setOnClickListener(v -> listener.onClick(reply));
    }

    @Override
    public int getItemCount() {
        return replies.size();
    }
}
