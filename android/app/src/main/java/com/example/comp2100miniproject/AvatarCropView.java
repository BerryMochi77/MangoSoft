package com.example.comp2100miniproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

public class AvatarCropView extends View {
    private static final int OUTPUT_SIZE = 512;

    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path overlayPath = new Path();
    private Bitmap bitmap;
    private float scale = 1f;
    private float minScale = 1f;
    private float offsetX;
    private float offsetY;
    private float lastX;
    private float lastY;
    private float lastDistance;
    private boolean ready;

    public AvatarCropView(Context context) {
        super(context);
        setup();
    }

    public AvatarCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    private void setup() {
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        overlayPaint.setColor(0x66000000);
        overlayPaint.setStyle(Paint.Style.FILL);
    }

    public void setImageUri(Uri uri) throws IOException {
        try (InputStream input = getContext().getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input);
        }
        ready = false;
        resetTransform();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetTransform();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        if (!ready) resetTransform();

        Matrix matrix = imageMatrix();
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(bitmap, matrix, null);

        float radius = cropSize() / 2f;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        overlayPath.reset();
        overlayPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        overlayPath.addCircle(cx, cy, radius, Path.Direction.CCW);
        overlayPath.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(overlayPath, overlayPaint);
        canvas.drawCircle(cx, cy, radius, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                lastX = event.getX();
                lastY = event.getY();
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN -> {
                lastDistance = pointerDistance(event);
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                if (event.getPointerCount() >= 2) {
                    float distance = pointerDistance(event);
                    if (lastDistance > 0f) {
                        scale *= distance / lastDistance;
                        scale = Math.max(minScale, Math.min(scale, minScale * 5f));
                        clampOffsets();
                        invalidate();
                    }
                    lastDistance = distance;
                } else {
                    offsetX += event.getX() - lastX;
                    offsetY += event.getY() - lastY;
                    lastX = event.getX();
                    lastY = event.getY();
                    clampOffsets();
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastDistance = 0f;
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    public Bitmap cropBitmap() {
        if (bitmap == null) return null;
        Bitmap output = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(Color.TRANSPARENT);
        float factor = OUTPUT_SIZE / cropSize();
        canvas.translate(OUTPUT_SIZE / 2f, OUTPUT_SIZE / 2f);
        canvas.scale(factor, factor);
        canvas.translate(-getWidth() / 2f, -getHeight() / 2f);
        canvas.drawBitmap(bitmap, imageMatrix(), null);
        return output;
    }

    private void resetTransform() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        float cropSize = cropSize();
        minScale = Math.max(cropSize / bitmap.getWidth(), cropSize / bitmap.getHeight());
        scale = minScale;
        offsetX = (getWidth() - bitmap.getWidth() * scale) / 2f;
        offsetY = (getHeight() - bitmap.getHeight() * scale) / 2f;
        ready = true;
        clampOffsets();
    }

    private Matrix imageMatrix() {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate(offsetX, offsetY);
        return matrix;
    }

    private float cropSize() {
        return Math.min(getWidth(), getHeight()) * 0.86f;
    }

    private float pointerDistance(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void clampOffsets() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        float cropSize = cropSize();
        float cropLeft = (getWidth() - cropSize) / 2f;
        float cropTop = (getHeight() - cropSize) / 2f;
        float cropRight = cropLeft + cropSize;
        float cropBottom = cropTop + cropSize;
        float imageWidth = bitmap.getWidth() * scale;
        float imageHeight = bitmap.getHeight() * scale;

        RectF image = new RectF(offsetX, offsetY, offsetX + imageWidth, offsetY + imageHeight);
        if (image.left > cropLeft) offsetX -= image.left - cropLeft;
        if (image.top > cropTop) offsetY -= image.top - cropTop;
        image.set(offsetX, offsetY, offsetX + imageWidth, offsetY + imageHeight);
        if (image.right < cropRight) offsetX += cropRight - image.right;
        if (image.bottom < cropBottom) offsetY += cropBottom - image.bottom;
    }
}
