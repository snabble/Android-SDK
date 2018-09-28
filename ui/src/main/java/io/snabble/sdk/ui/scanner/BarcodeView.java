package io.snabble.sdk.ui.scanner;


import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import androidx.appcompat.widget.AppCompatImageView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.WindowManager;

import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Logger;

public class BarcodeView extends AppCompatImageView {
    private String text;
    private String generatedText;
    private BarcodeFormat format;

    private int width;
    private int height;

    private boolean isNumberDisplayEnabled = true;
    private boolean adjustBrightness = true;

    private Handler uiHandler;
    private int backgroundColor;

    public BarcodeView(Context context) {
        super(context);
        init(context, null);
    }

    public BarcodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BarcodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        uiHandler = new Handler(Looper.getMainLooper());

        if (attrs != null) {
            TypedArray arr = context.getTheme().obtainStyledAttributes(
                    attrs, R.styleable.BarcodeView, 0, 0);

            try {
                String format = arr.getString(R.styleable.BarcodeView_format);
                if (format != null) {
                    BarcodeFormat barcodeFormat = BarcodeFormat.valueOf(format);
                    if (barcodeFormat != null) {
                        setFormat(barcodeFormat);
                    }
                }

                String text = arr.getString(R.styleable.BarcodeView_text);
                if (text != null) {
                    setText(text);
                }

                adjustBrightness = arr.getBoolean(R.styleable.BarcodeView_adjustBrightness, true);
            } finally {
                arr.recycle();
            }
        }

