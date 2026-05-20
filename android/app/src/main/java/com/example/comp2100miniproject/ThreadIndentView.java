package com.example.comp2100miniproject;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Zero-content View that reserves the horizontal indent for a comment row.
 * Width = {@code depth * INDENT_PER_LEVEL_DP + LEADING_PAD_DP}, so the row's
 * avatar starts at the same X regardless of nesting.
 *
 * <p>The actual thread-connector lines are painted on the RecyclerView's
 * canvas by {@link com.example.comp2100miniproject.src.ThreadConnectorDecoration},
 * because a connector has to span from a parent row to a child row and a
 * View can't draw outside its own bounds.</p>
 *
 * <p>Geometry constants are duplicated in {@code ThreadConnectorDecoration}
 * and {@code fragment_message.xml}. Keep them in sync.</p>
 */
public class ThreadIndentView extends View {

    private static final float INDENT_PER_LEVEL_DP = 22f;
    private static final float LEADING_PAD_DP = 14f;

    private int depth = 0;
    private final int indentPerLevelPx;
    private final int leadingPadPx;

    public ThreadIndentView(Context context) {
        this(context, null);
    }

    public ThreadIndentView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThreadIndentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        indentPerLevelPx = Math.round(INDENT_PER_LEVEL_DP * density);
        leadingPadPx = Math.round(LEADING_PAD_DP * density);
    }

    public void setDepth(int depth) {
        int clamped = Math.max(0, depth);
        if (clamped == this.depth) return;
        this.depth = clamped;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = leadingPadPx + depth * indentPerLevelPx;
        int resolvedHeight = resolveSize(0, heightMeasureSpec);
        setMeasuredDimension(width, resolvedHeight);
    }
}
