package io.snabble.sdk.ui.scanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Logger;

/**
 * A Barcode Scanner View using ZXing as its internal barcode scanner.
 * <p>
 * For a list of supported formats see {@link BarcodeFormat}.
 */
public class BarcodeScannerView extends FrameLayout implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {
    // the camera handler needs to be static because we can only access the camera
    // one time, if the camera is still releasing in another BarcodeScannerView in the background
    // and we try to start it while its releasing we would cause a crash
    //
    // to ensure that a camera is released properly first the camera handler is static so that
    // all events posted are processed in proper order
    private static Handler cameraHandler;
    private static Handler barcodeProcessingHandler;
    private static WeakReference<BarcodeScannerView> activeScannerView = new WeakReference<>(null);

    private Handler mainThreadHandler;

    private Callback callback;
    private Camera camera;

    private final Object frameBufferLock = new Object();
    private byte[] frontBuffer;
    private byte[] backBuffer;

    private boolean isProcessing;

    private Camera.Size previewSize;
    private Camera.CameraInfo cameraInfo;
    private boolean running;
    private boolean startRequested;

    private List<BarcodeFormat> supportedBarcodeFormats = new ArrayList<>();
    private int surfaceWidth;
    private int surfaceHeight;
    private TextureView textureView;
    private boolean zoomToFitView = true;
    private Rect detectionRect = new Rect();
    private ScanIndicatorView scanIndicatorView;
    private boolean torchEnabled;
    private boolean decodeEnabled;
    private int detectionDelayMs;
    private long nextDetectionTimeMs;
    private boolean isPaused;
    private boolean isAttachedToWindow;
    private TextView cameraUnavailableView;
    private int displayOrientation;
    private boolean restrictScanningToIndicator = true;
    private float restrictionOvershoot = 1.0f;
    private int bitsPerPixel;
    private boolean indicatorEnabled = true;
    private FrameLayout splashView;

    private BarcodeDetector barcodeDetector;

    public BarcodeScannerView(Context context) {
        super(context);
        init();
    }

    public BarcodeScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarcodeScannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (cameraHandler == null) {
            HandlerThread cameraHandlerThread = new HandlerThread("snabble CameraHandler");
            cameraHandlerThread.start();
            cameraHandler = new Handler(cameraHandlerThread.getLooper());
        }

        if (barcodeProcessingHandler == null) {
            HandlerThread frameProcessingThread = new HandlerThread("snabble BarcodeFrameProcessor");
            frameProcessingThread.start();
            barcodeProcessingHandler = new Handler(frameProcessingThread.getLooper());
        }

        mainThreadHandler = new Handler(Looper.getMainLooper());

        textureView = new TextureView(getContext());
        textureView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        addView(textureView);

        scanIndicatorView = new ScanIndicatorView(getContext());
        scanIndicatorView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addView(scanIndicatorView);

        splashView = new FrameLayout(getContext());
        splashView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        splashView.setBackgroundResource(UIUtils.getColorByAttribute(getContext(), android.R.attr.background));
        addView(splashView);

        cameraUnavailableView = new TextView(getContext());
        cameraUnavailableView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        cameraUnavailableView.setText(R.string.Snabble_Scanner_Camera_accessDenied);
        cameraUnavailableView.setGravity(Gravity.CENTER);
        cameraUnavailableView.setTextColor(UIUtils.getColorByAttribute(getContext(), android.R.attr.textColorPrimary));
        cameraUnavailableView.setBackgroundResource(UIUtils.getColorByAttribute(getContext(), android.R.attr.background));
        cameraUnavailableView.setVisibility(View.GONE);
        addView(cameraUnavailableView);

