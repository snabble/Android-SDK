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
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.core.view.isInvisible
import io.snabble.sdk.ui.databinding.SnabbleViewPaymentStatusBinding
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.executeUiAction
import io.snabble.sdk.utils.Logger
import android.text.Editable

import android.text.TextWatcher
import androidx.annotation.NonNull


@Suppress("LeakingThis")
open class PaymentStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr),
    LifecycleObserver {

    private var isStopped: Boolean = false
    private var project: Project
    private var checkout: Checkout

    private val binding: SnabbleViewPaymentStatusBinding
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

        }
    }
    private var ratingMessage: String? = null
    private var lastState: Checkout.State? = null

    init {
        inflate(getContext(), R.layout.snabble_view_payment_status, this)
        project = SnabbleUI.getProject()
        checkout = project.checkout
        binding = SnabbleViewPaymentStatusBinding.bind(this)

        binding.back.isEnabled = false
        binding.back.setOnClickListener {
            executeUiAction(SnabbleUI.Action.SHOW_PAYMENT_DONE, null)
        }

        checkout.onCheckoutStateChanged.observeView(this) {
            onStateChanged(it)
        }

        checkout.onFulfillmentStateUpdated.observeView(this) {
            onFulfillmentStateChanged(it)
        }

        binding.rating1.setOnClickListener {
            ratingMessage = ""
            binding.inputBadRatingLayout.isVisible = true
            binding.inputBadRatingLayout.getEditText()?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    ratingMessage = s.toString()
                }
            })
        }

        binding.rating2.setOnClickListener {
            sendRating("2")
        }

        binding.rating3.setOnClickListener {
            sendRating("3")
        }

        val activity = getFragmentActivity()
        activity?.lifecycle?.addObserver(this)
        activity?.onBackPressedDispatcher?.addCallback(backPressedCallback)
    }

    private fun sendRating(rating: String) {
        Logger.d("Send rating " + rating + " message = " + ratingMessage)

        project.events.analytics("rating", rating, ratingMessage ?: "")
        Telemetry.event(Telemetry.Event.Rating, rating)
        binding.ratingTitle.setText(R.string.Snabble_PaymentStatus_Ratings_thanks)
        binding.ratingContainer.isInvisible = true
        ratingMessage = null
    }

    private fun onStateChanged(state: Checkout.State?) {
        if (lastState == state) {
            return
        }

        lastState = state

        binding.payment.isVisible = true
        binding.payment.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Payment_title))

        binding.receipt.isVisible = true
        binding.receipt.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Receipt_title))
        binding.receipt.state = PaymentStatusItemView.State.IN_PROGRESS

        when (state) {
            Checkout.State.PAYMENT_PROCESSING,
            Checkout.State.VERIFYING_PAYMENT_METHOD -> {
                binding.payment.state = PaymentStatusItemView.State.IN_PROGRESS
            }
            Checkout.State.PAYMENT_APPROVED -> {
                binding.title.text = resources.getString(R.string.Snabble_PaymentStatus_Title_success)
                binding.image.setImageResource(R.drawable.snabble_ic_payment_success_big)
                binding.image.isVisible = true
                binding.progress.isVisible = false
                binding.payment.state = PaymentStatusItemView.State.SUCCESS
                startPollingForReceipts(checkout.checkoutProcess?.orderId)
                binding.back.isEnabled = true
                backPressedCallback.isEnabled = false
                binding.ratingLayout.isVisible = true
            }
            // TODO: be more explicit with the error handling - more detailed messages
            Checkout.State.PAYMENT_PROCESSING_ERROR,
            Checkout.State.DENIED_TOO_YOUNG,
            Checkout.State.DENIED_BY_PAYMENT_PROVIDER,
            Checkout.State.DENIED_BY_SUPERVISOR,
            Checkout.State.PAYMENT_ABORTED -> {
                binding.payment.state = PaymentStatusItemView.State.FAILED
                binding.payment.setText(resources.getString(R.string.Snabble_PaymentStatus_Payment_error))
                binding.payment.setAction(resources.getString(R.string.Snabble_PaymentStatus_Payment_tryAgain)) {
                    executeUiAction(SnabbleUI.Action.GO_BACK, null)
                }
                binding.title.text = resources.getString(R.string.Snabble_PaymentStatus_Title_error)
                binding.image.isVisible = true
                binding.image.setImageResource(R.drawable.snabble_ic_payment_error_big)
                binding.progress.isVisible = false
                binding.receipt.state = PaymentStatusItemView.State.NOT_EXECUTED
                binding.back.isEnabled = true
                backPressedCallback.isEnabled = false
            }
            Checkout.State.REQUEST_PAYMENT_AUTHORIZATION_TOKEN -> {
                val price = checkout.verifiedOnlinePrice;
                if (price != -1) {
                    val googlePayHelper = project.googlePayHelper
                    if (googlePayHelper != null) {
                        googlePayHelper.requestPayment(price)
                    } else {
                        checkout.abort()
                    }
                } else {
                    checkout.abort()
                }
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

        binding.statusContainer.layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        onStateChanged(checkout.state)
        isStopped = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        isStopped = true

        if (ratingMessage != null) {
            sendRating("1")
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (visibility != VISIBLE) {
            if (ratingMessage != null) {
                sendRating("1")
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (ratingMessage != null) {
            sendRating("1")
        }
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
                binding.fulfillment.setText("")
                binding.fulfillment.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                binding.fulfillment.state = PaymentStatusItemView.State.IN_PROGRESS
            } else if (it.state.isFailure) {
                binding.fulfillment.isVisible = true
                binding.fulfillment.setText(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_error))
                binding.fulfillment.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                binding.fulfillment.state = PaymentStatusItemView.State.FAILED
            } else if (it.state.isClosed) {
                binding.fulfillment.isVisible = true
                binding.fulfillment.setText(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_message))
                binding.fulfillment.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                binding.fulfillment.state = PaymentStatusItemView.State.SUCCESS
            }
        }
    }
}