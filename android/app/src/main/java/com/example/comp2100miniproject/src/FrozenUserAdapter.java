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
import java.util.UUID;

import dao.UserDAO;
import dao.model.User;

public class FrozenUserAdapter extends RecyclerView.Adapter<FrozenUserAdapter.VH> {
    public interface OnUnfreezeClick {
        void onUnfreeze(UUID userId);
    }

    private final List<UUID> userIds;
    private final OnUnfreezeClick onUnfreezeClick;
    private final AuthManager authManager;

    public FrozenUserAdapter(Context context, List<UUID> userIds, OnUnfreezeClick onUnfreezeClick) {
        this.userIds = userIds;
        this.onUnfreezeClick = onUnfreezeClick;
        this.authManager = new AuthManager(context);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final TextView username;
        private final Button unfreezeButton;

        VH(View view) {
            super(view);
            username = view.findViewById(R.id.textFrozenUsername);
            unfreezeButton = view.findViewById(R.id.buttonUnfreezeUser);
        }

        void display(UUID userId, OnUnfreezeClick onUnfreezeClick, AuthManager authManager) {
            User user = UserDAO.getInstance().getByUUID(userId);
            username.setText(user == null ? userId.toString() : authManager.getDisplayName(user));
            unfreezeButton.setOnClickListener(v -> onUnfreezeClick.onUnfreeze(userId));
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_frozen_user, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.display(userIds.get(position), onUnfreezeClick, authManager);
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }
}
