package io.snabble.sdk.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraView
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import io.snabble.sdk.BarcodeFormat
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.UIUtils
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutionException


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
    private val supportedBarcodeFormats: MutableList<BarcodeFormat> = ArrayList()

    private var previewView: PreviewView

//    private var splashView: FrameLayout
//    private var cameraUnavailableView: TextView
//    private var scanIndicatorView: ScanIndicatorView

    private var indicatorEnabled = true
    private var barcodeDetector: BarcodeDetector
    private var callback: Callback? = null

    init {
        previewView = PreviewView(context)
        previewView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        addView(previewView)

//        scanIndicatorView = ScanIndicatorView(context)
//        scanIndicatorView.layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT)
//        addView(scanIndicatorView)
//
//        splashView = FrameLayout(context)
//        splashView.layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT)
//        splashView.setBackgroundResource(UIUtils.getColorByAttribute(context, android.R.attr.background))
//        addView(splashView)
//
//        cameraUnavailableView = TextView(context)
//        cameraUnavailableView.layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT)
//        cameraUnavailableView.setText(R.string.Snabble_Scanner_Camera_accessDenied)
//        cameraUnavailableView.gravity = Gravity.CENTER
//        cameraUnavailableView.setTextColor(UIUtils.getColorByAttribute(context, android.R.attr.textColorPrimary))
//        cameraUnavailableView.setBackgroundResource(UIUtils.getColorByAttribute(context, android.R.attr.background))
//        cameraUnavailableView.visibility = GONE
//
//        addView(cameraUnavailableView)

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

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview : Preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()


        var camera = cameraProvider.bindToLifecycle(UIUtils.getHostFragmentActivity(context), cameraSelector, preview)
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    fun stop() {

    }

    /**
     * Pauses the camera preview and barcode scanning, but keeps the camera intact.
     */
    fun pause() {

    }

    /**
     * Resumes the camera preview and barcode scanning.
     */
    fun resume() {

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
        set(enabled) {}

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
       // scanIndicatorView!!.setScale(scale)
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
        //scanIndicatorView!!.setStyle(style)
    }

    /**
     * Sets indicator style in when in normalized style, ignoring all padding's and offsets
     *
     * setIndicatorStyle(ScanIndicatorView.Style.NORMALIZED)
     */
    fun setIndicatorNormalizedSize(left: Float, top: Float, right: Float, bottom: Float) {
        //scanIndicatorView!!.setNormalizedSize(left, top, right, bottom)
    }

    private fun setScanIndicatorVisible(visible: Boolean) {
//        if (indicatorEnabled && visible) {
//            scanIndicatorView!!.visibility = VISIBLE
//        } else {
//            scanIndicatorView!!.visibility = GONE
//        }
    }

    fun setIndicatorOffset(offsetX: Int, offsetY: Int) {
        //scanIndicatorView!!.setOffset(offsetX, offsetY)
    }

    /**
     * Set to true, if you want to restrict barcode scanning to the center.
     *
     *
     * Default: true
     */
    fun setRestrictScanningToIndicator(restrictScanningToIndicator: Boolean) {}

    /**
     * Sets the multiplicaton value that is used to allow scanning outside of the indicator if
     * [.setRestrictScanningToIndicator] is set
     *
     * Default is 1.0
     */
    fun setRestrictionOvershoot(`val`: Float) {}

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