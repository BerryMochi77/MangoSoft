package com.example.comp2100miniproject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ComposerFormatManager {
    private static final String TAG = "ComposerFormatManager";
    private static final String ATTACHMENT_DIR = "composer_attachments";
    private static final String DEMO_IMAGE_PREFIX = "demo:";
    private static final Pattern IMAGE_TOKEN = Pattern.compile("\\[\\[image:([^\\]]+)\\]\\]");

    private ComposerFormatManager() {
    }

    public static Uri copyImage(Context context, Uri sourceUri) {
        if (context == null || sourceUri == null) return null;

        File destDir = new File(context.getFilesDir(), ATTACHMENT_DIR);
        if (!destDir.exists() && !destDir.mkdirs()) {
            Log.w(TAG, "Could not create composer attachment directory: " + destDir);
            return null;
        }

        File destFile = new File(destDir, "image_" + UUID.randomUUID() + ".bin");
        ContentResolver resolver = context.getContentResolver();
        try (InputStream in = resolver.openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destFile)) {
            if (in == null) return null;
            byte[] buf = new byte[8 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Failed to copy composer image from " + sourceUri, e);
            return null;
        }

        return Uri.fromFile(destFile);
    }

    public static void insertImage(EditText input, Uri imageUri) {
        if (input == null || imageUri == null) return;
        insertText(input, "\n[[image:" + imageUri + "]]\n");
    }

    public static void insertEmoji(EditText input, String emoji) {
        if (input == null || emoji == null || emoji.isEmpty()) return;
        insertText(input, emoji);
    }

    public static void bindContent(String rawContent, TextView textView, ImageView imageView) {
        String text = stripImageTokens(rawContent).trim();
        String imageRef = firstImageRef(rawContent);

        if (text.isEmpty()) {
            textView.setVisibility(TextView.GONE);
        } else {
            textView.setVisibility(TextView.VISIBLE);
            textView.setText(text);
        }

        if (imageRef == null) {
            imageView.setVisibility(ImageView.GONE);
            imageView.setImageDrawable(null);
            imageView.setOnClickListener(null);
        } else {
            imageView.setVisibility(ImageView.VISIBLE);
            setImage(imageView, imageRef);
            imageView.setOnClickListener(v -> showImagePreview(imageView.getContext(), imageRef));
        }
    }

    public static void showImagePreview(Context context, Uri imageUri) {
        if (context == null || imageUri == null) return;
        showImagePreview(context, imageUri.toString());
    }

    public static void showImagePreview(Context context, String imageRef) {
        if (context == null || imageRef == null) return;

        Dialog dialog = new Dialog(context);
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.BLACK);

        ImageView image = new ImageView(context);
        setImage(image, imageRef);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ImageButton menuButton = new ImageButton(context);
        menuButton.setImageResource(R.drawable.ic_more_vertical);
        menuButton.setBackgroundColor(Color.TRANSPARENT);
        menuButton.setContentDescription(context.getString(R.string.image_options));
        menuButton.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));

        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(dp(context, 48), dp(context, 48));
        menuParams.gravity = Gravity.TOP | Gravity.END;
        menuParams.topMargin = dp(context, 12);
        menuParams.rightMargin = dp(context, 12);
        root.addView(menuButton, menuParams);

        image.setOnClickListener(v -> dialog.dismiss());
        menuButton.setOnClickListener(v -> showImageOptions(context, menuButton, imageRef));

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    private static void showImageOptions(Context context, ImageButton anchor, String imageRef) {
        PopupMenu menu = new PopupMenu(context, anchor);
        menu.getMenu().add(R.string.save_image_to_gallery);
        menu.setOnMenuItemClickListener(item -> {
            if (saveImageToGallery(context, imageRef)) {
                Toast.makeText(context, R.string.image_saved_to_gallery, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.image_save_failed, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        menu.show();
    }

    private static boolean saveImageToGallery(Context context, String imageRef) {
        ContentResolver resolver = context.getContentResolver();
        String fileName = "social_moderation_" + System.currentTimeMillis() + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Social Moderation");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (destUri == null) return false;

        try (OutputStream out = resolver.openOutputStream(destUri)) {
            if (out == null) return false;
            if (isDemoImageRef(imageRef)) {
                if (!writeDemoImage(context, imageRef, out)) return false;
            } else {
                try (InputStream in = resolver.openInputStream(Uri.parse(imageRef))) {
                    if (in == null) return false;
                    byte[] buf = new byte[8 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                }
            }
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Failed to save image to gallery: " + imageRef, e);
            resolver.delete(destUri, null, null);
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(destUri, values, null, null);
        }
        return true;
    }

    private static void setImage(ImageView imageView, String imageRef) {
        if (isDemoImageRef(imageRef)) {
            imageView.setImageResource(demoImageResId(imageRef));
        } else {
            imageView.setImageURI(Uri.parse(imageRef));
        }
    }

    private static boolean writeDemoImage(Context context, String imageRef, OutputStream out) {
        Drawable drawable = context.getDrawable(demoImageResId(imageRef));
        if (drawable == null) return false;

        Bitmap bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
    }

    private static boolean isDemoImageRef(String imageRef) {
        return imageRef != null && imageRef.startsWith(DEMO_IMAGE_PREFIX);
    }

    private static int demoImageResId(String imageRef) {
        String key = imageRef == null ? "" : imageRef.substring(DEMO_IMAGE_PREFIX.length());
        if ("forest".equals(key)) return R.drawable.profile_background_default_2;
        if ("violet".equals(key)) return R.drawable.profile_background_default_3;
        return R.drawable.profile_background_default_1;
    }

    private static void insertText(EditText input, String text) {
        int start = Math.max(input.getSelectionStart(), 0);
        int end = Math.max(input.getSelectionEnd(), 0);
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        input.getText().replace(min, max, text);
    }

    private static String stripImageTokens(String rawContent) {
        if (rawContent == null) return "";
        return IMAGE_TOKEN.matcher(rawContent).replaceAll("").replaceAll("\\n{3,}", "\n\n");
    }

    private static String firstImageRef(String rawContent) {
        if (rawContent == null) return null;
        Matcher matcher = IMAGE_TOKEN.matcher(rawContent);
        if (!matcher.find()) return null;
        return matcher.group(1);
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
