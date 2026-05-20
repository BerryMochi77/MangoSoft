package com.example.comp2100miniproject;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
        Uri imageUri = firstImageUri(rawContent);

        if (text.isEmpty()) {
            textView.setVisibility(TextView.GONE);
        } else {
            textView.setVisibility(TextView.VISIBLE);
            textView.setText(text);
        }

        if (imageUri == null) {
            imageView.setVisibility(ImageView.GONE);
            imageView.setImageDrawable(null);
        } else {
            imageView.setVisibility(ImageView.VISIBLE);
            imageView.setImageURI(imageUri);
        }
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

    private static Uri firstImageUri(String rawContent) {
        if (rawContent == null) return null;
        Matcher matcher = IMAGE_TOKEN.matcher(rawContent);
        if (!matcher.find()) return null;
        return Uri.parse(matcher.group(1));
    }
}
