package com.example.comp2100miniproject;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.example.comp2100miniproject.auth.AuthManager;

import dao.model.User;

public class AvatarManager {
    public static final String SOURCE_DEFAULT = "default";
    public static final String SOURCE_GALLERY = "gallery";

    private static final AvatarOption[] DEFAULT_AVATARS = {
            new AvatarOption("avatar_default_1", R.string.avatar_default_1, R.drawable.avatar_default_1),
            new AvatarOption("avatar_default_2", R.string.avatar_default_2, R.drawable.avatar_default_2),
            new AvatarOption("avatar_default_3", R.string.avatar_default_3, R.drawable.avatar_default_3),
            new AvatarOption("avatar_default_4", R.string.avatar_default_4, R.drawable.avatar_default_4)
    };

    private final AuthManager authManager;

    public AvatarManager(AuthManager authManager) {
        this.authManager = authManager;
    }

    public AvatarOption[] defaultAvatars() {
        return DEFAULT_AVATARS.clone();
    }

    public void displayAvatar(User user, ImageView imageView) {
        AuthManager.Avatar avatar = authManager.getAvatar(user);
        if (SOURCE_GALLERY.equals(avatar.source())) {
            imageView.setImageDrawable(null);
            imageView.setImageURI(Uri.parse(avatar.value()));
            return;
        }

        imageView.setImageResource(defaultAvatarResId(avatar.value()));
    }

    public boolean setDefaultAvatar(User user, AvatarOption option) {
        if (user == null || option == null) return false;
        return authManager.updateAvatar(user.getUUID(), SOURCE_DEFAULT, option.key());
    }

    public boolean setGalleryAvatar(User user, Uri uri) {
        if (user == null || uri == null) return false;
        return authManager.updateAvatar(user.getUUID(), SOURCE_GALLERY, uri.toString());
    }

    public String[] defaultAvatarLabels(Context context) {
        String[] labels = new String[DEFAULT_AVATARS.length];
        for (int i = 0; i < DEFAULT_AVATARS.length; i++) {
            labels[i] = context.getString(DEFAULT_AVATARS[i].labelResId());
        }
        return labels;
    }

    private int defaultAvatarResId(String key) {
        for (AvatarOption option : DEFAULT_AVATARS) {
            if (option.key().equals(key)) {
                return option.drawableResId();
            }
        }
        return DEFAULT_AVATARS[0].drawableResId();
    }

    public record AvatarOption(String key, int labelResId, int drawableResId) {
    }
}
