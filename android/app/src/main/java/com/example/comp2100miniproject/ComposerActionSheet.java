package com.example.comp2100miniproject;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

final class ComposerActionSheet {
    private ComposerActionSheet() {}

    static void show(Context context, Runnable imageAction, Runnable emojiAction) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 24));
        root.setBackgroundColor(ContextCompat.getColor(context, R.color.surface));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(context);
        title.setText(R.string.more_composer_options);
        title.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        title.setTextSize(18f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton close = new ImageButton(context);
        close.setImageResource(R.drawable.ic_close);
        close.setContentDescription(context.getString(R.string.cancel));
        close.setBackgroundResource(android.R.color.transparent);
        close.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        close.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));
        root.addView(header);

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(context, 14), 0, 0);

        actions.addView(actionButton(
                context,
                R.drawable.ic_image,
                R.string.add_image,
                () -> {
                    dialog.dismiss();
                    imageAction.run();
                }
        ), new LinearLayout.LayoutParams(0, dp(context, 92), 1f));

        View spacer = new View(context);
        actions.addView(spacer, new LinearLayout.LayoutParams(dp(context, 12), 1));

        actions.addView(actionButton(
                context,
                R.drawable.ic_emoji,
                R.string.add_emoji,
                () -> {
                    dialog.dismiss();
                    emojiAction.run();
                }
        ), new LinearLayout.LayoutParams(0, dp(context, 92), 1f));

        root.addView(actions);
        dialog.setContentView(root);
        dialog.show();
    }

    static void showMoreFormats(Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 24));
        root.setBackgroundColor(ContextCompat.getColor(context, R.color.surface));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(context);
        title.setText(R.string.more_composer_options);
        title.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        title.setTextSize(18f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton close = new ImageButton(context);
        close.setImageResource(R.drawable.ic_close);
        close.setContentDescription(context.getString(R.string.cancel));
        close.setBackgroundResource(android.R.color.transparent);
        close.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        close.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));
        root.addView(header);

        View spacer = new View(context);
        root.addView(spacer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 72)
        ));

        dialog.setContentView(root);
        dialog.show();
    }

    private static View actionButton(Context context,
                                     @DrawableRes int iconRes,
                                     @StringRes int labelRes,
                                     Runnable action) {
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        button.setBackgroundResource(R.drawable.bg_card);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(v -> action.run());

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(context, R.color.accent));
        button.addView(icon, new LinearLayout.LayoutParams(dp(context, 30), dp(context, 30)));

        TextView label = new TextView(context);
        label.setText(labelRes);
        label.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        label.setTextSize(14f);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dp(context, 8);
        button.addView(label, labelParams);
        return button;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
