package com.example.comp2100miniproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

public class CircleAvatarImageView extends AppCompatImageView {
    private final Path circlePath = new Path();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float borderWidth;

    public CircleAvatarImageView(Context context) {
        super(context);
        init(context);
    }

    public CircleAvatarImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CircleAvatarImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.CENTER_CROP);
        borderWidth = 1.5f * context.getResources().getDisplayMetrics().density;
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderWidth);
        borderPaint.setColor(ContextCompat.getColor(context, R.color.border));
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

        canvas.drawCircle(
                getWidth() / 2f,
                getHeight() / 2f,
                radius - borderWidth / 2f,
                borderPaint
        );
    }
}
