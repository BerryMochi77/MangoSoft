package com.example.comp2100miniproject;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.example.comp2100miniproject.auth.AuthManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Curated set of friendly single-codepoint emoji used to render the
     * "default" avatar instead of the bland coloured-disc drawables.
     * Each user's emoji is chosen deterministically from their UUID, so
     * the same user always sees the same character.
     */
    private static final String[] EMOJI_PALETTE = {
            "🦊", // fox
            "🐱", // cat
            "🐼", // panda
            "🐧", // penguin
            "🦁", // lion
            "🐸", // frog
            "🐵", // monkey
            "🦄", // unicorn
            "🐢", // turtle
            "🦋", // butterfly
            "🐝", // bee
            "🦉", // owl
            "🐳", // whale
            "🦝", // raccoon
            "🐶", // dog
            "🦒"  // giraffe
    };

    /** Soft pastel background palette so adjacent avatars look distinct. */
    private static final int[] BG_PALETTE = {
            0xFF6CACE4, 0xFFFFB07A, 0xFFA8E6CF, 0xFFFFD3B6,
            0xFFFFAAA5, 0xFFE0BBE4, 0xFFB5EAD7, 0xFFFFDAC1,
            0xFFC7CEEA, 0xFF95B8D1, 0xFFFEC8D8, 0xFFFFDFD3
    };

    /** Cache of generated bitmaps so RecyclerView scrolling stays smooth. */
    private static final Map<UUID, Bitmap> emojiBitmapCache = new HashMap<>();

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

        // Default avatar: render an emoji-on-coloured-circle bitmap whose
        // emoji and background colour are derived deterministically from
        // the user's UUID. The four legacy drawable options (Mango / Green
        // / Purple / Rose) are intentionally bypassed — they read as
        // robotic placeholders. Users can still upload their own via
        // Photo Picker (handled in the gallery branch above).
        imageView.setImageBitmap(getOrCreateEmojiAvatar(user.getUUID()));
    }

    private Bitmap getOrCreateEmojiAvatar(UUID userId) {
        Bitmap cached = emojiBitmapCache.get(userId);
        if (cached != null) return cached;
        Bitmap bmp = createEmojiAvatar(userId);
        emojiBitmapCache.put(userId, bmp);
        return bmp;
    }

    private static Bitmap createEmojiAvatar(UUID userId) {
        // 256 px keeps the avatar sharp at any size the layouts use today
        // (32 dp in message rows up to ~70 dp in post cards). One bitmap
        // per user ≈ 256 kB so even 100 users would stay under 30 MB.
        final int size = 256;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Hash the two halves of the UUID independently so the emoji and
        // colour don't correlate — feels more random to the eye.
        long hi = userId.getMostSignificantBits();
        long lo = userId.getLeastSignificantBits();
        int emojiIndex = Math.floorMod(hi, EMOJI_PALETTE.length);
        int colorIndex = Math.floorMod(lo, BG_PALETTE.length);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(BG_PALETTE[colorIndex]);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(size * 0.55f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        // Centre the emoji vertically using the font metrics rather than
        // the View's own y/2, so different emoji with different visual
        // heights stay optically centred.
        float baselineY = size / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(EMOJI_PALETTE[emojiIndex], size / 2f, baselineY, textPaint);

        return bmp;
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

    public record AvatarOption(String key, int labelResId, int drawableResId) {
    }
}
