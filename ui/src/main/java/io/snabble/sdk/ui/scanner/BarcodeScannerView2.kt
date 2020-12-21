package io.snabble.sdk.ui.scanner

import android.Manifest
import android.R.attr
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import io.snabble.sdk.BarcodeFormat
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Dispatch
import kotlinx.android.synthetic.main.snabble_item_checkout_offline_qrcode.view.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * A Barcode Scanner View using ZXing as its internal barcode scanner.
 *
 *
 * For a list of supported formats see [BarcodeFormat].
 */
@SuppressWarnings("MissingPermission")
class BarcodeScannerView2 @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    var restrictionOvershoot: Float? = 0.0f

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private var cameraSelector: CameraSelector? = null
    private var camera: androidx.camera.core.Camera? = null
    private var cameraExecutor: ExecutorService? = null
    private val supportedBarcodeFormats: MutableList<BarcodeFormat> = ArrayList()

    private val previewView: PreviewView
    private var cameraUnavailableView: TextView
    private var scanIndicatorView: ScanIndicatorView

    private var indicatorEnabled = true
    private var barcodeDetector: BarcodeDetector
    private var callback: Callback? = null

    init {
        previewView = PreviewView(context)
        previewView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        addView(previewView)

        scanIndicatorView = ScanIndicatorView(context)
        scanIndicatorView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        addView(scanIndicatorView)

        cameraUnavailableView = TextView(context)
        cameraUnavailableView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        cameraUnavailableView.setText(R.string.Snabble_Scanner_Camera_accessDenied)
        cameraUnavailableView.gravity = Gravity.CENTER
        cameraUnavailableView.setTextColor(UIUtils.getColorByAttribute(context, android.R.attr.textColorPrimary))
        cameraUnavailableView.setBackgroundResource(UIUtils.getColorByAttribute(context, android.R.attr.background))
        cameraUnavailableView.visibility = GONE

        addView(cameraUnavailableView)

        barcodeDetector = BarcodeDetectorFactory.getDefaultBarcodeDetectorFactory().create()
    }

    /**
     * Initializes the camera and starts the barcode scanner.
     *
     *
     * Preferably called in onStart of your Activity. After calling start you need to call [.stop] at some time,
     * to release allocated camera resources.
     *
     *
     * If you don't do that, other apps may not be able to access the camera on Android 4.x
     *
     * @throws RuntimeException if the app does not have permission to access the camera
     */
    fun start() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            throw RuntimeException("Missing camera permission")
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        previewView.post {
            setCameraProviderListener()
        }

        barcodeDetector.reset()
        barcodeDetector.setup(supportedBarcodeFormats)
    }

    private fun setCameraProviderListener() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindPreview(cp: ProcessCameraProvider) {
        val rotation = previewView.display.rotation

        cameraProvider = cp

        cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

        preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(rotation)
                .build()

        imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(rotation)
                .build()
                .also {
                    // TODO remove !!
                    it.setAnalyzer(cameraExecutor!!, { image ->
                        val y = image.planes[0].buffer // Y plane of a YUV buffer
                        val rect = if (scanIndicatorView.visibility == View.VISIBLE) {
                            val height = image.height / 3
                            val top = (image.height / 2) - height
                            val bottom = (image.height / 2) + height
                            Rect(0, top, image.width, bottom)
                        } else {
                            Rect(0, 0, image.width, image.height)
                        }

                        val barcode = barcodeDetector.detect(y.toByteArray(),
                                image.width, image.height, 8, rect, previewView.display.rotation)

                        if (barcode != null) {
                            Dispatch.mainThread {
                                callback?.onBarcodeDetected(barcode)
                            }
                        }
                        image.close()
                    })
                }

        cameraProvider?.unbindAll()
        resume()
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    fun stop() {
        pause()
        cameraExecutor?.shutdown()
    }

    /**
     * Pauses the camera preview and barcode scanning, but keeps the camera intact.
     */
    fun pause() {
        cameraProvider?.unbindAll()
    }

    /**
     * Resumes the camera preview and barcode scanning.
     */
    fun resume() {
        try {
            camera = cameraProvider?.bindToLifecycle(UIUtils.getHostFragmentActivity(context), cameraSelector!!, preview, imageAnalyzer)

            val factory = previewView.meteringPointFactory
            val dm = resources.displayMetrics
            val point = factory.createPoint((dm.widthPixels / 2).toFloat(), (dm.heightPixels / 2).toFloat())
            val action = FocusMeteringAction.Builder(point)
                    .setAutoCancelDuration(2, TimeUnit.SECONDS)
                    .build()
            camera?.cameraControl?.startFocusAndMetering(action)
        } catch (e: Exception) {
            cameraUnavailableView.visibility = View.VISIBLE
            scanIndicatorView.visibility = View.GONE
        }
    }

    /**
     * @return Returns true if the torch is enabled, false otherwise.
     */
    /**
     * Sets the state of the torch, if available on the device.
     *
     * @param enabled true if you want to see the light, otherwise fall into darkness
     */
    var isTorchEnabled: Boolean
        get() = false
        set(enabled) {
            camera?.cameraControl?.enableTorch(enabled)
        }

    private fun setupBarcodeDetector() {

    }

    /**
     * Sets a delay in milliseconds between successful barcode detection
     *
     * @param millis the delay in milliseconds
     */
    fun setDetectionDelayMs(millis: Int) {}

    /**
     * Adds a [BarcodeFormat] that should get detected.
     *
     *
     * Note: Scanning for multiple barcode formats has a performance impact.
     *
     * @param format the [BarcodeFormat]  to add
     */
    fun addBarcodeFormat(format: BarcodeFormat) {
        supportedBarcodeFormats.add(format)
    }

    /**
     * Removes a barcode from the scannable [BarcodeFormat]'s
     *
     * @param format the [BarcodeFormat] to remove
     */
    fun removeBarcodeFormat(format: BarcodeFormat) {
        supportedBarcodeFormats.remove(format)
    }

    /**
     * Sets the scale of the [ScanIndicatorView].
     *
     * @param scale the scale of the [ScanIndicatorView]. Defaults to 1.
     */
    fun setIndicatorScale(scale: Float) {
       scanIndicatorView.setScale(scale)
    }

    /**
     * Enable or disable the [ScanIndicatorView].
     *
     * @param enabled false to disable the [ScanIndicatorView]. Defaults to true.
     */
    fun setIndicatorEnabled(enabled: Boolean) {
        indicatorEnabled = enabled
        setScanIndicatorVisible(enabled)
    }

    fun setIndicatorStyle(style: ScanIndicatorView.Style?) {
        scanIndicatorView.setStyle(style)
    }

    /**
     * Sets indicator style in when in normalized style, ignoring all padding's and offsets
     *
     * setIndicatorStyle(ScanIndicatorView.Style.NORMALIZED)
     */
    fun setIndicatorNormalizedSize(left: Float, top: Float, right: Float, bottom: Float) {
        scanIndicatorView.setNormalizedSize(left, top, right, bottom)
    }

    private fun setScanIndicatorVisible(visible: Boolean) {
        if (indicatorEnabled && visible) {
            scanIndicatorView.visibility = VISIBLE
        } else {
            scanIndicatorView.visibility = GONE
        }
    }

    fun setIndicatorOffset(offsetX: Int, offsetY: Int) {
        scanIndicatorView.setOffset(offsetX, offsetY)
    }

    /**
     * Sets a callback to get scan results.
     *
     * @param callback the callback
     */
    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    interface Callback {
        fun onBarcodeDetected(barcode: Barcode?)
    }
}