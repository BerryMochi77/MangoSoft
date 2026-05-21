package com.example.comp2100miniproject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

public final class UiFontManager {
    private static final Map<TextView, Float> ORIGINAL_TEXT_SIZES = new WeakHashMap<>();
    private static final Map<TextView, Typeface> ORIGINAL_TYPEFACES = new WeakHashMap<>();

    private UiFontManager() {
    }

    public static void applyToActivity(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        View root = activity.getWindow().getDecorView();
        root.post(() -> applyToViewTree(activity, root));
    }

    public static void applyToViewTree(Context context, View root) {
        if (context == null || root == null) return;
        float scale = AppPreferencesRepository.fontScale(context);
        Typeface typeface = selectedTypeface(context);
        applyRecursive(root, scale, typeface);
    }

    private static void applyRecursive(View view, float scale, Typeface selectedTypeface) {
        if (view instanceof TextView) {
            applyToTextView((TextView) view, scale, selectedTypeface);
        }

        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            applyRecursive(group.getChildAt(i), scale, selectedTypeface);
        }
    }

    private static void applyToTextView(TextView textView, float scale, Typeface selectedTypeface) {
        if (!ORIGINAL_TEXT_SIZES.containsKey(textView)) {
            ORIGINAL_TEXT_SIZES.put(textView, textView.getTextSize());
            ORIGINAL_TYPEFACES.put(textView, textView.getTypeface());
        }

        Float originalSize = ORIGINAL_TEXT_SIZES.get(textView);
        if (originalSize != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSize * scale);
        }

        Typeface originalTypeface = ORIGINAL_TYPEFACES.get(textView);
        if (selectedTypeface == null) {
            textView.setTypeface(originalTypeface);
        } else {
            int style = originalTypeface == null ? Typeface.NORMAL : originalTypeface.getStyle();
            textView.setTypeface(Typeface.create(selectedTypeface, style));
        }
    }

    private static Typeface selectedTypeface(Context context) {
        String family = AppPreferencesRepository.getFontFamily(context);
        if (AppPreferencesRepository.FONT_SERIF.equals(family)) {
            return Typeface.SERIF;
        }
        if (AppPreferencesRepository.FONT_MONOSPACE.equals(family)) {
            return Typeface.MONOSPACE;
        }
        return null;
    }
}