        Drawable background = getBackground();
        if (background instanceof ColorDrawable) {
            backgroundColor = ((ColorDrawable) background).getColor();
        } else {
            backgroundColor = Color.WHITE;
        }
    }

    /**
     * Sets the {@link BarcodeFormat} to be displayed. For an enumeration of supported formats
     * see {@link BarcodeFormat}.
     */
    public void setFormat(BarcodeFormat format) {
        if (this.format != format) {
            this.format = format;

            if (text != null) {
                generate();
            }
        }
    }

    /**
     * Sets the content of the barcode. The user is responsible for the correctness of the content.
     * <p>
     * The user is responsible for the correctness of the content, for example if you pass an
     * EAN 13 code that does not matches the checksum an Exception will be thrown.
     *
     * @throws IllegalStateException if no barcode format was set with {@link #setFormat(BarcodeFormat)}.
     */
    public void setText(String code) {
        if (format == null) {
            throw new IllegalStateException("No format set!");
        }

        if (code != null && code.length() > 0 && !code.equals(text)) {
            text = code;
            generate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        generate();
    }

    /**
     * When enabled, draws text over the generated barcode that shows its content.
     * <p>
     * Currently works only for EAN13 codes.
     */
    public void setNumberDisplayEnabled(boolean enabled) {
        isNumberDisplayEnabled = enabled;
    }

    private void generate() {
        final int w = getWidth();
        final int h = getHeight();

        if (text != null && w > 0 && h > 0) {
            if (!text.equals(generatedText) || width != w || height != h) {
                width = w;
                height = h;
                generatedText = text;

                final HandlerThread handlerThread = new HandlerThread("BarcodeView");
                handlerThread.start();
                final Handler backgroundHandler = new Handler(handlerThread.getLooper());
                backgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        MultiFormatWriter writer = new MultiFormatWriter();
                        try {
                            int paddingWidth = getPaddingRight() + getPaddingLeft();
                            int paddingHeight = getPaddingBottom() + getPaddingTop();

                            int tw = w - paddingWidth;
                            int th = h - paddingHeight;

                            BitMatrix bm = writer.encode(text, format.getZxingBarcodeFormat(), tw, th);
                            int[] pixels = new int[w * h];

                            // DATA-MATRIX codes are not scaled
                            // See https://github.com/zxing/zxing/issues/836
                            if (format == BarcodeFormat.DATA_MATRIX) {
                                float dw = (float)tw / (float)bm.getWidth();
                                float dh = (float)th / (float)bm.getHeight();

                                for (int y = 0; y < th; y++) {
                                    final int stride = y * tw;
                                    int ay = (int)((float)y/dh);

                                    for (int x = 0; x < tw; x++) {
                                        int ax = (int)((float)x/dw);
                                        pixels[x + stride] = bm.get(ax, ay) ? Color.BLACK : backgroundColor;
                                    }
                                }
                            } else {
                                for (int y = 0; y < th; y++) {
                                    final int stride = y * tw;

                                    for (int x = 0; x < tw; x++) {
                                        pixels[x + stride] = bm.get(x, y) ? Color.BLACK : backgroundColor;
                                    }
                                }
                            }

                            Bitmap bitmap = Bitmap.createBitmap(pixels, tw, th, Bitmap.Config.ARGB_8888);

                            if (isNumberDisplayEnabled) {
                                if (format == BarcodeFormat.EAN_13) {
                                    bitmap = drawEan13Text(bm, bitmap);
                                }
                            }

                            final Bitmap finalBitmap = bitmap;

                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setImageBitmap(finalBitmap);
                                }
                            });
                        } catch (WriterException e) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    width = 0;
                                    height = 0;
                                    generatedText = null;
                                    setImageBitmap(null);
                                }
                            });
                        }

                        handlerThread.quit();
                    }
                });
            }
        } else {
            width = 0;
            height = 0;
            generatedText = null;
            setImageBitmap(null);
        }
    }

    private Bitmap drawEan13Text(BitMatrix bm, Bitmap barcodeBitmap) {
        Bitmap bitmap = Bitmap.createBitmap(
                barcodeBitmap.getWidth(),
                barcodeBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        int[] rect = bm.getEnclosingRectangle();

        int left = rect[0];
        int top = rect[1];
        int bottom = top + rect[3];
        int oneBitWidth = rect[2] / 95;

        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(barcodeBitmap, 0, 0, null);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(backgroundColor);

        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(dp2px(14));
        textPaint.setAntiAlias(true);

        Rect bounds = new Rect();

        String part1 = text.substring(0, 1);
        String part2 = text.substring(1, 7);
        String part3 = text.substring(7);

        //digit 1
        textPaint.getTextBounds(part1, 0, part1.length(), bounds);
        int x = left - bounds.width() - dp2px(6);

        canvas.drawRect(x, top, left, bottom, backgroundPaint);
        canvas.drawText(part1, x + dp2px(3), bottom, textPaint);

        //digits 2 - 7
        textPaint.getTextBounds(part2, 0, part2.length(), bounds);
        x = left + oneBitWidth * 3;
        int w = oneBitWidth * 7 * 6;

        int dp1 = dp2px(1);

        canvas.drawRect(x, bottom - bounds.height() - dp1, x + w, bottom, backgroundPaint);
        canvas.drawText(part2, x + w / 2 - bounds.width() / 2, bottom, textPaint);

        //digits 8 - 13
        textPaint.getTextBounds(part3, 0, part3.length(), bounds);
        x = x + w + oneBitWidth * 5;

        canvas.drawRect(x, bottom - bounds.height() - dp1, x + w, bottom, backgroundPaint);
        canvas.drawText(part3, x + w / 2 - bounds.width() / 2, bottom, textPaint);

        return bitmap;
    }

    private int dp2px(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * When set to true, adjusts the screen brightness when the view is visible
     */
    public void setAdjustBrightness(boolean enabled) {
        adjustBrightness = enabled;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!isInEditMode() && adjustBrightness) {
            Activity activity = UIUtils.getHostActivity(getContext());
            if (activity != null && isShown()) {
                WindowManager.LayoutParams localLayoutParams = activity.getWindow().getAttributes();
                localLayoutParams.screenBrightness = 0.99f; // value of 1.0 is not accepted
                activity.getWindow().setAttributes(localLayoutParams);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (!isInEditMode() && adjustBrightness) {
            Activity activity = UIUtils.getHostActivity(getContext());
            if (activity != null) {
                WindowManager.LayoutParams localLayoutParams = activity.getWindow().getAttributes();
                localLayoutParams.screenBrightness = -1f;
                activity.getWindow().setAttributes(localLayoutParams);
            }
        }
    }
}
