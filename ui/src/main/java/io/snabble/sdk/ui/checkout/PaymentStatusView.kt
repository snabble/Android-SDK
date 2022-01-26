package io.snabble.sdk.ui.checkout

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.*
import io.snabble.sdk.*
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.getFragmentActivity
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.utils.Dispatch
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import io.snabble.sdk.ui.databinding.SnabbleViewPaymentStatusBinding
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.executeUiAction
import android.text.Editable

import android.text.TextWatcher
import android.view.ViewGroup
import io.snabble.sdk.PaymentOriginCandidateHelper.PaymentOriginCandidate
import io.snabble.sdk.PaymentOriginCandidateHelper.PaymentOriginCandidateAvailableListener
import io.snabble.sdk.ui.payment.SEPACardInputActivity
import io.snabble.sdk.ui.utils.requireFragmentActivity


class PaymentStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr),
    LifecycleObserver, PaymentOriginCandidateAvailableListener {

    private var isStopped: Boolean = false
    private val project: Project
    private val checkout: Checkout

    private val binding: SnabbleViewPaymentStatusBinding
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

        }
    }

    private var ratingMessage: String? = null
    private var lastState: Checkout.State? = null

    private var paymentOriginCandidate: PaymentOriginCandidate? = null
    private var paymentOriginCandidateHelper: PaymentOriginCandidateHelper
    private var ignoreStateChanges = false
    private var hasShownSEPAInput = false

    init {
        inflate(getContext(), R.layout.snabble_view_payment_status, this)
        clipChildren = false

        project = SnabbleUI.project
        checkout = project.checkout
        paymentOriginCandidateHelper = PaymentOriginCandidateHelper(project)
        paymentOriginCandidateHelper.addPaymentOriginCandidateAvailableListener(this)
        binding = SnabbleViewPaymentStatusBinding.bind(this)

        binding.back.isEnabled = false
        binding.back.setOnClickListener {
            ignoreStateChanges = true
            val state = lastState
            checkout.reset()
            requireFragmentActivity().finish()

            if (state == Checkout.State.PAYMENT_APPROVED) {
                executeUiAction(SnabbleUI.Event.SHOW_CHECKOUT_DONE)
            }
        }

        checkout.checkoutState.observeView(this) {
            onStateChanged(it)
        }

        checkout.fulfillmentState.observeView(this) {
            onFulfillmentStateChanged(it)
        }

        binding.rating1.setOnClickListener {
            ratingMessage = ""
            binding.inputBadRatingLayout.isVisible = true
            binding.inputBadRatingLayout.editText
                ?.addTextChangedListener(object : TextWatcher {
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

        binding.addIbanLayout.isVisible = false
        binding.addIbanButton.setOnClickListener {
            val paymentOriginCandidate = paymentOriginCandidate
            val intent = Intent(getContext(), SEPACardInputActivity::class.java)
            intent.putExtra(SEPACardInputActivity.ARG_PAYMENT_ORIGIN_CANDIDATE, paymentOriginCandidate)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            getContext()?.startActivity(intent)
            binding.addIbanLayout.isVisible = false
            hasShownSEPAInput = true
        }

        val activity = getFragmentActivity()
        activity?.lifecycle?.addObserver(this)
        activity?.onBackPressedDispatcher?.addCallback(backPressedCallback)
    }

    private fun sendRating(rating: String) {
        project.events.analytics("rating", rating, ratingMessage ?: "")
        Telemetry.event(Telemetry.Event.Rating, rating)
        binding.ratingTitle.setText(R.string.Snabble_PaymentStatus_Ratings_thanks)
        binding.ratingContainer.isInvisible = true
        ratingMessage = null
    }

    private fun onStateChanged(state: Checkout.State?) {
        if (lastState == state || ignoreStateChanges) {
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
                binding.title.text =
                    resources.getString(R.string.Snabble_PaymentStatus_Title_success)
                binding.image.setImageResource(R.drawable.snabble_ic_payment_success_big)
                binding.image.isVisible = true
                binding.progress.isVisible = false
                binding.payment.state = PaymentStatusItemView.State.SUCCESS
                if (checkout.checkoutProcess?.orderId != null) {
                    startPollingForReceipts(checkout.checkoutProcess?.orderId)
                } else {
                    binding.receipt.state = PaymentStatusItemView.State.NOT_EXECUTED
                }
                binding.back.isEnabled = true
                backPressedCallback.isEnabled = false
                binding.ratingLayout.isVisible = true
                paymentOriginCandidateHelper.startPollingIfLinkIsAvailable(checkout.checkoutProcess)
            }
            Checkout.State.PAYMENT_PROCESSING_ERROR -> {
                Telemetry.event(Telemetry.Event.CheckoutDeniedByPaymentProvider);
                handlePaymentAborted()
            }
            Checkout.State.DENIED_TOO_YOUNG -> {
                Telemetry.event(Telemetry.Event.CheckoutDeniedByTooYoung);
                handlePaymentAborted()
            }
            Checkout.State.DENIED_BY_PAYMENT_PROVIDER -> {
                Telemetry.event(Telemetry.Event.CheckoutDeniedByPaymentProvider);
                handlePaymentAborted()
            }
            Checkout.State.DENIED_BY_SUPERVISOR -> {
                Telemetry.event(Telemetry.Event.CheckoutDeniedBySupervisor);
                handlePaymentAborted()
            }
            Checkout.State.PAYMENT_ABORTED -> {
                Telemetry.event(Telemetry.Event.CheckoutAbortByUser);
                handlePaymentAborted()
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
                    SnabbleUI.executeAction(requireFragmentActivity(), SnabbleUI.Event.EXIT_TOKEN_AVAILABLE,
                        Bundle().apply {
                            putString("token", it.value)
                            putString("format", it.format)
                        }
                    )

                    binding.exitTokenContainer.isVisible = true
                    binding.exitToken.state = PaymentStatusItemView.State.SUCCESS
                    binding.exitToken.setTitle(resources.getString(R.string.Snabble_PaymentStatus_ExitCode_title))
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

    private fun handlePaymentAborted() {
        binding.payment.state = PaymentStatusItemView.State.FAILED
        binding.payment.setText(resources.getString(R.string.Snabble_PaymentStatus_Payment_error))
        binding.payment.setAction(resources.getString(R.string.Snabble_PaymentStatus_Payment_tryAgain)) {
            project.shoppingCart.generateNewUUID()
            requireFragmentActivity().finish()
        }
        binding.title.text = resources.getString(R.string.Snabble_PaymentStatus_Title_error)
        binding.image.isVisible = true
        binding.image.setImageResource(R.drawable.snabble_ic_payment_error_big)
        binding.progress.isVisible = false
        binding.receipt.state = PaymentStatusItemView.State.NOT_EXECUTED
        binding.back.isEnabled = true
        backPressedCallback.isEnabled = false
    }

    private fun startPollingForPaymentOriginCandidate() {
        if (hasShownSEPAInput) {
            return
        }

        paymentOriginCandidateHelper.addPaymentOriginCandidateAvailableListener(this)
        if (checkout.state == Checkout.State.PAYMENT_APPROVED) {
            paymentOriginCandidateHelper.startPollingIfLinkIsAvailable(checkout.checkoutProcess)
        }
    }

    private fun stopPollingForPaymentOriginCandidate(){
        paymentOriginCandidateHelper.removePaymentOriginCandidateAvailableListener(this)
        paymentOriginCandidateHelper.stopPolling()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        startPollingForPaymentOriginCandidate()

        onStateChanged(checkout.state)
        isStopped = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        stopPollingForPaymentOriginCandidate()

        isStopped = true

        if (ratingMessage != null) {
            sendRating("1")
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (visibility != VISIBLE && ratingMessage != null) {
            sendRating("1")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        startPollingForPaymentOriginCandidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (ratingMessage != null) {
            sendRating("1")
        }

        stopPollingForPaymentOriginCandidate()
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
        fulfillments?.forEach {
            var itemView = binding.fulfillmentContainer.findViewWithTag<PaymentStatusItemView>(it.type)
            if (itemView == null) {
                itemView = PaymentStatusItemView(context)
                itemView.tag = it.type
                binding.fulfillmentContainer.addView(itemView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            if (it.type == "tobaccolandEWA") {
                if (it.state.isOpen) {
                    itemView.setText(null)
                    itemView.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                    itemView.state = PaymentStatusItemView.State.IN_PROGRESS
                } else if (it.state.isFailure) {
                    if (it.state == FulfillmentState.ALLOCATION_FAILED
                        || it.state == FulfillmentState.ALLOCATION_TIMED_OUT) {
                        handlePaymentAborted()
                    }

                    itemView.setText(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_error))
                    itemView.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                    itemView.state = PaymentStatusItemView.State.FAILED
                } else if (it.state.isClosed) {
                    itemView.setText(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_message))
                    itemView.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                    itemView.state = PaymentStatusItemView.State.SUCCESS
                }
            } else {
                itemView.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Fulfillment_title))

                if (it.state.isOpen) {
                    itemView.state = PaymentStatusItemView.State.IN_PROGRESS
                } else if (it.state.isFailure) {
                    itemView.state = PaymentStatusItemView.State.FAILED
                } else if (it.state.isClosed) {
                    itemView.state = PaymentStatusItemView.State.SUCCESS
                }
            }
        }
    }

    override fun onPaymentOriginCandidateAvailable(paymentOriginCandidate: PaymentOriginCandidate?) {
        if (paymentOriginCandidate != null) {
            binding.addIbanLayout.isVisible = true
            this.paymentOriginCandidate = paymentOriginCandidate
        }
    }
}