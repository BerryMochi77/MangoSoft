package com.example.comp2100miniproject.src;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp2100miniproject.R;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.IntFunction;

import messagestate.MessageThreadRegistry;

/**
 * Paints L-shaped connector lines from each reply's parent avatar down to
 * the reply's own avatar. Lives on the RecyclerView's canvas (not inside
 * individual item views) so a line can span across rows — that's the only
 * way to draw from one avatar to another when the parent is in the
 * previous item.
 *
 * <p>Geometry constants mirror {@code fragment_message.xml} and
 * {@link com.example.comp2100miniproject.ThreadIndentView}. If the layout
 * changes those values must change here too — there is no single source
 * of truth yet.</p>
 */
public class ThreadConnectorDecoration extends RecyclerView.ItemDecoration {

    // Mirror fragment_message.xml geometry.
    private static final float INDENT_PER_LEVEL_DP = 22f;
    private static final float LEADING_PAD_DP = 14f;
    private static final float AVATAR_SIZE_DP = 32f;
    private static final float AVATAR_TOP_DP = 10f;
    private static final float CORNER_RADIUS_DP = 8f;
    private static final float STROKE_DP = 2f;

    private final float indentPerLevelPx;
    private final float leadingPadPx;
    private final float avatarSizePx;
    private final float avatarTopPx;
    private final float cornerRadiusPx;

    private final Paint paint;
    private final Path path = new Path();
    private final IntFunction<UUID> idAt;

    /**
     * @param idAt looks up the message UUID at a given adapter position;
     *             return {@code null} for out-of-range or unknown positions.
     */
    public ThreadConnectorDecoration(Context context, IntFunction<UUID> idAt) {
        this.idAt = idAt;
        float density = context.getResources().getDisplayMetrics().density;
        indentPerLevelPx = INDENT_PER_LEVEL_DP * density;
        leadingPadPx = LEADING_PAD_DP * density;
        avatarSizePx = AVATAR_SIZE_DP * density;
        avatarTopPx = AVATAR_TOP_DP * density;
        cornerRadiusPx = CORNER_RADIUS_DP * density;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        paint.setStrokeWidth(STROKE_DP * density);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {
        MessageThreadRegistry threads = MessageThreadRegistry.getInstance();
        int childCount = parent.getChildCount();
        if (childCount == 0) return;

        // Index visible children by message UUID so each child can find its
        // parent's view in O(1). Children whose UUID we can't resolve are
        // simply skipped.
        HashMap<UUID, View> visibleByUUID = new HashMap<>(childCount);
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            if (position == RecyclerView.NO_POSITION) continue;
            UUID id = idAt.apply(position);
            if (id != null) visibleByUUID.put(id, child);
        }

        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            if (position == RecyclerView.NO_POSITION) continue;

            UUID id = idAt.apply(position);
            if (id == null) continue;
            UUID parentId = threads.parentOf(id);
            if (parentId == null) continue;

            int depth = threads.depthOf(id);
            if (depth <= 0) continue;

            float parentColumnX = (depth - 1) * indentPerLevelPx
                    + leadingPadPx
                    + avatarSizePx / 2f;
            float avatarLeftX = depth * indentPerLevelPx + leadingPadPx;
            float childAvatarCenterY = child.getTop() + child.getTranslationY()
                    + avatarTopPx
                    + avatarSizePx / 2f;

            // Anchor the line at the parent's avatar centre if visible, or
            // at the top of the viewport if the parent has scrolled off.
            View parentView = visibleByUUID.get(parentId);
            float startY = parentView != null
                    ? parentView.getTop() + parentView.getTranslationY()
                            + avatarTopPx + avatarSizePx / 2f
                    : 0f;

            path.reset();
            path.moveTo(parentColumnX, startY);
            path.lineTo(parentColumnX, childAvatarCenterY - cornerRadiusPx);
            path.quadTo(parentColumnX, childAvatarCenterY,
                    parentColumnX + cornerRadiusPx, childAvatarCenterY);
            path.lineTo(avatarLeftX, childAvatarCenterY);
            canvas.drawPath(path, paint);
        }
    }
}
