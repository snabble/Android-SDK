package io.snabble.sdk.ui.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ScanIndicatorView extends View {
    public enum Style {
        RECT,
        QUAD,
        NORMALIZED
    }

    private float scale = 1.0f;

    private RectF rect = new RectF();
    private Path path = new Path();

    private int offsetX;
    private int offsetY;
    private int backgroundColor;

    private int minPaddingLeft;
    private int minPaddingTop;
    private int minPaddingRight;
    private int minPaddingBottom;

    private float normalizedLeft;
    private float normalizedTop;
    private float normalizedRight;
    private float normalizedBottom;

    private Style style = Style.RECT;

    public ScanIndicatorView(@NonNull Context context) {
        super(context);
        init();
    }

    public ScanIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundColor = Color.parseColor("#99222222");

        setMinPadding(dp2px(24), dp2px(80), dp2px(24), dp2px(80));
        setStyle(Style.RECT);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        update();
    }

    private void update() {
        int w = getWidth();
        int h = getHeight();

        if (style == Style.NORMALIZED) {
            rect.left = w * normalizedLeft;
            rect.top = h * normalizedTop;
            rect.right = w * normalizedRight;
            rect.bottom = h * normalizedBottom;
        } else {
            int maxWidth = w - minPaddingLeft - minPaddingRight;
            int maxHeight = h - minPaddingTop - minPaddingBottom;

            int rectWidth;
            int rectHeight;

            if (style == Style.RECT) {
                rectWidth = getWidth() - minPaddingLeft - minPaddingRight;
                rectHeight = Math.round(rectWidth * 0.55f);
            } else {
                rectWidth = dp2px(360) - minPaddingLeft - minPaddingRight;
                rectHeight = Math.round(rectWidth * 0.85f);
            }

            if (rectHeight > maxHeight) {
                float scaleFactor = ((float) maxHeight / (float) rectHeight);

                rectWidth = Math.round((float) rectWidth * scaleFactor);
                rectHeight = Math.round((float) rectHeight * scaleFactor);
            }

            if (rectWidth > maxWidth) {
                float scaleFactor = ((float) maxWidth / (float) rectWidth);

                rectWidth = Math.round((float) rectWidth * scaleFactor);
                rectHeight = Math.round((float) rectHeight * scaleFactor);
            }

            rect.left = w / 2.0f - rectWidth / 2.0f;
            rect.right = w / 2.0f + rectWidth / 2.0f;
            rect.top = h / 2.0f - rectHeight / 2.0f;
            rect.bottom = h / 2.0f + rectHeight / 2.0f;

            float rw = rect.width();
            float rh = rect.height();

            rect.left = offsetX + w / 2.0f - Math.round(rw / 2.0f * scale);
            rect.right = offsetX + w / 2.0f + Math.round(rw / 2.0f * scale);
            rect.top = offsetY + h / 2.0f - Math.round(rh / 2.0f * scale);
            rect.bottom = offsetY + h / 2.0f + Math.round(rh / 2.0f * scale);
        }

        path.reset();
        path.addRoundRect(rect, dp2px(10), dp2px(10), Path.Direction.CCW);
    }

    public void setMinPadding(int left, int top, int right, int bottom) {
        minPaddingLeft = left;
        minPaddingTop = top;
        minPaddingRight = right;
        minPaddingBottom = bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(path, Region.Op.DIFFERENCE);
        canvas.drawColor(backgroundColor);
        canvas.restore();
    }

    public Rect getIndicatorRect() {
        return new Rect(Math.round(rect.left),
                Math.round(rect.top),
                Math.round(rect.right),
                Math.round(rect.bottom));
    }

    public void setStyle(Style style) {
        this.style = style;
        update();
    }

    public void setNormalizedSize(float left, float top, float right, float bottom) {
        normalizedLeft = left;
        normalizedTop = top;
        normalizedRight = right;
        normalizedBottom = bottom;
    }

    public void setOffset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        update();
    }

    public void setScale(float scale) {
        if (scale > 0) {
            this.scale = scale;
            update();
        } else {
            setVisibility(View.INVISIBLE);
        }
    }

    private int dp2px(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
