package io.snabble.sdk.ui.checkout.routingtargets

import io.snabble.sdk.ui.SnabbleUI.project
import android.widget.FrameLayout
import io.snabble.sdk.Checkout.OnCheckoutStateChangedListener
import io.snabble.sdk.Checkout
import io.snabble.sdk.ui.scanner.BarcodeView
import android.widget.TextView
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.I18nUtils
import android.graphics.Bitmap
import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import io.snabble.sdk.ui.utils.UIUtils
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.snabble.sdk.utils.Logger
import kotlin.math.roundToInt

class RoutingTargetGatekeeperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), OnCheckoutStateChangedListener, LifecycleObserver {
    private var checkout: Checkout
    private var checkoutIdCode: BarcodeView
    private var cancel: View
    private var cancelProgress: View
    private var helperTextNoImage: TextView
    private var helperImage: ImageView
    private var upArrow: View

    private var currentState: Checkout.State? = null

    init {
        inflate(context, R.layout.snabble_view_routing_gatekeeper, this)
        val project = project
        checkout = project.checkout

        checkoutIdCode = findViewById(R.id.checkout_id_code)
        cancel = findViewById(R.id.cancel)
        cancelProgress = findViewById(R.id.cancel_progress)

        val helperText = findViewById<TextView>(R.id.helper_text)
        val text = I18nUtils.getString(resources, "Snabble.Payment.Online.message")

        if (text != null) {
            helperText.visibility = VISIBLE
            helperText.text = text
        } else {
            helperText.visibility = GONE
        }

        helperTextNoImage = findViewById(R.id.helper_text_no_image)
        helperImage = findViewById(R.id.helper_image)
        upArrow  = findViewById(R.id.arrow)

        cancel.setOnClickListener {
            abort()
        }

        val checkoutId = findViewById<TextView>(R.id.checkout_id)
        val id = checkout.id
        if (id != null && id.length >= 4) {
            checkoutId.text = id.substring(id.length - 4)
        } else {
            checkoutId.visibility = GONE
        }

        checkoutIdCode.visibility = VISIBLE

        val handoverInformation = checkout.checkoutProcess?.paymentInformation?.handoverInformation
        val qrCodeContent = checkout.checkoutProcess?.paymentInformation?.qrCodeContent
        val content = when {
            handoverInformation != null -> {
                handoverInformation
            }
            qrCodeContent != null -> {
                qrCodeContent
            }
            else -> {
                id
            }
        }
        Logger.d("QRCode content: $content")
        checkoutIdCode.setText(content)

        project.assets["checkout-online", { bitmap: Bitmap? -> setHelperImage(bitmap) }]

        onStateChanged(checkout.state)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        onStateChanged(checkout.state)
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val dm = resources.displayMetrics
            val dpHeight = (height / dm.density).roundToInt()
            if (dpHeight < 500) {
                if (helperImage.layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    helperImage.layoutParams = LinearLayout.LayoutParams(
                        (helperImage.width * 0.5f).roundToInt(),
                        (helperImage.height * 0.5f).roundToInt()
                    )
                }
            } else {
                helperImage.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkout.addOnCheckoutStateChangedListener(this)
        val fragmentActivity = UIUtils.getHostFragmentActivity(context)
        fragmentActivity?.lifecycle?.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        checkout.removeOnCheckoutStateChangedListener(this)
        val fragmentActivity = UIUtils.getHostFragmentActivity(context)
        fragmentActivity?.lifecycle?.removeObserver(this)
    }

    private fun abort() {
        checkout.abort()
        cancelProgress.visibility = VISIBLE
        cancel.isEnabled = false
    }

    override fun onStateChanged(state: Checkout.State) {
        if (state == currentState) {
            return
        }

        val hostActivity = UIUtils.getHostFragmentActivity(context)
        if (!hostActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }

        if (state == Checkout.State.PAYMENT_ABORT_FAILED) {
            cancelProgress.visibility = INVISIBLE
            cancel.isEnabled = true
            AlertDialog.Builder(context)
                .setTitle(R.string.Snabble_Payment_cancelError_title)
                .setMessage(R.string.Snabble_Payment_cancelError_message)
                .setPositiveButton(R.string.Snabble_OK) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .create()
                .show()
        }

        currentState = state
    }

    fun setHelperImage(bitmap: Bitmap?) {
        if (bitmap != null) {
            helperImage.setImageBitmap(bitmap)
            helperImage.visibility = VISIBLE
            upArrow.visibility = VISIBLE
            helperTextNoImage.visibility = GONE
        } else {
            helperImage.visibility = GONE
            upArrow.visibility = GONE
            helperTextNoImage.visibility = VISIBLE
        }
    }
}