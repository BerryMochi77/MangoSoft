package com.example.comp2100miniproject;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.example.comp2100miniproject.auth.AuthManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import dao.model.User;

public class AvatarManager {
    private static final String TAG = "AvatarManager";

    public static final String SOURCE_DEFAULT = "default";
    public static final String SOURCE_GALLERY = "gallery";

    private static final String AVATAR_DIR = "avatars";

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

    /**
     * Copies the picked image into the app's private files dir and stores
     * the resulting local file URI. Photo Picker URIs are temporary and
     * cannot be persisted with {@code takePersistableUriPermission}, so the
     * copy is the only way to keep the avatar visible after a process
     * restart.
     */
    public boolean setGalleryAvatar(Context context, User user, Uri uri) {
        if (context == null || user == null || uri == null) return false;

        File destDir = new File(context.getFilesDir(), AVATAR_DIR);
        if (!destDir.exists() && !destDir.mkdirs()) {
            Log.w(TAG, "Could not create avatar directory: " + destDir);
            return false;
        }
        File destFile = new File(destDir, user.getUUID().toString());

        ContentResolver resolver = context.getContentResolver();
        try (InputStream in = resolver.openInputStream(uri);
             OutputStream out = new FileOutputStream(destFile)) {
            if (in == null) return false;
            byte[] buf = new byte[8 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Failed to copy avatar from " + uri, e);
            return false;
        }

        return authManager.updateAvatar(
                user.getUUID(),
                SOURCE_GALLERY,
                Uri.fromFile(destFile).toString()
        );
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
