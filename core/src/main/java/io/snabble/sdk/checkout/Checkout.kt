package io.snabble.sdk.checkout

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.utils.Dispatch
import androidx.lifecycle.LiveData
import io.snabble.sdk.*
import io.snabble.sdk.utils.Logger
import java.lang.Exception
import java.util.*
import java.util.concurrent.Future

class Checkout @JvmOverloads constructor(
    private val project: Project,
    private val shoppingCart: ShoppingCart,
    private val checkoutApi: CheckoutApi = DefaultCheckoutApi(
        project, shoppingCart
    )
) {
    companion object {
        const val INVALID_PRICE = -1
    }

    private var signedCheckoutInfo: SignedCheckoutInfo? = null

    var checkoutProcess: CheckoutProcessResponse? = null
        private set
    var rawCheckoutProcessJson: String? = null
        private set

    /**
     * Gets the currently selected payment method, or null if currently none is selected.
     */
    var selectedPaymentMethod: PaymentMethod? = null
        private set
    var priceToPay = 0
        private set

    val state: LiveData<CheckoutState> = MutableAccessibleLiveData(CheckoutState.NONE)
    val fulfillmentState: LiveData<List<Fulfillment>?> = MutableAccessibleLiveData()

    /** List of codes that can be added to offline qr codes **/
    val codes: List<String> = mutableListOf()

    var invalidProducts: List<Product>? = null
        private set

    private val checkoutRetryer: CheckoutRetryer
    private var currentPollFuture: Future<*>? = null
    private var storedAuthorizePaymentRequest: AuthorizePaymentRequest? = null
    private var authorizePaymentRequestFailed = false
    private var redeemedCoupons: List<Coupon> = emptyList()

    init {
        checkoutRetryer = CheckoutRetryer(project, fallbackPaymentMethod)
    }

    fun abortError() {
        abort(true)
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled
     */
    @JvmOverloads
    fun abort(error: Boolean = false) {
        if (state.value != CheckoutState.PAYMENT_APPROVED
            && state.value != CheckoutState.DENIED_BY_PAYMENT_PROVIDER
            && state.value != CheckoutState.DENIED_BY_SUPERVISOR && checkoutProcess != null) {
            checkoutApi.abort(checkoutProcess, object : PaymentAbortResult {
                override fun success() {
                    cancelOutstandingCalls()
                    synchronized(this@Checkout) {
                        if (error) {
                            notifyStateChanged(CheckoutState.PAYMENT_PROCESSING_ERROR)
                        } else {
                            notifyStateChanged(CheckoutState.PAYMENT_ABORTED)
                        }
                        stopPolling()
                        invalidProducts = null
                        selectedPaymentMethod = null
                        shoppingCart.generateNewUUID()
                    }
                }

                override fun error() {
                    if (error) {
                        notifyStateChanged(CheckoutState.PAYMENT_PROCESSING_ERROR)
                    } else {
                        if (state.value != CheckoutState.PAYMENT_PROCESSING && state.value != CheckoutState.PAYMENT_APPROVED) {
                            notifyStateChanged(CheckoutState.PAYMENT_ABORT_FAILED)
                        }
                    }
                }
            })
            project.shoppingCart.updatePrices(false)
        } else {
            notifyStateChanged(CheckoutState.NONE)
        }
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled, but does not notify listeners and ignores the backend response
     */
    fun abortSilently() {
        if (state.value != CheckoutState.PAYMENT_APPROVED
            && state.value != CheckoutState.DENIED_BY_PAYMENT_PROVIDER
            && state.value != CheckoutState.DENIED_BY_SUPERVISOR && checkoutProcess != null) {
            checkoutApi.abort(checkoutProcess, null)
        }
        reset()
    }

    fun cancelOutstandingCalls() {
        checkoutApi.cancel()
    }

    /**
     * Cancels outstanding http calls and sets the checkout to its initial state.
     *
     *
     * Does NOT notify the backend that the checkout was cancelled.
     */
    fun reset() {
        cancelOutstandingCalls()
        stopPolling()
        notifyStateChanged(CheckoutState.NONE)
        invalidProducts = null
        selectedPaymentMethod = null
        shoppingCart.generateNewUUID()
    }

    val isAvailable: Boolean
        get() = project.checkoutUrl != null && project.isCheckoutAvailable

    private val fallbackPaymentMethod: PaymentMethod?
        private get() {
            val paymentMethods = project.paymentMethodDescriptors
            for (descriptor in paymentMethods) {
                val pm = descriptor.paymentMethod
                if (pm.isOfflineMethod) {
                    return pm
                }
            }
            return null
        }

    /**
     * Starts the checkout process.
     *
     * Requires a shop to be set with [Snabble.setCheckedInShop].
     *
     * If successful and there is more then 1 payment method
     * the checkout state will be [CheckoutState.REQUEST_PAYMENT_METHOD].
     * You then need to sometime after call @link Checkout#pay(PaymentMethod)}
     * to pay with that payment method.
     */
    @JvmOverloads
    fun checkout(timeout: Long = -1, allowFallbackAfterTimeout: Boolean = false) {
        checkoutProcess = null
        rawCheckoutProcessJson = null
        signedCheckoutInfo = null
        selectedPaymentMethod = null
        priceToPay = 0
        invalidProducts = null
        storedAuthorizePaymentRequest = null

        redeemedCoupons = emptyList()
        (fulfillmentState as MutableAccessibleLiveData).value = null

        notifyStateChanged(CheckoutState.HANDSHAKING)

        val shop = instance.checkedInShop
        if (shop == null) {
            notifyStateChanged(CheckoutState.NO_SHOP)
            return
        }

        val backendCart = shoppingCart.toBackendCart()
        checkoutApi.createCheckoutInfo(backendCart,
            object : CheckoutInfoResult {
                override fun success(
                    signedCheckoutInfo: SignedCheckoutInfo,
                    onlinePrice: Int,
                    availablePaymentMethods: List<PaymentMethodInfo>
                ) {
                    this@Checkout.signedCheckoutInfo = signedCheckoutInfo
                    if (signedCheckoutInfo.isRequiringTaxation) {
                        Logger.d("Taxation requested")
                        notifyStateChanged(CheckoutState.REQUEST_TAXATION)
                        return
                    }

                    priceToPay = shoppingCart.totalPrice
                    if (availablePaymentMethods.size == 1) {
                        val paymentMethod = PaymentMethod.fromString(
                            availablePaymentMethods[0].id
                        )
                        if (paymentMethod != null && !paymentMethod.isRequiringCredentials) {
                            pay(paymentMethod, null)
                        } else {
                            notifyStateChanged(CheckoutState.REQUEST_PAYMENT_METHOD)
                            Logger.d("Payment method requested")
                        }
                    } else {
                        notifyStateChanged(CheckoutState.REQUEST_PAYMENT_METHOD)
                        Logger.d("Payment method requested")
                    }
                }

                override fun noShop() {
                    notifyStateChanged(CheckoutState.NO_SHOP)
                }

                override fun invalidProducts(products: List<Product>) {
                    invalidProducts = products
                    notifyStateChanged(CheckoutState.INVALID_PRODUCTS)
                }

                override fun noAvailablePaymentMethod() {
                    notifyStateChanged(CheckoutState.NO_PAYMENT_METHOD_AVAILABLE)
                }

                override fun invalidDepositReturnVoucher() {
                    notifyStateChanged(CheckoutState.CONNECTION_ERROR)
                }

                override fun unknownError() {
                    notifyStateChanged(CheckoutState.CONNECTION_ERROR)
                }

                override fun connectionError() {
                    val fallback = fallbackPaymentMethod
                    if (fallback != null && allowFallbackAfterTimeout) {
                        selectedPaymentMethod = fallbackPaymentMethod
                        priceToPay = shoppingCart.totalPrice
                        checkoutRetryer.add(backendCart)
                        notifyStateChanged(CheckoutState.WAIT_FOR_APPROVAL)
                    } else {
                        notifyStateChanged(CheckoutState.CONNECTION_ERROR)
                    }
                }
            }, timeout
        )
    }

    /**
     * Needs to be called when the checkout is state [CheckoutState.REQUEST_PAYMENT_METHOD].
     *
     *
     * When successful the state will switch to [CheckoutState.WAIT_FOR_APPROVAL] which
     * waits for a event of the backend that the payment has been confirmed or denied.
     *
     *
     * Possible states after receiving the event:
     *
     * [CheckoutState.PAYMENT_APPROVED]
     * [CheckoutState.PAYMENT_ABORTED]
     * [CheckoutState.DENIED_BY_PAYMENT_PROVIDER]
     * [CheckoutState.DENIED_BY_SUPERVISOR]
     *
     * @param paymentMethod the payment method to pay with
     * @param paymentCredentials may be null if the payment method requires no payment credentials
     */
    fun pay(paymentMethod: PaymentMethod, paymentCredentials: PaymentCredentials?) {
        if (signedCheckoutInfo != null) {
            selectedPaymentMethod = paymentMethod
            notifyStateChanged(CheckoutState.VERIFYING_PAYMENT_METHOD)
            checkoutApi.createPaymentProcess(
                shoppingCart.uuid, signedCheckoutInfo,
                paymentMethod, paymentCredentials, false, null, object : PaymentProcessResult {
                    override fun success(
                        checkoutProcessResponse: CheckoutProcessResponse?,
                        rawResponse: String?
                    ) {
                        synchronized(this@Checkout) {
                            checkoutProcess = checkoutProcessResponse
                            rawCheckoutProcessJson = rawResponse
                            if (!handleProcessResponse()) {
                                if (!paymentMethod.isOfflineMethod) {
                                    scheduleNextPoll()
                                }
                            }
                        }
                    }

                    override fun error() {
                        Logger.e("Connection error while creating checkout process")
                        notifyStateChanged(CheckoutState.CONNECTION_ERROR)
                    }
                })
        } else {
            Logger.e("Invalid checkout state")
            notifyStateChanged(CheckoutState.CONNECTION_ERROR)
        }
    }

    fun authorizePayment(encryptedOrigin: String?) {
        val authorizePaymentRequest = AuthorizePaymentRequest()
        authorizePaymentRequest.encryptedOrigin = encryptedOrigin
        storedAuthorizePaymentRequest = authorizePaymentRequest
        checkoutApi.authorizePayment(checkoutProcess,
            authorizePaymentRequest,
            object : AuthorizePaymentResult {
                override fun success() {
                    // ignore
                }

                override fun error() {
                    authorizePaymentRequestFailed = true
                }
            })
    }

    private fun areAllChecksSucceeded(checkoutProcessResponse: CheckoutProcessResponse): Boolean {
        return checkoutProcessResponse.checks.all { it.state == CheckState.SUCCESSFUL }
    }

    private fun hasAnyCheckFailed(checkoutProcessResponse: CheckoutProcessResponse): Boolean {
        return checkoutProcessResponse.checks.any { it.state == CheckState.FAILED }
    }

    private fun hasStillPendingChecks(checkoutProcessResponse: CheckoutProcessResponse): Boolean {
        return checkoutProcessResponse.checks.any { it.state == CheckState.PENDING }
    }

    private fun hasAnyFulfillmentAllocationFailed(): Boolean {
        return checkoutProcess?.fulfillments?.any {
            it.state == FulfillmentState.ALLOCATION_FAILED
         || it.state == FulfillmentState.ALLOCATION_TIMED_OUT
        } ?: false
    }

    private fun areAllFulfillmentsClosed(): Boolean {
        return checkoutProcess?.fulfillments?.any {
            it.state?.isOpen ?: false
        } ?: true
    }

    private fun hasAnyFulfillmentFailed(): Boolean {
        return checkoutProcess?.fulfillments?.any {
            it.state?.isFailure ?: false
        } ?: false
    }

    private fun scheduleNextPoll() {
        currentPollFuture = Dispatch.background({ poll() }, 2000)
    }

    private fun stopPolling() {
        Logger.d("Stop polling")
        currentPollFuture?.cancel(true)
    }

    @VisibleForTesting
    fun poll() {
        if (checkoutProcess == null) {
            notifyStateChanged(CheckoutState.PAYMENT_ABORTED)
            return
        }

        Logger.d("Polling for approval state...")
        Logger.d("RoutingTarget = $routingTarget")
        checkoutApi.updatePaymentProcess(checkoutProcess, object : PaymentProcessResult {
            override fun success(
                checkoutProcessResponse: CheckoutProcessResponse?,
                rawResponse: String?
            ) {
                synchronized(this@Checkout) {
                    checkoutProcess = checkoutProcessResponse
                    rawCheckoutProcessJson = rawResponse
                    if (handleProcessResponse()) {
                        stopPolling()
                    }
                }
            }

            override fun error() {}
        })

        val state = state.value
        if (state == CheckoutState.WAIT_FOR_APPROVAL
            || state == CheckoutState.WAIT_FOR_GATEKEEPER
            || state == CheckoutState.WAIT_FOR_SUPERVISOR
            || state == CheckoutState.VERIFYING_PAYMENT_METHOD
            || state == CheckoutState.REQUEST_PAYMENT_AUTHORIZATION_TOKEN
            || state == CheckoutState.PAYMENT_PROCESSING
            || state == CheckoutState.PAYMENT_APPROVED && !areAllFulfillmentsClosed()) {
            scheduleNextPoll()
        }
    }

    private fun handleProcessResponse(): Boolean {
        checkoutProcess?.let { checkoutProcess ->
            if (checkoutProcess.aborted) {
                Logger.d("Payment aborted")
                if (hasAnyFulfillmentFailed()) {
                    notifyStateChanged(CheckoutState.PAYMENT_PROCESSING_ERROR)
                } else {
                    notifyStateChanged(CheckoutState.PAYMENT_ABORTED)
                }
                return true
            }

            if (state.value == CheckoutState.VERIFYING_PAYMENT_METHOD) {
                when (checkoutProcess.routingTarget) {
                    RoutingTarget.SUPERVISOR -> {
                        notifyStateChanged(CheckoutState.WAIT_FOR_SUPERVISOR)
                    }
                    RoutingTarget.GATEKEEPER -> {
                        notifyStateChanged(CheckoutState.WAIT_FOR_GATEKEEPER)
                    }
                    else -> {
                        notifyStateChanged(CheckoutState.WAIT_FOR_APPROVAL)
                    }
                }
                return false
            }

            val authorizePaymentUrl = checkoutProcess.authorizePaymentLink
            if (authorizePaymentUrl != null) {
                if (authorizePaymentRequestFailed) {
                    authorizePaymentRequestFailed = false
                    authorizePayment(storedAuthorizePaymentRequest?.encryptedOrigin)
                } else {
                    val storedAuthorizePaymentRequest = storedAuthorizePaymentRequest
                    if (storedAuthorizePaymentRequest != null) {
                        authorizePayment(storedAuthorizePaymentRequest.encryptedOrigin)
                    } else {
                        notifyStateChanged(CheckoutState.REQUEST_PAYMENT_AUTHORIZATION_TOKEN)
                    }
                }
                return false
            }

            when (checkoutProcess.paymentState) {
                CheckState.UNAUTHORIZED,
                CheckState.PENDING -> {
                    if (hasAnyFulfillmentFailed()) {
                        checkoutApi.abort(checkoutProcess, null)
                        notifyStateChanged(CheckoutState.PAYMENT_PROCESSING)
                        notifyFulfillmentUpdate()
                        return false
                    }

                    if (hasAnyFulfillmentAllocationFailed()) {
                        notifyStateChanged(CheckoutState.PAYMENT_ABORTED)
                        return true
                    }
                    if (hasAnyCheckFailed(checkoutProcess)) {
                        Logger.d("Payment denied by supervisor")
                        shoppingCart.generateNewUUID()
                        notifyStateChanged(CheckoutState.DENIED_BY_SUPERVISOR)
                    }
                }
                CheckState.PROCESSING -> {
                    notifyStateChanged(CheckoutState.PAYMENT_PROCESSING)
                }
                CheckState.SUCCESSFUL -> {
                    val exitToken = checkoutProcess.exitToken
                    return if (exitToken != null && (exitToken.format.isNullOrEmpty() || exitToken.value.isNullOrEmpty())) {
                        false
                    } else {
                        approve()
                        notifyFulfillmentUpdate()
                        areAllFulfillmentsClosed()
                    }
                }
                CheckState.FAILED -> {
                    if (checkoutProcess.paymentResult?.failureCause != null
                        && checkoutProcess.paymentResult.failureCause == "terminalAbort") {
                        Logger.d("Payment aborted by terminal")
                        notifyStateChanged(CheckoutState.PAYMENT_ABORTED)
                    } else {
                        Logger.d("Payment denied by payment provider")
                        notifyStateChanged(CheckoutState.DENIED_BY_PAYMENT_PROVIDER)
                    }
                    shoppingCart.generateNewUUID()
                    return true
                }
                else -> {
                    return false
                }
            }
        }
        return false
    }

    private fun approve() {
        if (state.value != CheckoutState.PAYMENT_APPROVED) {
            Logger.d("Payment approved")

            if (selectedPaymentMethod?.isOfflineMethod == true) {
                shoppingCart.backup()
            }

            signedCheckoutInfo?.let { signedCheckoutInfo ->
                val coupons = project.coupons.get()
                redeemedCoupons = if (coupons != null) {
                    signedCheckoutInfo.getRedeemedCoupons(coupons)
                } else {
                    emptyList()
                }
            }

            shoppingCart.invalidate()
            clearCodes()
            notifyStateChanged(CheckoutState.PAYMENT_APPROVED)
            instance.users.update()
        }
    }

    /**
     * Approve offline processed payment methods. E.g. on clicking the "finish" button or leaving
     * the qr code view.
     *
     * This is for resetting the cart state and notifying the backend
     */
    fun approveOfflineMethod() {
        if (selectedPaymentMethod?.isOfflineMethod == true || selectedPaymentMethod == PaymentMethod.CUSTOMERCARD_POS) {
            shoppingCart.generateNewUUID()
            approve()
        }
    }

    /**
     * Add code to append to offline qr codes
     */
    fun addCode(code: String) {
        (codes as MutableList).add(code)
    }

    /**
     * Remove code to append to offline qr codes
     */
    fun removeCode(code: String) {
        (codes as MutableList).remove(code)
    }

    /**
     * Clear all codes that are appended to offline qr codes
     */
    fun clearCodes() {
        (codes as MutableList).clear()
    }

    private val routingTarget: RoutingTarget
        get() =  checkoutProcess?.routingTarget ?: RoutingTarget.NONE

    /**
     * The final price of the checkout, calculated by the backend.
     *
     * Returns a price after [CheckoutState.WAIT_FOR_APPROVAL], otherwise -1
     */
    val verifiedOnlinePrice: Int
        get() {
            return checkoutProcess?.pricing?.price?.price ?: -1
        }

    /**
     * Gets all available payment methods, callable after [CheckoutState.REQUEST_PAYMENT_METHOD].
     */
    val availablePaymentMethods: List<PaymentMethod>
        get() = signedCheckoutInfo?.getAvailablePaymentMethods()
            ?.map { PaymentMethod.fromString(it.id) }
            ?: emptyList()

    /**
     * Gets the unique identifier of the checkout.
     *
     * This id can be used for identification in the supervisor app.
     */
    val id: String?
        get() = checkoutProcess?.selfLink?.let {
            it.substring(it.lastIndexOf('/') + 1)
        }

    /**
     * Gets the content of the qrcode that needs to be displayed,
     * or null if no qrcode needs to be displayed.
     */
    val qrCodePOSContent: String?
        get() = checkoutProcess?.paymentInformation?.qrCodeContent

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun processPendingCheckouts() {
        checkoutRetryer.processPendingCheckouts()
    }

    private fun notifyStateChanged(state: CheckoutState) {
        with(this.state as MutableAccessibleLiveData) {
            value = state
        }
    }

    private fun notifyFulfillmentUpdate() {
        (fulfillmentState as MutableAccessibleLiveData).value = checkoutProcess?.fulfillments
    }

    //    val orderId: String?
    //        get() = if (checkoutProcess != null) checkoutProcess!!.orderId else null

    //    val paymentMethodForPayment: PaymentMethod?
    //        get() = if (checkoutProcess != null) {
    //            checkoutProcess!!.paymentMethod
    //        } else null
}