package com.example.comp2100miniproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class RoundedImageView extends AppCompatImageView {
    private final Path roundedPath = new Path();
    private final RectF bounds = new RectF();
    private float cornerRadius;

    public RoundedImageView(Context context) {
        super(context);
        init(context);
    }

    public RoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.CENTER_CROP);
        cornerRadius = 8f * context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        bounds.set(0, 0, getWidth(), getHeight());
        roundedPath.reset();
        roundedPath.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(roundedPath);
        super.onDraw(canvas);
        canvas.restore();
    }
}
