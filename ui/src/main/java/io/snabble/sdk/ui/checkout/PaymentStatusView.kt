package io.snabble.sdk.ui.checkout

import android.animation.LayoutTransition
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.*
import io.snabble.sdk.*
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.getFragmentActivity
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.utils.Dispatch
import android.os.Bundle
import android.widget.RelativeLayout
import io.snabble.sdk.ui.databinding.SnabbleViewPaymentStatusBinding
import io.snabble.sdk.ui.utils.executeUiAction


@Suppress("LeakingThis")
open class PaymentStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr),
    LifecycleObserver {

    private var isStopped: Boolean = false
    private var project: Project
    private var checkout: Checkout

    private val binding: SnabbleViewPaymentStatusBinding

    init {
        inflate(getContext(), R.layout.snabble_view_payment_status, this)
        project = SnabbleUI.getProject()
        checkout = project.checkout
        binding = SnabbleViewPaymentStatusBinding.bind(this)

        checkout.onCheckoutStateChanged.observeView(this) {
            onStateChanged(it)
        }

        checkout.onFulfillmentStateUpdated.observeView(this) {
            onFulfillmentStateChanged(it)
        }

        getFragmentActivity()?.lifecycle?.addObserver(this)
    }

    private fun onStateChanged(state: Checkout.State?) {
        binding.payment.isVisible = true
        binding.payment.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Payment_title))

        binding.receipt.isVisible = true
        binding.receipt.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Receipt_title))
        binding.receipt.state = PaymentStatusItemView.State.IN_PROGRESS

        when (state) {
            Checkout.State.PAYMENT_PROCESSING -> {
                binding.payment.state = PaymentStatusItemView.State.IN_PROGRESS
            }
            Checkout.State.PAYMENT_APPROVED -> {
                binding.payment.state = PaymentStatusItemView.State.SUCCESS
                startPollingForReceipts(checkout.checkoutProcess?.orderId)
            }
            // TODO: be more explicit with the error handling - more detailed messages
            Checkout.State.PAYMENT_PROCESSING_ERROR,
            Checkout.State.DENIED_TOO_YOUNG,
            Checkout.State.DENIED_BY_PAYMENT_PROVIDER,
            Checkout.State.DENIED_BY_SUPERVISOR,
            Checkout.State.PAYMENT_ABORTED -> {
                binding.payment.state = PaymentStatusItemView.State.FAILED
                binding.payment.setText(resources.getString(R.string.Snabble_PaymentStatus_Payment_error))
                binding.payment.setAction(resources.getString(R.string.Snabble_PaymentStatus_Payment_tryAgain), {
                    executeUiAction(SnabbleUI.Action.GO_BACK, null)
                })
                binding.receipt.state = PaymentStatusItemView.State.FAILED // TODO indeterminate
            }
            else -> {
                executeUiAction(SnabbleUI.Action.GO_BACK, null)
            }
        }

        checkout.checkoutProcess?.exitToken?.let {
            if (it.value != null && it.format != null) {
                val format = BarcodeFormat.parse(it.format)
                if (format != null) {
                    binding.exitToken.isVisible = true
                    binding.exitToken.state = PaymentStatusItemView.State.SUCCESS
                    binding.exitToken.setTitle(resources.getString(R.string.Snabble_PaymentStatus_ExitCode_title))

                    binding.exitTokenBarcode.isVisible = true
                    binding.exitTokenBarcode.setFormat(format)
                    binding.exitTokenBarcode.setText(it.value)
                } else {
                    binding.exitToken.isVisible = false
                    binding.exitTokenBarcode.isVisible = false
                }
            }
        }

        binding.statusContainer.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        onStateChanged(checkout.state)
        isStopped = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        isStopped = true
    }

    private fun startPollingForReceipts(orderId: String?) {
        if (orderId == null || isStopped) {
            return
        }

        val receipts = Snabble.getInstance().receipts
        receipts.getReceiptInfos(object : Receipts.ReceiptInfoCallback {
            override fun success(receiptInfos: Array<out ReceiptInfo>?) {
                val receipt = receiptInfos?.filter { it.id == orderId }?.firstOrNull()
                if (receipt != null) {
                    Dispatch.mainThread {
                        binding.receipt.state = PaymentStatusItemView.State.SUCCESS
                    }
                } else {
                    Dispatch.mainThread({
                        startPollingForReceipts(orderId)
                    }, 2000)
                }
            }

            override fun failure() {
                Dispatch.mainThread({
                    startPollingForReceipts(orderId)
                }, 2000)
            }
        })
    }

    private fun onFulfillmentStateChanged(fulfillments: Array<CheckoutApi.Fulfillment>?) {
        val tobaccolandEWA = fulfillments?.find { it.type == "tobaccolandEWA" }
        tobaccolandEWA?.let {
            if (it.state.isOpen) {
                binding.fulfillment.isVisible = true
                binding.fulfillment.state = PaymentStatusItemView.State.IN_PROGRESS
            } else if (it.state.isFailure) {
                binding.fulfillment.isVisible = true
                binding.fulfillment.state = PaymentStatusItemView.State.FAILED
            } else if (it.state.isClosed) {
                binding.fulfillment.isVisible = true
                binding.fulfillment.state = PaymentStatusItemView.State.SUCCESS
            }
        }
    }
}