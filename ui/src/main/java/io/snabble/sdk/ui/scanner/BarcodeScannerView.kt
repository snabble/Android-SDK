package io.snabble.sdk.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.snabble.sdk.BarcodeFormat
import io.snabble.sdk.ui.BuildConfig
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class BarcodeScannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
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
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            .apply { gravity = Gravity.CENTER }
    }

    var barcodeDetector: BarcodeDetector
        private set

    private var isPaused: Boolean = false

    private var restrictScanningToIndicator: Boolean = true
    var callback: Callback? = null

    init {
        previewView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

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

        fakePauseView.apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(Color.BLACK)
            isVisible = false
        }
        addView(fakePauseView)

        addView(scanIndicatorView)

        cameraUnavailableView.apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setText(R.string.Snabble_Scanner_Camera_accessDenied)
            gravity = Gravity.CENTER
            setTextColor(UIUtils.getColorByAttribute(context, android.R.attr.textColorPrimary))
            setBackgroundResource(UIUtils.getColorByAttribute(context, android.R.attr.background))
            isVisible = false
        }
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
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
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
                cameraProviderFuture.addListener(
                    {
                        val cameraProvider = cameraProviderFuture.get()
                        bindPreview(cameraProvider)
                        startRequested = false
                    },
                    ContextCompat.getMainExecutor(context)
                )
            }
        }
    }

    private fun bindPreview(cp: ProcessCameraProvider) {
        val rotation = previewView.display?.rotation ?: return

        cameraProvider = cp
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageSize = Size(IMAGE_WIDTH, IMAGE_HEIGHT)

        this.preview = Preview.Builder()
            .setTargetResolution(imageSize)
            .setTargetRotation(rotation)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(imageSize)
            .setTargetRotation(rotation)
            .setImageQueueDepth(1)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        cameraExecutor?.let { executorService ->
            imageAnalyzer.setAnalyzer(executorService) { image ->
                processImage(image, rotation)
            }
        }

        cameraProvider?.unbindAll()

        try {
            val activity = UIUtils.getHostFragmentActivity(context)
            camera = cameraProvider?.bindToLifecycle(
                activity,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(previewView.surfaceProvider)
        } catch (ignored: Exception) {
            cameraUnavailableView.isVisible = true
            scanIndicatorView.isVisible = false
        }

        fakePauseView.isVisible = isPaused
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
        fakePauseView.isVisible = true
    }

    /**
     * Resumes the camera preview and barcode scanning.
     */
    fun resume() {
        isPaused = false
        fakePauseView.isVisible = false
    }

    private fun startAutoFocus() {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (camera != null && measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)

                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(width / 2f, height / 2f)
                    camera?.cameraControl?.startFocusAndMetering(
                        FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .setAutoCancelDuration(2, TimeUnit.SECONDS)
                            .build()
                    )
                }
            }
        })
    }

    private val imageSaver = ImageSaver()

    private fun processImage(image: ImageProxy, rotation: Int) {
        if (isPaused) {
            image.close()
            return
        }

        if (BuildConfig.DEBUG) imageSaver.save(image)

        val yPlane = image.planes.first()
        val yBytes = yPlane.buffer
        val yRowStride = yPlane.rowStride
        val cropRect = if (scanIndicatorView.isVisible && restrictScanningToIndicator) {
            val isImagePortrait = image.height >= image.width
            if (isImagePortrait) {
                val height = image.height / 3
                val top = (image.height / 2) - height
                val bottom = (image.height / 2) + height
                Rect(0, top, image.width, bottom)
            } else {
                val width = image.width / 4
                val left = (image.width / 2) - width
                val right = (image.width / 2) + width
                Rect(left, 0, right, image.height)
            }
        } else {
            Rect(0, 0, image.width, image.height)
        }
        Log.d(
            "xx",
            "hasIndicator: ${scanIndicatorView.isVisible}, " +
                    "isRestricted: $restrictScanningToIndicator, " +
                    "isPortrait: ${image.height >= image.width}, " +
                    "Rotation: ${previewView.display?.rotation}, " +
                    "CropRect: ${cropRect.left}/${cropRect.top} ${cropRect.width()}:${cropRect.height()}, " +
                    "Image: ${image.width}:${image.height}, " +
                    "Display: ${display.width}:${display.height}"
        )

        // Use the rowStride instead of the width to avoid analysis errors by ignoring the
        // attentional bytes at the end of each row. On one device we had a diff of 32 bytes
        // per row and the last image line has those 32 bytes missing. Since the last image
        // line should not contain any useful data we will just skip it to avoid accessing
        // the last 32 bytes which causes some out of bounds exceptions.
        // See also https://issuetracker.google.com/issues/134740191
        val barcode = barcodeDetector
            .detect(yBytes.toByteArray(), yRowStride, image.height - 1, 8, cropRect, rotation)

        if (barcode != null) {
            Dispatch.mainThread {
                if (!isPaused) {
                    callback?.onBarcodeDetected(barcode)
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
     * Removes all [BarcodeFormat]'s
     */
    fun removeAllBarcodeFormats() {
        supportedBarcodeFormats.clear()
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

    private companion object {

        const val IMAGE_WIDTH = 720
        const val IMAGE_HEIGHT = 1280
    }
}

class ImageSaver {

    private var savedImage: Boolean = false
    private var skipped = 0

    fun save(image: ImageProxy, oneShot: Boolean = true) {
        skipped++
        if (skipped <= SKIPS) return
        if (oneShot && savedImage) return

        try {
            val yBuffer: ByteBuffer = image.planes[0].buffer
            val uBuffer: ByteBuffer = image.planes[1].buffer
            val vBuffer: ByteBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // For NV21, U and V are swapped
            yBuffer[nv21, 0, ySize]
            vBuffer[nv21, ySize, vSize]
            uBuffer[nv21, ySize + vSize, uSize]

            val out = ByteArrayOutputStream()
            YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                .compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
            val imageFile = File(
                "${Environment.getExternalStorageDirectory().path}/Pictures",
                "${System.currentTimeMillis()}.jpeg"
            )
            BufferedOutputStream(FileOutputStream(imageFile)).use {
                it.write(out.toByteArray())
                it.flush()
            }
        } catch (e: Exception) {
            Log.w("ImageSaver", "${e.message}")
        }
        savedImage = true
    }

    private companion object {

        const val SKIPS = 3
    }
}
