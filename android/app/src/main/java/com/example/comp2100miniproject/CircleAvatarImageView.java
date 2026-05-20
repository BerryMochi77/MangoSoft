package com.example.comp2100miniproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class CircleAvatarImageView extends AppCompatImageView {
    private final Path circlePath = new Path();

    public CircleAvatarImageView(Context context) {
        super(context);
        init();
    }

    public CircleAvatarImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleAvatarImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float radius = Math.min(getWidth(), getHeight()) / 2f;
        circlePath.reset();
        circlePath.addCircle(getWidth() / 2f, getHeight() / 2f, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(circlePath);
        super.onDraw(canvas);
        canvas.restore();
    }
}
