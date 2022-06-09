package io.snabble.sdk.ui.checkout.routingtargets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import io.snabble.sdk.checkout.Checkout
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkout.CheckoutState
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.scanner.BarcodeView
import io.snabble.sdk.ui.utils.I18nUtils
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.ui.utils.setTextOrHide
import io.snabble.sdk.utils.Logger
import kotlin.math.roundToInt

class RoutingTargetSupervisorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var checkout: Checkout
    private var checkoutIdCode: BarcodeView
    private var cancel: View
    private var cancelProgress: View
    private var helperTextNoImage: TextView
    private var helperImage: ImageView

    private var currentState: CheckoutState? = null

    init {
        inflate(context, R.layout.snabble_view_routing_supervisor, this)
        val project = requireNotNull(Snabble.checkedInProject.value)
        checkout = project.checkout

        checkoutIdCode = findViewById(R.id.checkout_id_code)
        cancel = findViewById(R.id.cancel)
        cancelProgress = findViewById(R.id.cancel_progress)

        val helperText = findViewById<TextView>(R.id.helper_text)
        val text = I18nUtils.getString(resources, "Snabble.Payment.Online.message")
        helperText.setTextOrHide(text)

        helperTextNoImage = findViewById(R.id.helper_text_no_image)
        helperImage = findViewById(R.id.helper_image)

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
        checkoutIdCode.setText(id)

        val qrCodeContent = checkout.checkoutProcess?.paymentInformation?.qrCodeContent
        val content = when {
            qrCodeContent != null -> {
                qrCodeContent
            }
            else -> {
                id
            }
        }
        Logger.d("QRCode content: $content")
        checkoutIdCode.setText(content)

        project.assets.get("checkout-online") { bitmap: Bitmap? ->
            setHelperImage(bitmap)
        }

        checkout.state.observeView(this) {
            onStateChanged(it)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // if layout is starved for space, hide the helper image
        if (changed) {
            val dm = resources.displayMetrics
            val dpHeight = (height / dm.density).roundToInt()
            if (dpHeight < 500) {
                if (helperImage.layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    helperImage.layoutParams = LinearLayout.LayoutParams(
                        helperImage.width / 2,
                        helperImage.height / 2
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

    private fun abort() {
        checkout.abort()
        cancelProgress.visibility = VISIBLE
        cancel.isEnabled = false
    }

    private fun onStateChanged(state: CheckoutState) {
        if (state == currentState) {
            return
        }

        val hostActivity = UIUtils.getHostFragmentActivity(context)
        if (!hostActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }

        if (state == CheckoutState.PAYMENT_ABORT_FAILED) {
            cancelProgress.visibility = INVISIBLE
            cancel.isEnabled = true
        }

        currentState = state
    }

    fun setHelperImage(bitmap: Bitmap?) {
        if (bitmap != null) {
            helperImage.setImageBitmap(bitmap)
            helperImage.visibility = VISIBLE
            helperTextNoImage.visibility = GONE
        } else {
            helperImage.visibility = GONE
            helperTextNoImage.visibility = VISIBLE
        }
    }
}