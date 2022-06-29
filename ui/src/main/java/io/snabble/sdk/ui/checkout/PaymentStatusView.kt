package io.snabble.sdk.ui.checkout

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import io.snabble.sdk.*
import io.snabble.sdk.ui.checkout.PaymentOriginCandidateHelper.PaymentOriginCandidate
import io.snabble.sdk.ui.checkout.PaymentOriginCandidateHelper.PaymentOriginCandidateAvailableListener
import io.snabble.sdk.checkout.Checkout
import io.snabble.sdk.checkout.CheckoutState
import io.snabble.sdk.checkout.Fulfillment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.payment.SEPACardInputActivity
import io.snabble.sdk.ui.scanner.BarcodeView
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.executeUiAction
import io.snabble.sdk.ui.utils.getFragmentActivity
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.ui.utils.requireFragmentActivity
import io.snabble.sdk.utils.Dispatch


class PaymentStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr),
    LifecycleObserver, PaymentOriginCandidateAvailableListener {
    init {
        inflate(getContext(), R.layout.snabble_view_payment_status, this)
    }

    private val image = findViewById<ImageView>(R.id.image)
    private val back = findViewById<MaterialButton>(R.id.back)
    private val rating1 = findViewById<ImageView>(R.id.rating_1)
    private val rating2 = findViewById<ImageView>(R.id.rating_2)
    private val rating3 = findViewById<ImageView>(R.id.rating_3)
    private val ratingLayout = findViewById<View>(R.id.rating_layout)
    private val inputBadRatingLayout = findViewById<TextInputLayout>(R.id.input_bad_rating_layout)
    private var addIbanLayout = findViewById<LinearLayout>(R.id.add_iban_layout)
    private var addIbanButton = findViewById<Button>(R.id.add_iban_button)
    private var ratingTitle = findViewById<TextView>(R.id.rating_title)
    private var ratingContainer = findViewById<LinearLayout>(R.id.rating_container)
    private var payment = findViewById<PaymentStatusItemView>(R.id.payment)
    private var receipt = findViewById<PaymentStatusItemView>(R.id.receipt)
    private var title = findViewById<TextView>(R.id.title)
    private var progress = findViewById<ProgressBar>(R.id.progress)
    private var exitTokenContainer = findViewById<LinearLayout>(R.id.exit_token_container)
    private var exitToken = findViewById<PaymentStatusItemView>(R.id.exit_token)
    private var exitTokenBarcode = findViewById<BarcodeView>(R.id.exit_token_barcode)
    private var statusContainer = findViewById<LinearLayout>(R.id.status_container)
    private var fulfillmentContainer = findViewById<LinearLayout>(R.id.fulfillment_container)

    private var isStopped: Boolean = false
    private lateinit var project: Project
    private lateinit var checkout: Checkout

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

        }
    }

    private var ratingMessage: String? = null
    private var lastState: CheckoutState? = null

    private var paymentOriginCandidate: PaymentOriginCandidate? = null
    private lateinit var paymentOriginCandidateHelper: PaymentOriginCandidateHelper
    private var ignoreStateChanges = false
    private var hasShownSEPAInput = false

    init {
        clipChildren = false

        if (!isInEditMode) {
            project = requireNotNull(Snabble.checkedInProject.value)
            checkout = project.checkout

            Snabble.checkedInProject.observeView(this) {
                project = requireNotNull(it)
                checkout = project.checkout
            }

            update()
        }
    }

    private fun update() {
        paymentOriginCandidateHelper = PaymentOriginCandidateHelper(project)
        paymentOriginCandidateHelper.addPaymentOriginCandidateAvailableListener(this)

        back.text = resources.getString(R.string.Snabble_Cancel)
        back.setOnClickListener {
            val state = lastState

            if (state == CheckoutState.PAYMENT_APPROVED) {
                ignoreStateChanges = true
                checkout.reset()
                requireFragmentActivity().finish()

                if (state == CheckoutState.PAYMENT_APPROVED) {
                    executeUiAction(SnabbleUI.Event.SHOW_CHECKOUT_DONE)
                }

                project.coupons.update()
            } else {
                checkout.abort()
            }
        }

        checkout.state.observeView(this) {
            onStateChanged(it)
        }

        checkout.fulfillmentState.observeView(this) {
            onFulfillmentStateChanged(it)
        }

        rating1.setOnClickListener {
            ratingMessage = ""
            inputBadRatingLayout.isVisible = true
            inputBadRatingLayout.editText?.addTextChangedListener { s ->
                ratingMessage = s.toString()
            }
        }

        rating2.setOnClickListener {
            sendRating("2")
        }

        rating3.setOnClickListener {
            sendRating("3")
        }

        addIbanLayout.isVisible = false
        addIbanButton.setOnClickListener {
            val paymentOriginCandidate = paymentOriginCandidate
            val intent = Intent(context, SEPACardInputActivity::class.java)
            intent.putExtra(SEPACardInputActivity.ARG_PAYMENT_ORIGIN_CANDIDATE, paymentOriginCandidate)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            context?.startActivity(intent)
            addIbanLayout.isVisible = false
            hasShownSEPAInput = true
        }

        val activity = getFragmentActivity()
        activity?.lifecycle?.addObserver(this)
        activity?.onBackPressedDispatcher?.addCallback(backPressedCallback)
    }

    private fun sendRating(rating: String) {
        project.events.analytics("rating", rating, ratingMessage ?: "")
        Telemetry.event(Telemetry.Event.Rating, rating)
        ratingTitle.setText(R.string.Snabble_PaymentStatus_Ratings_thanks)
        ratingContainer.isInvisible = true
        ratingMessage = null
    }

    private fun onStateChanged(state: CheckoutState?) {
        if (ignoreStateChanges) {
            return
        }

        checkForExitToken()

        if (lastState == state) {
            return
        }

        lastState = state

        payment.isVisible = true
        payment.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Payment_title))

        receipt.isVisible = true
        receipt.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Receipt_title))
        receipt.state = PaymentStatusItemView.State.IN_PROGRESS

        when (state) {
            CheckoutState.PAYMENT_PROCESSING,
            CheckoutState.VERIFYING_PAYMENT_METHOD -> {
                payment.state = PaymentStatusItemView.State.IN_PROGRESS
            }
            CheckoutState.PAYMENT_APPROVED -> {
                title.text = resources.getString(R.string.Snabble_PaymentStatus_Title_success)
                image.setImageResource(R.drawable.snabble_ic_payment_success_big)
                image.isVisible = true
                progress.isVisible = false
                payment.state = PaymentStatusItemView.State.SUCCESS
                val checkoutProcess = checkout.checkoutProcess
                if (checkoutProcess?.orderId != null) {
                    startPollingForReceipts(checkout.checkoutProcess?.orderId)
                } else {
                    receipt.state = PaymentStatusItemView.State.NOT_EXECUTED
                }
                back.text = resources.getString(R.string.Snabble_PaymentStatus_back)
                backPressedCallback.isEnabled = false
                ratingLayout.isVisible = true
                paymentOriginCandidateHelper.startPollingIfLinkIsAvailable(checkout.checkoutProcess)
            }
            CheckoutState.PAYMENT_PROCESSING_ERROR -> {
                Telemetry.event(Telemetry.Event.CheckoutDeniedByPaymentProvider)
                handlePaymentAborted()
            }
            CheckoutState.DENIED_TOO_YOUNG -> {
                Telemetry.event(Telemetry.Event.CheckoutDeniedByTooYoung)
                handlePaymentAborted()
            }
            CheckoutState.DENIED_BY_PAYMENT_PROVIDER -> {
                Telemetry.event(Telemetry.Event.CheckoutDeniedByPaymentProvider)
                handlePaymentAborted()
            }
            CheckoutState.DENIED_BY_SUPERVISOR -> {
                Telemetry.event(Telemetry.Event.CheckoutDeniedBySupervisor)
                handlePaymentAborted()
            }
            CheckoutState.PAYMENT_ABORTED -> {
                Telemetry.event(Telemetry.Event.CheckoutAbortByUser)
                handlePaymentAborted()
            }
            CheckoutState.REQUEST_PAYMENT_AUTHORIZATION_TOKEN -> {
                val price = checkout.verifiedOnlinePrice
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

        statusContainer.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
    }

    private fun checkForExitToken() {
        checkout.checkoutProcess?.exitToken?.let {
            if (it.value != null && it.format != null) {
                val format = BarcodeFormat.parse(it.format)
                if (format != null) {
                    SnabbleUI.executeAction(
                        requireFragmentActivity(),
                        SnabbleUI.Event.EXIT_TOKEN_AVAILABLE,
                        Bundle().apply {
                            putString("token", it.value)
                            putString("format", it.format)
                        }
                    )

                    exitToken.isVisible = true
                    exitTokenContainer.isVisible = true
                    exitToken.state = PaymentStatusItemView.State.SUCCESS
                    exitToken.setTitle(resources.getString(R.string.Snabble_PaymentStatus_ExitCode_title))
                    exitTokenBarcode.setFormat(format)
                    exitTokenBarcode.setText(it.value)
                } else {
                    exitToken.isVisible = false
                    exitTokenBarcode.isVisible = false
                }
            }
        }
    }

    private fun handlePaymentAborted() {
        payment.state = PaymentStatusItemView.State.FAILED
        payment.setText(resources.getString(R.string.Snabble_PaymentStatus_Payment_error))
        payment.setAction(resources.getString(R.string.Snabble_PaymentStatus_Payment_tryAgain)) {
            project.shoppingCart.generateNewUUID()
            requireFragmentActivity().finish()
        }
        title.text = resources.getString(R.string.Snabble_PaymentStatus_Title_error)
        image.isVisible = true
        image.setImageResource(R.drawable.snabble_ic_payment_error_big)
        progress.isVisible = false
        receipt.state = PaymentStatusItemView.State.NOT_EXECUTED
        back.isEnabled = true
        backPressedCallback.isEnabled = false
    }

    private fun startPollingForPaymentOriginCandidate() {
        if (hasShownSEPAInput) {
            return
        }

        paymentOriginCandidateHelper.addPaymentOriginCandidateAvailableListener(this)
        if (checkout.state.value == CheckoutState.PAYMENT_APPROVED) {
            paymentOriginCandidateHelper.startPollingIfLinkIsAvailable(checkout.checkoutProcess)
        }
    }

    private fun stopPollingForPaymentOriginCandidate() {
        paymentOriginCandidateHelper.removePaymentOriginCandidateAvailableListener(this)
        paymentOriginCandidateHelper.stopPolling()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        startPollingForPaymentOriginCandidate()

        onStateChanged(checkout.state.value)
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

        if (!isInEditMode) {
            startPollingForPaymentOriginCandidate()
        }
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

        val receipts = Snabble.receipts
        receipts.getReceiptInfo(object : Receipts.ReceiptInfoCallback {
            override fun success(receiptInfos: Array<ReceiptInfo>?) {
                val receiptInfo = receiptInfos?.firstOrNull { it.id == orderId }
                if (receiptInfo != null) {
                    Dispatch.mainThread {
                        receipt.state = PaymentStatusItemView.State.SUCCESS
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

    private fun onFulfillmentStateChanged(fulfillments: List<Fulfillment>?) {
        fulfillments?.forEach {
            var itemView = fulfillmentContainer.findViewWithTag<PaymentStatusItemView>(it.type)
            if (itemView == null) {
                itemView = PaymentStatusItemView(context)
                itemView.tag = it.type
                fulfillmentContainer.addView(
                    itemView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val state = it.state
            if (it.type == "tobaccolandEWA") {
                if (state == null || state.isFailure) {
                    if (it.state == FulfillmentState.ALLOCATION_FAILED
                        || it.state == FulfillmentState.ALLOCATION_TIMED_OUT) {
                        handlePaymentAborted()
                    }

                    itemView.setText(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_error))
                    itemView.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                    itemView.state = PaymentStatusItemView.State.FAILED
                } else if (state.isOpen) {
                    itemView.setText(null)
                    itemView.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                    itemView.state = PaymentStatusItemView.State.IN_PROGRESS
                } else if (state.isClosed) {
                    itemView.setText(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_message))
                    itemView.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Tobacco_title))
                    itemView.state = PaymentStatusItemView.State.SUCCESS
                }
            } else {
                itemView.setTitle(resources.getString(R.string.Snabble_PaymentStatus_Fulfillment_title))

                if (state == null || state.isFailure) {
                    itemView.state = PaymentStatusItemView.State.FAILED
                } else if (state.isOpen) {
                    itemView.state = PaymentStatusItemView.State.IN_PROGRESS
                } else if (state.isClosed) {
                    itemView.state = PaymentStatusItemView.State.SUCCESS
                }
            }
        }
    }

    override fun onPaymentOriginCandidateAvailable(paymentOriginCandidate: PaymentOriginCandidate?) {
        if (paymentOriginCandidate != null) {
            addIbanLayout.isVisible = true
            this.paymentOriginCandidate = paymentOriginCandidate
        }
    }
}