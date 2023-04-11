package io.snabble.sdk.ui.checkout

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import io.snabble.sdk.*
import io.snabble.sdk.checkout.Checkout
import io.snabble.sdk.checkout.CheckoutState
import io.snabble.sdk.checkout.Fulfillment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.checkout.PaymentOriginCandidateHelper.PaymentOriginCandidate
import io.snabble.sdk.ui.checkout.PaymentOriginCandidateHelper.PaymentOriginCandidateAvailableListener
import io.snabble.sdk.ui.payment.SEPACardInputActivity
import io.snabble.sdk.ui.payment.payone.sepa.form.PayoneSepaActivity
import io.snabble.sdk.ui.scanner.BarcodeView
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.executeUiAction
import io.snabble.sdk.ui.utils.getFragmentActivity
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.ui.utils.requireFragmentActivity
import io.snabble.sdk.utils.Dispatch

class PaymentStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr),
    DefaultLifecycleObserver, PaymentOriginCandidateAvailableListener {

    init {
        inflate(getContext(), R.layout.snabble_view_payment_status, this)
    }

    private val image = findViewById<ImageView>(R.id.image)
    private val back = findViewById<MaterialButton>(R.id.back)
    private var selectedRating = ""
    private val sendFeedback: View? = findViewById(R.id.send_feedback)
    private val successAnimation: LottieAnimationView = findViewById(R.id.success_animation)
    private val inputBadRatingLayout = findViewById<TextInputLayout>(R.id.input_bad_rating_layout)
    private var addIbanLayout = findViewById<LinearLayout>(R.id.add_iban_layout)
    private var addIbanButton = findViewById<Button>(R.id.add_iban_button)
    private var payment = findViewById<PaymentStatusItemView>(R.id.payment)
    private val ratingCardLayout = findViewById<View>(R.id.ratingCardLayout)
    private val ratingExtraFeedBackView: View? = findViewById(R.id.checkout_extra_feedback_view)
    private var ratingLayoutGroup = findViewById<View>(R.id.ratingLayoutGroup)
    private var badRatingLayoutGroup = findViewById<View>(R.id.badRatingLayoutGroup)
    private var ratingTitle = findViewById<TextView>(R.id.rating_title)
    private val ratingButtonNegative = findViewById<RadioButton>(R.id.ratingButtonNegative)
    private val ratingButtonNeutral = findViewById<RadioButton>(R.id.ratingButtonNeutral)
    private val ratingButtonPositive = findViewById<RadioButton>(R.id.ratingButtonPositive)
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
            // Do nothing
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

        back.text = resources.getString(R.string.Snabble_cancel)
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

        ratingButtonNegative.setOnClickListener {
            createMessageForRating("1")
        }

        ratingButtonNeutral.setOnClickListener {
            createMessageForRating("2")
        }

        ratingButtonPositive.setOnClickListener {
            sendRating("3")
        }

        sendFeedback?.setOnClickListener {
            sendRating(selectedRating)
            inputBadRatingLayout.isVisible = false
        }

        ratingExtraFeedBackView?.setOnClickListener {
            val url = it.tag.toString()
            if (URLUtil.isValidUrl(url)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        addIbanLayout.isVisible = false
        addIbanButton.setOnClickListener {
            context.startActivity(createAddIbanIntent())

            addIbanLayout.isVisible = false
            hasShownSEPAInput = true
        }

        val activity = getFragmentActivity()
        activity?.lifecycle?.addObserver(this)
        activity?.onBackPressedDispatcher?.addCallback(backPressedCallback)
    }

    private fun createAddIbanIntent() =
        if (project.paymentMethodDescriptors.any { it.paymentMethod == PaymentMethod.PAYONE_SEPA }) {
            PayoneSepaActivity.newIntent(context, paymentOriginCandidate)
        } else {
            SEPACardInputActivity.newIntent(context, paymentOriginCandidate)
        }

    private fun createMessageForRating(rating: String) {
        selectedRating = rating
        ratingMessage = ""
        badRatingLayoutGroup.isVisible = true
        sendFeedback?.isVisible = true
        ratingExtraFeedBackView?.isVisible = true
        inputBadRatingLayout.isVisible = true
        inputBadRatingLayout.editText?.requestFocusWithKeyboard()
        inputBadRatingLayout.editText?.addTextChangedListener { s ->
            ratingMessage = s.toString()
        }
    }

    private fun EditText.requestFocusWithKeyboard() {
        requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeInputKeyboard() {
        val keyboardManager: InputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        keyboardManager.hideSoftInputFromWindow(this.windowToken, 0)
    }

    private fun sendRating(rating: String) {
        project.events.analytics("rating", rating, ratingMessage ?: "")
        Telemetry.event(Telemetry.Event.Rating, rating)
        ratingTitle.setText(R.string.Snabble_PaymentStatus_Ratings_thanksForFeedback)
        ratingLayoutGroup.isVisible = false
        ratingMessage = null
        closeInputKeyboard()
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
            CheckoutState.VERIFYING_PAYMENT_METHOD,
            -> {
                payment.state = PaymentStatusItemView.State.IN_PROGRESS
            }

            CheckoutState.PAYMENT_APPROVED -> {
                title.text = resources.getString(R.string.Snabble_PaymentStatus_Title_success)
                image.setImageResource(R.drawable.snabble_ic_payment_success_big)
                image.isVisible = true
                successAnimation.playAnimation()
                progress.isVisible = false
                payment.state = PaymentStatusItemView.State.SUCCESS
                val checkoutProcess = checkout.checkoutProcess
                if (checkoutProcess?.orderId != null) {
                    startPollingForReceipts(checkout.checkoutProcess?.orderId)
                } else {
                    receipt.state = PaymentStatusItemView.State.NOT_EXECUTED
                }
                back.text = resources.getString(R.string.Snabble_PaymentStatus_close)
                backPressedCallback.isEnabled = false
                ratingCardLayout.isVisible = true
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

            else -> Unit
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

                    exitTokenContainer.isVisible = true
                    exitToken.state = PaymentStatusItemView.State.SUCCESS
                    exitToken.setTitle(resources.getString(R.string.Snabble_PaymentStatus_ExitCode_title))
                    exitTokenBarcode.setFormat(format)
                    exitTokenBarcode.setText(it.value)
                } else {
                    exitTokenContainer.isVisible = false
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

    override fun onStart(owner: LifecycleOwner) {
        startPollingForPaymentOriginCandidate()

        onStateChanged(checkout.state.value)
        isStopped = false
    }

    override fun onStop(owner: LifecycleOwner) {
        stopPollingForPaymentOriginCandidate()
        isStopped = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!isInEditMode) {
            startPollingForPaymentOriginCandidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

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
                        || it.state == FulfillmentState.ALLOCATION_TIMED_OUT
                    ) {
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
