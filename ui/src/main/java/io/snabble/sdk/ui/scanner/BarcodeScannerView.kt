package io.snabble.sdk.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Size
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.snabble.sdk.BarcodeFormat
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class BarcodeScannerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var startRequested: Boolean = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService? = null
    private val supportedBarcodeFormats: MutableList<BarcodeFormat> = ArrayList()

    private val previewView = PreviewView(context)
    private val fakePauseView = ImageView(context)
    private var cameraUnavailableView = TextView(context)
    private var scanIndicatorView = ImageView(context).apply {
        setImageResource(R.drawable.snabble_ic_barcode)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
    }

    private var barcodeDetector: BarcodeDetector

    private var isPaused: Boolean = false

    var restrictScanningToIndicator: Boolean = true
    var callback: Callback? = null

    init {
        previewView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)

        // WORKAROUND: prevents black screen issue when resuming from multi-window
        // (and possible other cases where onresume is used)
        //
        // known affected devices:
        //  - Google Pixel 4a (5G)
        //
        // this is a bug in CameraX.
        // using COMPATIBLE forces the use of TextureView instead of SurfaceView
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        addView(previewView)

        fakePauseView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        fakePauseView.setBackgroundColor(Color.BLACK)
        fakePauseView.visibility = View.GONE
        addView(fakePauseView)

        addView(scanIndicatorView)

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

        startAutoFocus()
    }

    /**
     * Starts the camera preview, requires runtime permission to the camera
     *
     * @throws RuntimeException if no permission to access the camera is present
     */
    fun start() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            throw RuntimeException("Missing camera permission")
        }

        Logger.d("start")

        if (!startRequested) {
            startRequested = true
            barcodeDetector.reset()
            barcodeDetector.setup(supportedBarcodeFormats)
            isPaused = false

            previewView.post {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(cameraProvider)
                    startRequested = false
                }, ContextCompat.getMainExecutor(context))
            }
        }
    }

    private fun bindPreview(cp: ProcessCameraProvider) {
        previewView.display?.rotation?.let { rotation ->
            cameraProvider = cp
            cameraExecutor = Executors.newSingleThreadExecutor()

            val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

            this.preview = Preview.Builder()
                    .setTargetResolution(Size(768, 1024))
                    .setTargetRotation(rotation)
                    .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(768, 1024))
                    .setTargetRotation(rotation)
                    .setImageQueueDepth(1)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        cameraExecutor?.let { exec ->
                            it.setAnalyzer(exec, { image ->
                                processImage(image)
                            })
                        }
                    }

            cameraProvider?.unbindAll()

            try {
                val activity = UIUtils.getHostFragmentActivity(context)
                camera = cameraProvider?.bindToLifecycle(activity,
                    cameraSelector,
                    preview,
                    imageAnalyzer)

                preview?.setSurfaceProvider(previewView.surfaceProvider)
            } catch (e: Exception) {
                cameraUnavailableView.visibility = View.VISIBLE
                scanIndicatorView.visibility = View.GONE
            }

            if (isPaused) {
                fakePauseView.visibility = View.VISIBLE
            } else {
                fakePauseView.visibility = View.GONE
            }
        }
    }

    /**
     * Stops the camera preview.
     */
    fun stop() {
        pause()
        cameraExecutor?.shutdown()
        cameraProvider?.unbindAll()
    }

    /**
     * Pauses the camera preview and barcode scanning, but keeps the camera intact.
     */
    fun pause() {
        isPaused = true
        fakePauseView.setImageBitmap(previewView.bitmap)
        fakePauseView.visibility = View.VISIBLE
    }

    /**
     * Resumes the camera preview and barcode scanning.
     */
    fun resume() {
        isPaused = false
        fakePauseView.visibility = View.GONE
    }

    private fun startAutoFocus() {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (camera != null && measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)

                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(width / 2f, height / 2f)
                    camera?.cameraControl?.startFocusAndMetering(
                            FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).apply {
                                setAutoCancelDuration(2, TimeUnit.SECONDS)
                            }.build()
                    )
                }
            }
        })
    }

    private fun processImage(image: ImageProxy) {
        if (isPaused) {
            image.close()
            return
        }

        if (image.format == ImageFormat.YUV_420_888
                || image.format == ImageFormat.YUV_422_888
                || image.format == ImageFormat.YUV_444_888
                && image.planes.size == 3) {
            val luminanceBytes = image.planes[0].buffer // Y plane of a YUV buffer
            val rect = if (scanIndicatorView.visibility == View.VISIBLE && restrictScanningToIndicator) {
                val height = image.height / 3
                val top = (image.height / 2) - height
                val bottom = (image.height / 2) + height
                Rect(0, top, image.width, bottom)
            } else {
                Rect(0, 0, image.width, image.height)
            }

            previewView.display?.rotation?.let { rotation ->
                val barcode = barcodeDetector.detect(luminanceBytes.toByteArray(),
                        image.width, image.height, 8, rect, rotation)

                if (barcode != null) {
                    Dispatch.mainThread {
                        if (!isPaused) {
                            callback?.onBarcodeDetected(barcode)
                        }
                    }
                }
            }
        }

        image.close()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    /**
     * Enabled or disables the torch on the camera. The state of the torch is bound to the lifecycle
     * of the BarcodeScannerView.
     */
    var isTorchEnabled: Boolean
        get() = camera?.cameraInfo?.torchState?.value == TorchState.ON
        set(enabled) {
            camera?.cameraControl?.enableTorch(enabled)
        }

    /**
     * Adds a [BarcodeFormat] that should get detected.
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
     * Enabled or disables the scan indicator.
     */
    var indicatorEnabled: Boolean
        get() = scanIndicatorView.isVisible
        set(value) {
            scanIndicatorView.isVisible = value
        }

    /**
     * Sets the offset of the scan indicator, in pixels.
     */
    fun setIndicatorOffset(offsetX: Int, offsetY: Int) {
        scanIndicatorView.translationX = offsetX.toFloat()
        scanIndicatorView.translationY = offsetY.toFloat()
    }

    interface Callback {
        fun onBarcodeDetected(barcode: Barcode?)
    }
}