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

public class ProfileBackgroundManager {
    private static final String TAG = "ProfileBackgroundManager";

    public static final String SOURCE_DEFAULT = "default";
    public static final String SOURCE_GALLERY = "gallery";

    private static final String BACKGROUND_DIR = "profile_backgrounds";

    private static final BackgroundOption[] DEFAULT_BACKGROUNDS = {
            new BackgroundOption("profile_background_default_1", R.string.profile_background_default_1, R.drawable.profile_background_default_1),
            new BackgroundOption("profile_background_default_2", R.string.profile_background_default_2, R.drawable.profile_background_default_2),
            new BackgroundOption("profile_background_default_3", R.string.profile_background_default_3, R.drawable.profile_background_default_3)
    };

    private final AuthManager authManager;

    public ProfileBackgroundManager(AuthManager authManager) {
        this.authManager = authManager;
    }

    public BackgroundOption[] defaultBackgrounds() {
        return DEFAULT_BACKGROUNDS.clone();
    }

    public String[] defaultBackgroundLabels(Context context) {
        String[] labels = new String[DEFAULT_BACKGROUNDS.length];
        for (int i = 0; i < DEFAULT_BACKGROUNDS.length; i++) {
            labels[i] = context.getString(DEFAULT_BACKGROUNDS[i].labelResId());
        }
        return labels;
    }

    public void displayBackground(User user, ImageView imageView) {
        AuthManager.ProfileBackground background = authManager.getProfileBackground(user);
        if (SOURCE_GALLERY.equals(background.source())) {
            imageView.setImageDrawable(null);
            imageView.setImageURI(Uri.parse(background.value()));
            return;
        }

        imageView.setImageResource(defaultBackgroundResId(background.value()));
    }

    public boolean setDefaultBackground(User user, BackgroundOption option) {
        if (user == null || option == null) return false;
        return authManager.updateProfileBackground(user.getUUID(), SOURCE_DEFAULT, option.key());
    }

    public boolean setGalleryBackground(Context context, User user, Uri uri) {
        if (context == null || user == null || uri == null) return false;

        File destDir = new File(context.getFilesDir(), BACKGROUND_DIR);
        if (!destDir.exists() && !destDir.mkdirs()) {
            Log.w(TAG, "Could not create profile background directory: " + destDir);
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
            Log.w(TAG, "Failed to copy profile background from " + uri, e);
            return false;
        }

        return authManager.updateProfileBackground(
                user.getUUID(),
                SOURCE_GALLERY,
                Uri.fromFile(destFile).toString()
        );
    }

    private int defaultBackgroundResId(String key) {
        for (BackgroundOption option : DEFAULT_BACKGROUNDS) {
            if (option.key().equals(key)) {
                return option.drawableResId();
            }
        }
        return DEFAULT_BACKGROUNDS[0].drawableResId();
    }

    public record BackgroundOption(String key, int labelResId, int drawableResId) {
    }
}
