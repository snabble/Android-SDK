package io.snabble.sdk.ui.checkout

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.*
import io.snabble.sdk.BarcodeFormat
import io.snabble.sdk.Checkout
import io.snabble.sdk.CheckoutApi
import io.snabble.sdk.Project
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.getFragmentActivity
import io.snabble.sdk.ui.utils.observeView

@Suppress("LeakingThis")
open class PaymentStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    LifecycleObserver {

    private var project: Project
    private var checkout: Checkout
    private var paymentItem: PaymentStatusItemView
    private var fulfillmentItem: PaymentStatusItemView
    private var exitTokenItem: PaymentStatusItemView
    private var receiptItem: PaymentStatusItemView

    init {
        inflate(getContext(), R.layout.snabble_view_payment_status, this)
        project = SnabbleUI.getProject()
        checkout = project.checkout
        paymentItem = findViewById(R.id.payment)
        fulfillmentItem = findViewById(R.id.fulfillment)
        exitTokenItem = findViewById(R.id.exit_token)
        receiptItem = findViewById(R.id.receipt)

        checkout.onCheckoutStateChanged.observeView(this) {
            onStateChanged(it)
        }

        checkout.onFulfillmentStateUpdated.observeView(this) {
            onFulfillmentStateChanged(it)
        }
    }

    private fun onStateChanged(state: Checkout.State?) {
        paymentItem.isVisible = true
        paymentItem.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Payment_title))

        when (state) {
            Checkout.State.PAYMENT_PROCESSING -> {
                paymentItem.state = PaymentStatusItemView.State.IN_PROGRESS
            }
            Checkout.State.PAYMENT_APPROVED -> {
                paymentItem.state = PaymentStatusItemView.State.SUCCESS
            }
            // TODO: be more explicit with the error handling - more detailed messages
            Checkout.State.PAYMENT_PROCESSING_ERROR,
            Checkout.State.DENIED_TOO_YOUNG,
            Checkout.State.DENIED_BY_PAYMENT_PROVIDER,
            Checkout.State.DENIED_BY_SUPERVISOR,
            Checkout.State.PAYMENT_ABORTED -> {
                paymentItem.state = PaymentStatusItemView.State.FAILED
                paymentItem.setText(resources.getString(R.string.Snabble_PaymentStatus_Payment_error))
                paymentItem.setAction(resources.getString(R.string.Snabble_PaymentStatus_Payment_tryAgain), {
                    // TODO "try again"
                })
            }
            else -> {}
        }

        checkout.checkoutProcess?.exitToken?.let {
            if (it.format != null) {
                val format = BarcodeFormat.parse(it.format)
                if (format != null) {
                    exitTokenItem.isVisible = true
                    exitTokenItem.state = PaymentStatusItemView.State.SUCCESS
                    exitTokenItem.setBarcode(it.value, format)
                } else {
                    exitTokenItem.isVisible = false
                }
            }
        }

        // TODO receipt
    }

    private fun onFulfillmentStateChanged(fulfillments: Array<CheckoutApi.Fulfillment>?) {
        val tobaccolandEWA = fulfillments?.find { it.type == "tobaccolandEWA" }
        tobaccolandEWA?.let {
            if (it.state.isOpen) {
                fulfillmentItem.isVisible = true
                fulfillmentItem.state = PaymentStatusItemView.State.IN_PROGRESS
            } else if (it.state.isFailure) {
                fulfillmentItem.isVisible = true
                fulfillmentItem.state = PaymentStatusItemView.State.FAILED
            } else if (it.state.isClosed) {
                fulfillmentItem.isVisible = true
                fulfillmentItem.state = PaymentStatusItemView.State.SUCCESS
            }
        }
    }
}