        textureView.setSurfaceTextureListener(this);
    }

    /**
     * Initializes the camera and starts the barcode scanner.
     * <p>
     * Preferably called in onStart of your Activity. After calling start you need to call {@link #stop()} at some time,
     * to release allocated camera resources.
     * <p>
     * If you don't do that, other apps may not be able to access the camera on Android 4.x
     *
     * @throws RuntimeException if the app does not have permission to access the camera
     */
    public void start() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            throw new RuntimeException("Missing camera permission");
        }

        cameraHandler.post(() -> {
            if (!running) {
                startRequested = true;
                startIfRequested();
            }
        });
    }

    private void startIfRequestedAsync() {
        splashView.setVisibility(View.VISIBLE);
        cameraHandler.post(() -> startIfRequested());
    }

    /**
     * Pauses the camera preview and barcode scanning, but keeps the camera intact.
     */
    public void pause() {
        cameraHandler.post(() -> {
            isPaused = true;

            if (running && activeScannerView.get() == this) {
                isProcessing = false;
                camera.stopPreview();
                camera.setPreviewCallbackWithBuffer(null);
                decodeEnabled = false;
                activeScannerView = new WeakReference<>(null);
            }
        });
    }

    /**
     * Resumes the camera preview and barcode scanning.
     */
    public void resume() {
        cameraHandler.post(() -> {
            resetFalsePositiveFilter();
            isPaused = false;

            if (!running) {
                start();
            } else {
                // as stated in the documentation:
                // focus parameters may not be preserved across preview restarts
                try {
                    Camera.Parameters parameters = camera.getParameters();
                    chooseFocusMode(parameters);
                    camera.setParameters(parameters);
                } catch (RuntimeException e) {
                    // occurs on some devices when calling getParameters. Just ignore it an start the preview
                    // without explicitly setting the focus mode then
                }

                try {
                    camera.startPreview();
                    activeScannerView = new WeakReference<>(this);
                } catch (RuntimeException e) {
                    showError(true);
                    return;
                }

                clearBuffers();
                decodeEnabled = true;

                synchronized (frameBufferLock) {
                    camera.setPreviewCallbackWithBuffer(BarcodeScannerView.this);
                    camera.addCallbackBuffer(backBuffer);
                }
            }
        });
    }

    private void startIfRequested() {
        try {
            final SurfaceTexture surface = textureView.getSurfaceTexture();

            if (surface == null || running || !startRequested || isPaused) {
                return;
            }

            setupBarcodeDetector();
            resetFalsePositiveFilter();

            showError(false);

            chooseBestCamera();

            if (camera == null || cameraInfo == null) {
                cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(0, cameraInfo);
                camera = Camera.open(0);
            } else {
                camera.stopPreview();
            }

            updateDisplayOrientation();
            int displayOrientation = getDisplayOrientation();
            camera.setDisplayOrientation(displayOrientation);

            Camera.Parameters parameters = camera.getParameters();

            chooseFocusMode(parameters);
            chooseFrameRate(parameters);

            parameters.setRotation(displayOrientation);
            chooseOptimalPreviewSize(parameters);

            // Some devices (Nexus 4) are defaulting to 5 preview fps instead of 30 without setting this hint
            // but settings this hint on all devices is not an option because
            // some Samsung devices (S5 Mini) with an exynos camera ignore the parameters
            // set previously completely when settings this hint, so we are only settings this hint
            // on devices that are known to be affected
            if (Build.MANUFACTURER.equals("LGE") && Build.MODEL.equals("Nexus 4")) {
                parameters.setRecordingHint(true);
            }

            camera.setParameters(parameters);

            camera.setPreviewTexture(surface);
            camera.startPreview();
            activeScannerView = new WeakReference<>(this);

            previewSize = parameters.getPreviewSize();

            bitsPerPixel = ImageFormat.getBitsPerPixel(parameters.getPreviewFormat());
            int bufferSize = previewSize.width * previewSize.height * bitsPerPixel / 8;

            // buffer where the barcode processing will be
            frontBuffer = new byte[bufferSize];

            // buffer where the camera will write to
            backBuffer = new byte[bufferSize];

            camera.setPreviewCallbackWithBuffer(this);
            camera.addCallbackBuffer(backBuffer);

            detectionRect.left = 0;
            detectionRect.top = 0;
            detectionRect.right = previewSize.width;
            detectionRect.bottom = previewSize.height;

            running = true;
            isProcessing = false;

            setTorchEnabled(torchEnabled);

            mainThreadHandler.post(() -> {
                autoFocus();
                updateTransform();
                decodeEnabled = true;
            });

            scheduleAutoFocus();
            showError(false);

            if (isPaused && activeScannerView.get() == this) {
                camera.stopPreview();
                camera.setPreviewCallbackWithBuffer(null);
                activeScannerView = new WeakReference<>(null);
            }
        } catch (Exception e) {
            showError(true);
        }
    }

    private void chooseBestCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    camera = Camera.open(i);
                    if (isSupportingAutoFocus(camera.getParameters())) {
                        cameraInfo = info;
                        break;
                    } else {
                        camera.release();
                    }
                } catch (Exception e) {
                    try {
                        if (camera != null) {
                            camera.release();
                        }
                    } catch (Exception ignored) { }
                }
            }
        }
    }

    private void resetFalsePositiveFilter() {
        barcodeProcessingHandler.post(() -> {
            if (barcodeDetector != null) {
                barcodeDetector.reset();
            }
        });
    }

    private boolean isSupportingAutoFocus(Camera.Parameters parameters) {
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        return supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
    }

    private void chooseOptimalPreviewSize(Camera.Parameters parameters) {
        Camera.Size optimalSize = getOptimalPreviewSize();

        if (optimalSize != null) {
            Logger.d("Choosing " + optimalSize.width + "x" + optimalSize.height + " as camera preview size");
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        } else {
            Logger.e("Could not get suitable preview size, using default camera preview size");
        }
    }

    private void chooseFrameRate(Camera.Parameters parameters) {
        List<int[]> supportedFrameRates = parameters.getSupportedPreviewFpsRange();
        if (supportedFrameRates != null) {
            int[] bestPair = supportedFrameRates.get(supportedFrameRates.size() - 1);

            // even tho the documentation states that the list is sorted from low->high
            // some camera drivers sort the other way around (e.g. Honer 8)
            for (int[] pair : supportedFrameRates) {
                if (pair[0] > bestPair[0] && pair[1] > bestPair[1]) {
                    bestPair = pair;
                }
            }

            parameters.setPreviewFpsRange(
                    bestPair[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                    bestPair[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        }
    }

    private void chooseFocusMode(Camera.Parameters parameters) {
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (supportedFocusModes != null) {
            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }
    }

    private void showError(final boolean show) {
        mainThreadHandler.post(() -> {
            if (show) {
                cameraUnavailableView.setVisibility(View.VISIBLE);
            } else {
                cameraUnavailableView.setVisibility(View.GONE);
            }

            setScanIndicatorVisible(!show);
        });
    }

    /**
     * Sets the state of the torch, if available on the device.
     *
     * @param enabled true if you want to see the light, otherwise fall into darkness
     */
    public void setTorchEnabled(boolean enabled) {
        if (camera != null && running) {
            try {
                Camera.Parameters parameters = camera.getParameters();

                if (parameters != null) {
                    List<String> supportedFlashModes = parameters.getSupportedFlashModes();
                    if (supportedFlashModes != null
                            && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)
                            && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                        if (enabled) {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        } else {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        }
                    }

                    camera.setParameters(parameters);
                    torchEnabled = enabled;
                }
            } catch (RuntimeException e) {
                // this is terrible, but happens on some devices in rare circumstances
                // one confirmed device is the motorola moto g7 plus
            }
        }
    }

    /**
     * @return Returns true if the torch is enabled, false otherwise.
     */
    public boolean isTorchEnabled() {
        if (camera != null && running) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                if (parameters != null) {
                    String flashMode = parameters.getFlashMode();
                    if (flashMode != null) {
                        return flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH);
                    }
                }
            } catch (Exception e) {
                // some devices throw an exception sometimes when calling getParameters()
            }
        }

        return false;
    }

    private void scheduleAutoFocus() {
        mainThreadHandler.postDelayed(() -> {
            if (running) {
                autoFocus();
                scheduleAutoFocus();
            }
        }, 1000);
    }

    private void autoFocus() {
        try {
            camera.autoFocus((success, camera) -> {
                // empty callback
            });
        } catch (RuntimeException e) {
            //ignore, happens mostly when calling autoFocus while its still focussing
        }
    }

    private void setupBarcodeDetector() {
        if (barcodeDetector == null) {
            barcodeDetector = BarcodeDetectorFactory.getDefaultBarcodeDetectorFactory().create();
        }

        barcodeDetector.setup(supportedBarcodeFormats);
    }

    /**
     * If set to true, zooms the camera preview image to fully fill the view.
     * The image will get cropped at one axis. (The one that is already at the border.
     * <p>
     * If set to false, the camera preview image will be fully shown, with black
     * (or whatever your background color is set at) bars.
     *
     * @param zoomToFitView true if you want to zoom the image, false otherwise.
     */
    public void setZoomToFitView(boolean zoomToFitView) {
        this.zoomToFitView = zoomToFitView;
        updateTransform();
    }

    /**
     * Sets a delay in milliseconds between successful barcode detection
     *
     * @param millis the delay in milliseconds
     */
    public void setDetectionDelayMs(int millis) {
        this.detectionDelayMs = millis;
    }

    /**
     * Adds a {@link BarcodeFormat} that should get detected.
     * <p>
     * Note: Scanning for multiple barcode formats has a performance impact.
     *
     * @param format the {@link BarcodeFormat}  to add
     */
    public void addBarcodeFormat(BarcodeFormat format) {
        supportedBarcodeFormats.add(format);
    }

    /**
     * Removes a barcode from the scannable {@link BarcodeFormat}'s
     *
     * @param format the {@link BarcodeFormat} to remove
     */
    public void removeBarcodeFormat(BarcodeFormat format) {
        supportedBarcodeFormats.remove(format);
    }

    public void stop() {
        cameraHandler.post(() -> {
            if (running && activeScannerView.get() == this) {
                camera.stopPreview();
                camera.setPreviewCallbackWithBuffer(null);
                camera.release();
                camera = null;
                activeScannerView = new WeakReference<>(null);
            }

            running = false;
            decodeEnabled = false;
            startRequested = false;
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startIfRequestedAsync();

        surfaceWidth = width;
        surfaceHeight = height;
        updateTransform();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        updateTransform();
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (running) {
            splashView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stop();
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
    }

    @Override
    public void onPreviewFrame(byte[] data, final Camera camera) {
        synchronized (frameBufferLock) {
            // skipping frames when the last frame is still processing
            if (isProcessing || nextDetectionTimeMs >= SystemClock.elapsedRealtime()) {
                camera.addCallbackBuffer(backBuffer);
                return;
            }

            swapBuffers();

            camera.addCallbackBuffer(backBuffer);

            isProcessing = true;
            nextDetectionTimeMs = SystemClock.elapsedRealtime() + detectionDelayMs;

            barcodeProcessingHandler.post(() -> {
                if (!isAttachedToWindow || !decodeEnabled) {
                    isProcessing = false;
                    return;
                }

                final Barcode barcode = barcodeDetector.detect(frontBuffer, previewSize.width,
                        previewSize.height, bitsPerPixel, detectionRect, displayOrientation);

                if (barcode != null) {
                    mainThreadHandler.post(() -> {
                        if (decodeEnabled && callback != null && isAttachedToWindow && !isPaused) {
                            callback.onBarcodeDetected(barcode);
                        }
                    });
                }

                isProcessing = false;
            });
        }
    }

    private void clearBuffers() {
        Arrays.fill(backBuffer, (byte) 0);
        Arrays.fill(frontBuffer, (byte) 0);
    }

    private void swapBuffers() {
        byte[] tmp = frontBuffer;
        frontBuffer = backBuffer;
        backBuffer = tmp;
    }

    /**
     * Sets the scale of the {@link ScanIndicatorView}.
     *
     * @param scale the scale of the {@link ScanIndicatorView}. Defaults to 1.
     */
    public void setIndicatorScale(float scale) {
        scanIndicatorView.setScale(scale);
    }

    /**
     * Enable or disable the {@link ScanIndicatorView}.
     *
     * @param enabled false to disable the {@link ScanIndicatorView}. Defaults to true.
     */
    public void setIndicatorEnabled(boolean enabled) {
        indicatorEnabled = enabled;
        setScanIndicatorVisible(enabled);
        updateTransform();
    }

    public void setIndicatorStyle(ScanIndicatorView.Style style) {
        scanIndicatorView.setStyle(style);
    }

    /**
     * Sets indicator style in when in normalized style, ignoring all padding's and offsets
     *
     * setIndicatorStyle(ScanIndicatorView.Style.NORMALIZED)
     */
    public void setIndicatorNormalizedSize(float left, float top, float right, float bottom) {
        scanIndicatorView.setNormalizedSize(left, top, right, bottom);
    }

    private void setScanIndicatorVisible(boolean visible) {
        if (indicatorEnabled && visible) {
            scanIndicatorView.setVisibility(View.VISIBLE);
        } else {
            scanIndicatorView.setVisibility(View.GONE);
        }
    }

    public void setIndicatorOffset(int offsetX, int offsetY) {
        scanIndicatorView.setOffset(offsetX, offsetY);
    }

    /**
     * Set to true, if you want to restrict barcode scanning to the center.
     * <p>
     * Default: true
     */
    public void setRestrictScanningToIndicator(boolean restrictScanningToIndicator) {
        this.restrictScanningToIndicator = restrictScanningToIndicator;
        updateTransform();
    }

    /**
     * Sets the multiplicaton value that is used to allow scanning outside of the indicator if
     * {@link #setRestrictScanningToIndicator(boolean)} is set
     *
     * Default is 1.0
     */
    public void setRestrictionOvershoot(float val) {
        if (val == 0.0f) {
            val = 1.0f;
        }

        restrictionOvershoot = val;
        updateTransform();
    }

    private void updateTransform() {
        if (camera == null || previewSize == null) {
            return;
        }

        float surfaceWidth = (float) this.surfaceWidth;
        float surfaceHeight = (float) this.surfaceHeight;

        float rotatedPreviewWidth = (float) previewSize.width;
        float rotatedPreviewHeight = (float) previewSize.height;

        if (surfaceWidth <= 0 || surfaceHeight <= 0 || rotatedPreviewWidth <= 0 || rotatedPreviewHeight <= 0) {
            return;
        }

        if (isInPortraitMode()) {
            float tmp = rotatedPreviewWidth;
            //noinspection SuspiciousNameCombination
            rotatedPreviewWidth = rotatedPreviewHeight;
            rotatedPreviewHeight = tmp;
        }

        float surfaceAspectRatio = surfaceWidth / surfaceHeight;
        float previewAspectRatio = rotatedPreviewWidth / rotatedPreviewHeight;

        Matrix adjustAspectRatioMatrix = new Matrix();

        float adjustedSurfaceWidth = Math.round(surfaceWidth / surfaceAspectRatio * previewAspectRatio);
        float adjustedWidthScaleFactor = adjustedSurfaceWidth / surfaceWidth;
        adjustAspectRatioMatrix.preScale(adjustedWidthScaleFactor, 1);

        float scale = 1;
        if (adjustedSurfaceWidth > surfaceWidth) {
            scale = surfaceWidth / adjustedSurfaceWidth;
        }

        adjustAspectRatioMatrix.postScale(scale, scale);

        Matrix transform = new Matrix();

        float destWidth = surfaceWidth * scale * adjustedWidthScaleFactor;
        float destHeight = surfaceHeight * scale;

        float dx = Math.abs(surfaceWidth - destWidth) / 2;
        float dy = Math.abs(surfaceHeight - destHeight) / 2;

        transform.preConcat(adjustAspectRatioMatrix);
        transform.postTranslate(dx, dy);

        float fitScale = 1.0f;

        if (zoomToFitView) {
            if (adjustedSurfaceWidth > surfaceWidth) {
                fitScale = adjustedSurfaceWidth / surfaceWidth;
            } else {
                fitScale = surfaceWidth / adjustedSurfaceWidth;
            }

            transform.postScale(fitScale, fitScale, surfaceWidth / 2, surfaceHeight / 2);
        }

        float destWidthScaled = destWidth * fitScale;
        float destHeightScaled = destHeight * fitScale;

        float offsetX = (destWidthScaled - surfaceWidth) / 2;
        float offsetY = (destHeightScaled - surfaceHeight) / 2;

        Rect rect;
        if (indicatorEnabled && restrictScanningToIndicator) {
            rect = scanIndicatorView.getIndicatorRect();
        } else {
            rect = new Rect(0, 0, getWidth(), getHeight());
        }

        rect.inset(Math.round(rect.width() * (1.0f - restrictionOvershoot)),
                Math.round(rect.height() * (1.0f - restrictionOvershoot)));

        rect.left = Math.max(0, rect.left);
        rect.top = Math.max(0, rect.top);
        rect.right = Math.min(getWidth(), rect.right);
        rect.bottom = Math.min(getHeight(), rect.bottom);

        float left = offsetX + surfaceWidth * ((float) rect.left / surfaceWidth);
        float top = offsetY + surfaceHeight * ((float) rect.top / surfaceHeight);
        float right = offsetX + surfaceWidth * ((float) rect.right / surfaceWidth);
        float bottom = offsetY + surfaceHeight * ((float) rect.bottom / surfaceHeight);

        float leftNormalized = left / destWidthScaled;
        float topNormalized = top / destHeightScaled;
        float rightNormalized = right / destWidthScaled;
        float bottomNormalized = bottom / destHeightScaled;

        if (isInPortraitMode()) {
            detectionRect.left = Math.round(previewSize.width * topNormalized);
            detectionRect.top = Math.round(previewSize.height * leftNormalized);
            detectionRect.right = Math.round(previewSize.width * bottomNormalized);
            detectionRect.bottom = Math.round(previewSize.height * rightNormalized);
        } else {
            detectionRect.left = Math.round(rotatedPreviewWidth * leftNormalized);
            detectionRect.top = Math.round(rotatedPreviewHeight * topNormalized);
            detectionRect.right = Math.round(rotatedPreviewWidth * rightNormalized);
            detectionRect.bottom = Math.round(rotatedPreviewHeight * bottomNormalized);
        }

        textureView.setTransform(transform);
    }

    private void updateDisplayOrientation() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm != null ? wm.getDefaultDisplay().getRotation() : 0;

        int degrees;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                degrees = 0;
                break;
        }

        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }

        displayOrientation = result;
    }

    private int getDisplayOrientation() {
        return displayOrientation;
    }

    private boolean isInPortraitMode() {
        return getDisplayOrientation() % 180 != 0;
    }

    private Camera.Size getOptimalPreviewSize() {
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null) return null;

        Camera.Size optimalSizeForWidth = null;
        Camera.Size optimalSizeForHeight = null;

        // we are using 1920x1080 as our base resolution, as its available on most devices and provides
        // a good balance between quality and performance
        // on low memory devices we try to use 1280x720

        final Runtime runtime = Runtime.getRuntime();
        final long used = runtime.totalMemory() - runtime.freeMemory();
        final long maxHeap = runtime.maxMemory();
        final long availableHeap = maxHeap - used;

        int targetWidth = 1920;
        int targetHeight = 1080;

        final long approxBytesNeeded = targetWidth * targetHeight * 4 * 2;

        if (availableHeap < approxBytesNeeded) {
            targetWidth = 1280;
            targetHeight = 720;
        }

        float minDiffX = Float.MAX_VALUE;
        float minDiffY = Float.MAX_VALUE;

        for (Camera.Size size : sizes) {
            if (Math.abs(size.height - targetHeight) < minDiffY) {
                optimalSizeForHeight = size;
                minDiffY = Math.abs(size.height - targetHeight);
            }

            if (Math.abs(size.width - targetWidth) < minDiffX) {
                optimalSizeForWidth = size;
                minDiffX = Math.abs(size.width - targetWidth);
            }
        }

        if (minDiffX < minDiffY) {
            return optimalSizeForWidth;
        } else {
            return optimalSizeForHeight;
        }
    }

    /**
     * Sets a callback to get scan results.
     *
     * @param callback the callback
     */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onBarcodeDetected(Barcode barcode);
    }
}
