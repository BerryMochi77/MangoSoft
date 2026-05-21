package com.example.comp2100miniproject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ComposerFormatManager {
    private static final String TAG = "ComposerFormatManager";
    private static final String ATTACHMENT_DIR = "composer_attachments";
    private static final String PREFS = "composer_formats";
    private static final String KEY_SAVED_EMOJIS = "saved_emojis";
    private static final String KEY_SAVED_STICKERS = "saved_stickers";
    private static final String DEMO_IMAGE_PREFIX = "demo:";
    private static final Pattern IMAGE_TOKEN = Pattern.compile("\\[\\[image:([^\\]]+)\\]\\]");
    private static final int MENU_SAVE_IMAGE_TO_GALLERY = 1;
    private static final int MENU_SAVE_IMAGE_AS_STICKER = 2;
    private static final String[] DEFAULT_EMOJIS = {
            "\uD83D\uDE42",
            "\uD83D\uDE02",
            "\uD83D\uDE0D",
            "\uD83D\uDC4D",
            "\uD83D\uDD25",
            "\uD83C\uDF89"
    };

    private ComposerFormatManager() {
    }

    public interface EmojiSelectionListener {
        void onEmojiSelected(String emoji);
    }

    private interface FormatSelectionListener {
        void onEmojiSelected(String emoji);
        void onStickerSelected(String imageRef);
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
        insertImageRef(input, imageUri.toString());
    }

    public static void insertImageRef(EditText input, String imageRef) {
        if (input == null || imageRef == null || imageRef.isEmpty()) return;
        insertText(input, "\n[[image:" + imageRef + "]]\n");
    }

    public static String imageToken(Uri imageUri) {
        if (imageUri == null) return "";
        return "[[image:" + imageUri + "]]";
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
            textView.setOnClickListener(null);
        } else {
            textView.setVisibility(TextView.VISIBLE);
            textView.setText(text);
            List<String> emojis = extractEmojis(text);
            textView.setOnClickListener(emojis.isEmpty()
                    ? null
                    : v -> showSaveEmojiChooser(textView.getContext(), emojis));
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

    public static String[] emojiOptions(Context context) {
        Set<String> options = new LinkedHashSet<>();
        for (String emoji : DEFAULT_EMOJIS) {
            options.add(emoji);
        }
        options.addAll(savedEmojis(context));
        return options.toArray(new String[0]);
    }

    public static void showEmojiChooser(Context context, EditText input) {
        showFormatChooser(context, true, new FormatSelectionListener() {
            @Override
            public void onEmojiSelected(String emoji) {
                insertEmoji(input, emoji);
            }

            @Override
            public void onStickerSelected(String imageRef) {
                insertImageRef(input, imageRef);
            }
        });
    }

    public static void showEmojiChooser(Context context, EmojiSelectionListener listener) {
        showFormatChooser(context, false, new FormatSelectionListener() {
            @Override
            public void onEmojiSelected(String emoji) {
                if (listener != null) listener.onEmojiSelected(emoji);
            }

            @Override
            public void onStickerSelected(String imageRef) {
                // Post reactions are text emoji only; sticker images remain a composer feature.
            }
        });
    }

    private static void showFormatChooser(Context context,
                                          boolean includeStickers,
                                          FormatSelectionListener listener) {
        List<FormatOption> options = new ArrayList<>();
        for (String emoji : emojiOptions(context)) {
            options.add(new FormatOption(emoji, emoji, false));
        }

        if (includeStickers) {
            List<String> stickers = savedStickers(context);
            for (int i = 0; i < stickers.size(); i++) {
                options.add(new FormatOption(
                        context.getString(R.string.sticker_label, i + 1),
                        stickers.get(i),
                        true
                ));
            }
        }

        GridView grid = new GridView(context);
        grid.setNumColumns(4);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(context, 10));
        grid.setHorizontalSpacing(dp(context, 10));
        grid.setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 10));
        grid.setClipToPadding(false);
        grid.setAdapter(new FormatOptionAdapter(context, options));

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 24));
        root.setBackgroundColor(context.getColor(R.color.surface));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(context);
        title.setText(R.string.add_emoji);
        title.setTextColor(context.getColor(R.color.text_primary));
        title.setTextSize(18f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton close = new ImageButton(context);
        close.setImageResource(R.drawable.ic_close);
        close.setContentDescription(context.getString(R.string.cancel));
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setColorFilter(context.getColor(R.color.text_secondary));
        close.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));

        root.addView(header);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 320)
        );
        gridParams.topMargin = dp(context, 8);
        root.addView(grid, gridParams);
        dialog.setContentView(root);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            FormatOption option = options.get(position);
            if (option.imageRef) {
                listener.onStickerSelected(option.value);
            } else {
                listener.onEmojiSelected(option.value);
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    public static boolean saveEmoji(Context context, String emoji) {
        if (context == null || emoji == null || emoji.isEmpty()) return false;

        Set<String> saved = new LinkedHashSet<>(savedEmojis(context));
        boolean added = saved.add(emoji);
        if (!added) return true;

        prefs(context)
                .edit()
                .putString(KEY_SAVED_EMOJIS, String.join("\n", saved))
                .apply();
        return true;
    }

    public static boolean saveSticker(Context context, String imageRef) {
        if (context == null || imageRef == null || imageRef.isEmpty()) return false;

        Set<String> saved = new LinkedHashSet<>(savedStickers(context));
        boolean added = saved.add(imageRef);
        if (!added) return true;

        prefs(context)
                .edit()
                .putString(KEY_SAVED_STICKERS, String.join("\n", saved))
                .apply();
        return true;
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
        menu.getMenu().add(0, MENU_SAVE_IMAGE_TO_GALLERY, 0, R.string.save_image_to_gallery);
        menu.getMenu().add(0, MENU_SAVE_IMAGE_AS_STICKER, 1, R.string.save_image_as_emoji);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_SAVE_IMAGE_AS_STICKER) {
                if (saveSticker(context, imageRef)) {
                    Toast.makeText(context, R.string.image_saved_as_emoji, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.image_save_failed, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (saveImageToGallery(context, imageRef)) {
                    Toast.makeText(context, R.string.image_saved_to_gallery, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.image_save_failed, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });
        menu.show();
    }

    private static void showSaveEmojiChooser(Context context, List<String> emojis) {
        String[] options = emojis.toArray(new String[0]);
        new AlertDialog.Builder(context)
                .setTitle(R.string.save_emoji_to_list)
                .setItems(options, (dialog, which) -> showEmojiOptions(context, options[which]))
                .show();
    }

    private static void showEmojiOptions(Context context, String emoji) {
        String[] options = {
                context.getString(R.string.save_emoji_to_list),
                context.getString(R.string.save_emoji_to_gallery)
        };

        new AlertDialog.Builder(context)
                .setTitle(emoji)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (saveEmoji(context, emoji)) {
                            Toast.makeText(context, R.string.emoji_saved_to_list, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, R.string.emoji_save_failed, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (saveEmojiToGallery(context, emoji)) {
                            Toast.makeText(context, R.string.emoji_saved_to_gallery, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, R.string.emoji_save_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private static boolean saveImageToGallery(Context context, String imageRef) {
        ContentResolver resolver = context.getContentResolver();
        String fileName = "social_moderation_" + System.currentTimeMillis() + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MangoSoft");
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

    private static boolean saveEmojiToGallery(Context context, String emoji) {
        ContentResolver resolver = context.getContentResolver();
        String fileName = "social_moderation_emoji_" + System.currentTimeMillis() + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MangoSoft");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (destUri == null) return false;

        try (OutputStream out = resolver.openOutputStream(destUri)) {
            if (out == null) return false;
            Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(260f);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float y = 256f - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(emoji, 256f, y, paint);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return false;
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Failed to save emoji to gallery: " + emoji, e);
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

    public static String previewText(String rawContent) {
        if (rawContent == null) return "";
        return IMAGE_TOKEN.matcher(rawContent)
                .replaceAll("[image]")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String textOnly(String rawContent) {
        return stripImageTokens(rawContent);
    }

    public static boolean hasImage(String rawContent) {
        return firstImageRef(rawContent) != null;
    }

    private static String stripImageTokens(String rawContent) {
        if (rawContent == null) return "";
        return IMAGE_TOKEN.matcher(rawContent).replaceAll("").replaceAll("\\n{3,}", "\n\n");
    }

    private static List<String> extractEmojis(String content) {
        ArrayList<String> emojis = new ArrayList<>();
        if (content == null) return emojis;

        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            if (isEmojiCodePoint(codePoint)) {
                int end = i + charCount;
                if (end < content.length() && content.codePointAt(end) == 0xFE0F) {
                    end += Character.charCount(0xFE0F);
                }
                String emoji = content.substring(i, end);
                if (!emojis.contains(emoji)) emojis.add(emoji);
                i = end;
            } else {
                i += charCount;
            }
        }
        return emojis;
    }

    private static boolean isEmojiCodePoint(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF);
    }

    private static List<String> savedEmojis(Context context) {
        ArrayList<String> emojis = new ArrayList<>();
        if (context == null) return emojis;

        String raw = prefs(context).getString(KEY_SAVED_EMOJIS, "");
        if (raw == null || raw.isEmpty()) return emojis;
        for (String emoji : raw.split("\\n")) {
            if (!emoji.isEmpty() && !emojis.contains(emoji)) {
                emojis.add(emoji);
            }
        }
        return emojis;
    }

    private static List<String> savedStickers(Context context) {
        ArrayList<String> stickers = new ArrayList<>();
        if (context == null) return stickers;

        String raw = prefs(context).getString(KEY_SAVED_STICKERS, "");
        if (raw == null || raw.isEmpty()) return stickers;
        for (String sticker : raw.split("\\n")) {
            if (!sticker.isEmpty() && !stickers.contains(sticker)) {
                stickers.add(sticker);
            }
        }
        return stickers;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static class FormatOption {
        final String label;
        final String value;
        final boolean imageRef;

        FormatOption(String label, String value, boolean imageRef) {
            this.label = label;
            this.value = value;
            this.imageRef = imageRef;
        }
    }

    private static class FormatOptionAdapter extends BaseAdapter {
        private final Context context;
        private final List<FormatOption> options;

        FormatOptionAdapter(Context context, List<FormatOption> options) {
            this.context = context;
            this.options = options;
        }

        @Override
        public int getCount() {
            return options.size();
        }

        @Override
        public FormatOption getItem(int position) {
            return options.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout cell;
            if (convertView instanceof FrameLayout) {
                cell = (FrameLayout) convertView;
                cell.removeAllViews();
            } else {
                cell = new FrameLayout(context);
                cell.setLayoutParams(new GridView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(context, 72)
                ));
                TypedArray attrs = context.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
                cell.setForeground(attrs.getDrawable(0));
                attrs.recycle();
            }

            FormatOption option = getItem(position);
            if (option == null) return cell;

            if (option.imageRef) {
                ImageView thumbnail = new ImageView(context);
                FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(dp(context, 56), dp(context, 56));
                imageParams.gravity = Gravity.CENTER;
                thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                setImage(thumbnail, option.value);
                cell.addView(thumbnail, imageParams);
            } else {
                TextView emoji = new TextView(context);
                emoji.setText(option.value);
                emoji.setTextSize(28f);
                emoji.setGravity(Gravity.CENTER);
                cell.addView(emoji, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
            }
            return cell;
        }
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
