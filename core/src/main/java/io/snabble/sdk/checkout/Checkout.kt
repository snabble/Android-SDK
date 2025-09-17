package io.snabble.sdk.checkout

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import io.snabble.sdk.FulfillmentState
import io.snabble.sdk.MutableAccessibleLiveData
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Product
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger
import java.io.File
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

    private var persistentState: PersistentState =
        PersistentState.restore(
            file = File(project.internalStorageDirectory, "checkout.json"),
            cartId = shoppingCart.id,
            projectId = project.id
        )

    /** The current checkout process response from the backend **/
    var checkoutProcess
        get() = persistentState.checkoutProcess
        private set(value) {
            persistentState.checkoutProcess = value
            persistentState.save()
        }

    /**
     * ShopId associated with this Checkout
     */
    var shopId: String? = null

    /**
     * Gets the currently selected payment method, or null if currently none is selected
     */
    var selectedPaymentMethod
        get() = persistentState.selectedPaymentMethod
        private set(value) {
            persistentState.selectedPaymentMethod = value
            persistentState.save()
        }

    /**
     * The price to pay in cents (or equivalent integer based currency type),
     * or 0 if no price is available
     */
    var priceToPay
        get() = persistentState.priceToPay
        private set(value) {
            persistentState.priceToPay = value
            persistentState.save()
        }

    /** List of codes that can be added to offline qr codes **/
    val codes
        get() = persistentState.codes

    /**
     * List of invalid products that were contained in a checkout request, available after
     * entering state CheckoutState.INVALID_PRODUCTS
     */
    var invalidProducts
        get() = persistentState.invalidProducts
        private set(value) {
            persistentState.invalidProducts = value
            persistentState.save()
        }

    var invalidItems: List<ShoppingCart.Item>?
        get() = persistentState.invalidItems
        private set(value) {
            persistentState.invalidItems = value
            persistentState.save()
        }

    /**
     * List of coupons that were redeemed during this checkout
     */
    var redeemedCoupons
        get() = persistentState.redeemedCoupons
        private set(value) {
            persistentState.redeemedCoupons = value
            persistentState.save()
        }

    /**
     * The current state of the checkout as a live data
     */
    val state: LiveData<CheckoutState> = MutableAccessibleLiveData(CheckoutState.NONE)

    /**
     * The current fulfillment state as a live data
     */
    val fulfillmentState: LiveData<List<Fulfillment>?> = MutableAccessibleLiveData()

    private var signedCheckoutInfo
        get() = persistentState.signedCheckoutInfo
        set(value) {
            persistentState.signedCheckoutInfo = value
            persistentState.save()
        }

    private var checkoutRetryer: CheckoutRetryer? = null
    private var currentPollFuture: Future<*>? = null
    private var storedAuthorizePaymentRequest: AuthorizePaymentRequest? = null
    private var authorizePaymentRequestFailed = false

    init {
        (state as MutableAccessibleLiveData).value = persistentState.state
        (fulfillmentState as MutableAccessibleLiveData).value = persistentState.fulfillmentState
        fallbackPaymentMethod?.let {
            checkoutRetryer = CheckoutRetryer(project, it)
        }
        pollIfNeeded()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun abortError() {
        abortInternal(true)
    }

    private fun abortInternal(error: Boolean = false) {
        val checkoutProcess = checkoutProcess
        if (checkoutProcess != null
            && state.value != CheckoutState.PAYMENT_APPROVED
            && state.value != CheckoutState.DENIED_BY_PAYMENT_PROVIDER
            && state.value != CheckoutState.DENIED_BY_SUPERVISOR
        ) {
            if (hasAnyFulfillmentAllocationFailed()) {
                reset()
                return
            }

            checkoutApi.abort(checkoutProcess, object : PaymentAbortResult {
                override fun onSuccess() {
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

                override fun onError() {
                    if (error) {
                        notifyStateChanged(CheckoutState.PAYMENT_PROCESSING_ERROR)
                    } else {
                        if (state.value != CheckoutState.PAYMENT_PROCESSING
                            && state.value != CheckoutState.PAYMENT_APPROVED
                        ) {
                            val lastState = state.value

                            Dispatch.mainThread {
                                notifyStateChanged(CheckoutState.PAYMENT_ABORT_FAILED)
                                lastState?.let { notifyStateChanged(it) }
                            }
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
     * was cancelled
     */
    fun abort() {
        abortInternal(false)
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled, but does not notify listeners and ignores the backend response
     */
    fun abortSilently() {
        val checkoutProcess = checkoutProcess
        if (checkoutProcess != null
            && state.value != CheckoutState.PAYMENT_APPROVED
            && state.value != CheckoutState.DENIED_BY_PAYMENT_PROVIDER
            && state.value != CheckoutState.DENIED_BY_SUPERVISOR
        ) {
            checkoutApi.abort(checkoutProcess, null)
        }
        reset()
    }

    /**
     * Cancels outstanding http calls and sets the checkout to its initial state.
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

    private fun cancelOutstandingCalls() {
        checkoutApi.cancel()
    }

    /**
     * Returns true of the checkout is currently available, or false if checkout is disabled
     * for this project
     */
    val isAvailable: Boolean
        get() = project.checkoutUrl != null && project.isCheckoutAvailable

    private val fallbackPaymentMethod: PaymentMethod?
        get() = project.paymentMethodDescriptors
            .mapNotNull { it.paymentMethod }
            .firstOrNull { it.isOfflineMethod }

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
        signedCheckoutInfo = null
        selectedPaymentMethod = null
        priceToPay = 0
        invalidProducts = null
        storedAuthorizePaymentRequest = null

        redeemedCoupons = emptyList()
        (fulfillmentState as MutableAccessibleLiveData).value = null

        notifyStateChanged(CheckoutState.HANDSHAKING)

        shopId = instance.checkedInShop?.id
        if (shopId == null) {
            notifyStateChanged(CheckoutState.NO_SHOP)
            return
        }

        val backendCart = shoppingCart.toBackendCart()
        checkoutApi.createCheckoutInfo(
            backendCart,
            object : CheckoutInfoResult {
                override fun onSuccess(
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
                        val paymentMethod = availablePaymentMethods[0].id?.let { PaymentMethod.fromString(it) }
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

                override fun onNoShopFound() {
                    notifyStateChanged(CheckoutState.NO_SHOP)
                }

                override fun onInvalidProducts(products: List<Product>) {
                    invalidProducts = products
                    notifyStateChanged(CheckoutState.INVALID_PRODUCTS)
                }

                override fun onInvalidItems(itemIds: List<String>) {
                    invalidItems = shoppingCart.filterNotNull().filter { it.id in itemIds }
                    notifyStateChanged(CheckoutState.INVALID_ITEMS)
                }

                override fun onNoAvailablePaymentMethodFound() {
                    notifyStateChanged(CheckoutState.NO_PAYMENT_METHOD_AVAILABLE)
                }

                override fun onUnknownError() {
                    notifyStateChanged(CheckoutState.CONNECTION_ERROR)
                }

                override fun onConnectionError() {
                    val fallback = fallbackPaymentMethod
                    if (fallback != null && allowFallbackAfterTimeout) {
                        selectedPaymentMethod = fallbackPaymentMethod
                        priceToPay = shoppingCart.totalPrice
                        checkoutRetryer?.add(backendCart)
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
     * When successful the state will switch to [CheckoutState.WAIT_FOR_APPROVAL] which
     * waits for a event of the backend that the payment has been confirmed or denied.
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
        val signedCheckoutInfo = signedCheckoutInfo
        if (signedCheckoutInfo != null) {
            selectedPaymentMethod = paymentMethod
            notifyStateChanged(CheckoutState.VERIFYING_PAYMENT_METHOD)
            checkoutApi.createPaymentProcess(
                id = shoppingCart.uuid,
                signedCheckoutInfo = signedCheckoutInfo,
                paymentMethod = paymentMethod,
                processedOffline = false,
                paymentCredentials = paymentCredentials,
                finalizedAt = null,
                paymentProcessResult = object : PaymentProcessResult {
                    override fun onSuccess(
                        checkoutProcessResponse: CheckoutProcessResponse?,
                        rawResponse: String?
                    ) {
                        synchronized(this@Checkout) {
                            checkoutProcess = checkoutProcessResponse
                            if (!handleProcessResponse()) {
                                if (!paymentMethod.isOfflineMethod) {
                                    scheduleNextPoll()
                                }
                            }
                        }
                    }

                    override fun onError() {
                        Logger.e("Connection error while creating checkout process")
                        notifyStateChanged(CheckoutState.CONNECTION_ERROR)
                    }

                    override fun onNotFound() {
                        reset()
                    }
                })
        } else {
            Logger.e("Invalid checkout state")
            notifyStateChanged(CheckoutState.CONNECTION_ERROR)
        }
    }

    /**
     * Authorize a for one-time use payment requests, e.g. sending tokenized payment data of google pay.
     */
    fun authorizePayment(authPayRequest: AuthorizePaymentRequest?) {
        val authorizePaymentRequest = authPayRequest ?: AuthorizePaymentRequest()
        storedAuthorizePaymentRequest = authorizePaymentRequest
        checkoutProcess?.let { checkoutProcess ->
            checkoutApi.authorizePayment(checkoutProcess,
                authorizePaymentRequest,
                object : AuthorizePaymentResult {
                    override fun onSuccess() {
                        // ignore
                    }

                    override fun onError() {
                        authorizePaymentRequestFailed = true
                    }
                })
        }
    }

    private fun hasAnyCheckFailed(checkoutProcessResponse: CheckoutProcessResponse): Boolean {
        return checkoutProcessResponse.checks.any { it.state == CheckState.FAILED }
    }

    private fun hasAnyFulfillmentAllocationFailed(): Boolean {
        return checkoutProcess?.fulfillments?.any {
            it.state == FulfillmentState.ALLOCATION_FAILED
                || it.state == FulfillmentState.ALLOCATION_TIMED_OUT
        } ?: false
    }

    private fun areAllFulfillmentsClosed(): Boolean {
        return checkoutProcess?.fulfillments?.all { it.state?.isClosed ?: false } ?: true
    }

    private fun hasAnyFulfillmentFailed(): Boolean {
        return checkoutProcess?.fulfillments?.any { it.state?.isFailure ?: false } ?: false
    }

    private fun scheduleNextPoll() {
        currentPollFuture = Dispatch.background({ poll() }, 2000)
    }

    private fun stopPolling() {
        Logger.d("Stop polling")
        currentPollFuture?.cancel(true)
    }

    @VisibleForTesting
    internal fun poll() {
        if (checkoutProcess == null) {
            notifyStateChanged(CheckoutState.PAYMENT_ABORTED)
            return
        }
        val hasAnyFailedDrvRedemptions =
            checkoutProcess?.depositReturnVouchers
                ?.any { it.state == DepositReturnVoucherState.REDEEMING_FAILED }
                ?: false

        if (hasAnyFailedDrvRedemptions) {
            notifyStateChanged(CheckoutState.DEPOSIT_RETURN_REDEMPTION_FAILED)
            return
        }

        Logger.d("Polling for approval state...")
        Logger.d("RoutingTarget = $routingTarget")

        checkoutProcess?.let { process ->
            checkoutApi.updatePaymentProcess(process, object : PaymentProcessResult {
                override fun onSuccess(
                    checkoutProcessResponse: CheckoutProcessResponse?,
                    rawResponse: String?
                ) {
                    synchronized(this@Checkout) {
                        checkoutProcess = checkoutProcessResponse
                        if (handleProcessResponse()) {
                            stopPolling()
                        }
                    }
                }

                override fun onError() {}

                override fun onNotFound() {
                    reset()
                }
            })
        }

        pollIfNeeded()
    }

    private fun pollIfNeeded() {
        val state = state.value
        if (state == CheckoutState.WAIT_FOR_APPROVAL
            || state == CheckoutState.WAIT_FOR_GATEKEEPER
            || state == CheckoutState.WAIT_FOR_SUPERVISOR
            || state == CheckoutState.VERIFYING_PAYMENT_METHOD
            || state == CheckoutState.REQUEST_PAYMENT_AUTHORIZATION_TOKEN
            || state == CheckoutState.PAYMENT_PROCESSING
            || (state == CheckoutState.PAYMENT_APPROVED && !areAllFulfillmentsClosed())
            || state == CheckoutState.PAYONE_SEPA_MANDATE_REQUIRED
            || state == CheckoutState.AUTHENTICATING
        ) {
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

            if (state.value == CheckoutState.VERIFYING_PAYMENT_METHOD && checkoutProcess.paymentState != CheckState.TRANSFERRED) {
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

            if (isPayoneSepaMandateRequired(checkoutProcess)) {
                Logger.d("Waiting for PAYONE SEPA mandate")
                notifyStateChanged(CheckoutState.PAYONE_SEPA_MANDATE_REQUIRED)
                return false
            }

            val authorizePaymentUrl = checkoutProcess.authorizePaymentLink
            if (authorizePaymentUrl != null) {
                if (authorizePaymentRequestFailed) {
                    authorizePaymentRequestFailed = false
                    authorizePayment(storedAuthorizePaymentRequest)
                } else {
                    val storedAuthorizePaymentRequest = storedAuthorizePaymentRequest
                    if (storedAuthorizePaymentRequest != null) {
                        authorizePayment(storedAuthorizePaymentRequest)
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

                CheckState.AUTHENTICATING -> {
                    notifyStateChanged(CheckoutState.AUTHENTICATING)
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

                CheckState.TRANSFERRED -> {
                    notifyStateChanged(CheckoutState.PAYMENT_TRANSFERRED)
                }

                CheckState.FAILED -> {
                    if (checkoutProcess.paymentResult?.failureCause != null
                        && checkoutProcess.paymentResult.failureCause == "terminalAbort"
                    ) {
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

    private fun isPayoneSepaMandateRequired(checkoutProcess: CheckoutProcessResponse) =
        checkoutProcess.paymentState == CheckState.UNAUTHORIZED &&
                checkoutProcess.paymentMethod == PaymentMethod.PAYONE_SEPA &&
                checkoutProcess.routingTarget != RoutingTarget.SUPERVISOR

    private fun approve() {
        Logger.d("dddd approve checkout " + System.identityHashCode(this))

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
        persistentState.save()
    }

    /**
     * Remove code to append to offline qr codes
     */
    fun removeCode(code: String) {
        (codes as MutableList).remove(code)
        persistentState.save()
    }

    /**
     * Clear all codes that are appended to offline qr codes
     */
    fun clearCodes() {
        (codes as MutableList).clear()
        persistentState.save()
    }

    private val routingTarget: RoutingTarget
        get() = checkoutProcess?.routingTarget ?: RoutingTarget.NONE

    /**
     * The final price of the checkout, calculated by the backend.
     *
     * Returns a price after [CheckoutState.WAIT_FOR_APPROVAL], otherwise -1
     */
    val verifiedOnlinePrice: Int
        get() = checkoutProcess?.pricing?.price?.price ?: -1

    /**
     * Gets all available payment methods, callable after [CheckoutState.REQUEST_PAYMENT_METHOD].
     */
    val availablePaymentMethods: List<PaymentMethod>
        get() = signedCheckoutInfo?.getAvailablePaymentMethods()
            ?.map { it.id?.let { PaymentMethod.fromString(it) } }
            ?.filterNotNull()
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
     * or null if no qrcode needs to be displayed
     */
    val qrCodePOSContent: String?
        get() = checkoutProcess?.paymentInformation?.qrCodeContent

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun processPendingCheckouts() {
        checkoutRetryer?.processPendingCheckouts()
    }

    private fun notifyStateChanged(state: CheckoutState) {
        with(this.state as MutableAccessibleLiveData) {
            value = state
        }
        persistentState.state = state
        persistentState.save()
    }

    private fun notifyFulfillmentUpdate() {
        (fulfillmentState as MutableAccessibleLiveData).value = checkoutProcess?.fulfillments
        persistentState.fulfillmentState = checkoutProcess?.fulfillments
        persistentState.save()
    }
}
