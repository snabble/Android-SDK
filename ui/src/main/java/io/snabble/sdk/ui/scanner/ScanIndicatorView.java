package io.snabble.sdk.ui.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class ScanIndicatorView extends View {
    private Paint rectPaint = new Paint();

    private float scale = 1.0f;

    private Rect rect = new Rect();
    private Rect borderRect = new Rect();

    private int offsetX;
    private int offsetY;
    private int borderColor;

    private int minPaddingLeft;
    private int minPaddingTop;
    private int minPaddingRight;
    private int minPaddingBottom;

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
        rectPaint.setColor(Color.WHITE);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(dp2px(1));
        rectPaint.setAlpha(51);

        borderColor = Color.parseColor("#99222222");

        setMinPadding(dp2px(8), dp2px(80), dp2px(8), dp2px(80));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        update();
    }

    private void update() {
        int w = getWidth();
        int h = getHeight();

        int maxWidth = w - minPaddingLeft - minPaddingRight;
        int maxHeight = h - minPaddingTop - minPaddingBottom;

        int rectWidth = dp2px(360) - minPaddingLeft - minPaddingRight;
        int rectHeight = dp2px(170);

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

        rect.left = w / 2 - rectWidth / 2;
        rect.right = w / 2 + rectWidth / 2;
        rect.top = (h / 2 - rectHeight / 2);
        rect.bottom = (h / 2 + rectHeight / 2);

        int rw = rect.width();
        int rh = rect.height();

        rect.left = offsetX + w / 2 - Math.round(rw / 2 * scale);
        rect.right = offsetX + w / 2 + Math.round(rw / 2 * scale);
        rect.top = offsetY + h / 2 - Math.round(rh / 2 * scale);
        rect.bottom = offsetY + h / 2 + Math.round(rh / 2 * scale);

        borderRect.set(rect);
        borderRect.inset(dp2px(1), dp2px(1));
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
        canvas.clipRect(rect, Region.Op.DIFFERENCE);
        canvas.drawColor(borderColor);
        canvas.restore();

        canvas.drawRect(borderRect, rectPaint);
    }

    public Rect getIndicatorRect() {
        return new Rect(rect);
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
