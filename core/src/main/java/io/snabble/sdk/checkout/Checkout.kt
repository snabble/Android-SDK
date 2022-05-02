package io.snabble.sdk.checkout

import androidx.annotation.VisibleForTesting
import io.snabble.sdk.Snabble.instance
import androidx.lifecycle.MutableLiveData
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.utils.Age
import io.snabble.sdk.utils.Dispatch
import androidx.lifecycle.LiveData
import io.snabble.sdk.*
import io.snabble.sdk.utils.Logger
import java.lang.Exception
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Future

class Checkout @JvmOverloads constructor(
    private val project: Project,
    private val shoppingCart: ShoppingCart,
    private val checkoutApi: CheckoutApi = DefaultCheckoutApi(
        project, shoppingCart
    )
) {

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
    private val checkoutStateListeners: MutableList<OnCheckoutStateChangedListener> =
        CopyOnWriteArrayList()
    private val fulfillmentUpdateListeners: MutableList<OnFulfillmentUpdateListener> =
        CopyOnWriteArrayList()

    val checkoutState = MutableLiveData(CheckoutState.NONE)
    val fulfillmentState = MutableLiveData<List<Fulfillment>?>()

    private var lastState = CheckoutState.NONE

    /**
     * Gets the current state of the Checkout.
     *
     *
     * See [State].
     *
     * @return the state
     */
    var state = CheckoutState.NONE
        private set
    private val codes: MutableList<String> = ArrayList()
    var clientAcceptedPaymentMethods: List<PaymentMethod>? = null
    private var shop: Shop? = null
    var invalidProducts: List<Product>? = null
        private set
    private val checkoutRetryer: CheckoutRetryer
    private var currentPollFuture: Future<*>? = null
    private var storedAuthorizePaymentRequest: AuthorizePaymentRequest? = null
    private var authorizePaymentRequestFailed = false
    private var redeemedCoupons: List<Coupon>? = null
    fun abortError() {
        abort(true)
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled
     */
    @JvmOverloads
    fun abort(error: Boolean = false) {
        if (state != CheckoutState.PAYMENT_APPROVED && state != CheckoutState.DENIED_BY_PAYMENT_PROVIDER && state != CheckoutState.DENIED_BY_SUPERVISOR && checkoutProcess != null) {
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
                        shop = null
                    }
                }

                override fun error() {
                    if (error) {
                        notifyStateChanged(CheckoutState.PAYMENT_PROCESSING_ERROR)
                    } else {
                        if (state != CheckoutState.PAYMENT_PROCESSING && state != CheckoutState.PAYMENT_APPROVED) {
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
     * was cancelled, but does not notify listeners
     */
    fun abortSilently() {
        if (state != CheckoutState.PAYMENT_APPROVED && state != CheckoutState.DENIED_BY_PAYMENT_PROVIDER && state != CheckoutState.DENIED_BY_SUPERVISOR && checkoutProcess != null) {
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
        shop = null
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
     *
     * Requires a shop to be set with [Snabble.setCheckedInShop].
     *
     *
     * If successful and there is more then 1 payment method
     * the checkout state will be [State.REQUEST_PAYMENT_METHOD].
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
        shop = instance.checkedInShop
        redeemedCoupons = null
        fulfillmentState.value = null
        notifyStateChanged(CheckoutState.HANDSHAKING)
        if (shop == null) {
            notifyStateChanged(CheckoutState.NO_SHOP)
            return
        }
        val backendCart = shoppingCart.toBackendCart()
        checkoutApi.createCheckoutInfo(backendCart,
            clientAcceptedPaymentMethods,
            object : CheckoutInfoResult {
                override fun success(
                    checkoutInfo: SignedCheckoutInfo,
                    onlinePrice: Int,
                    availablePaymentMethods: List<PaymentMethodInfo>
                ) {
                    signedCheckoutInfo = checkoutInfo
                    if (signedCheckoutInfo!!.isRequiringTaxation) {
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
                        selectedPaymentMethod = fallback
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
     * Needs to be called when the checkout is state [State.REQUEST_PAYMENT_METHOD].
     *
     *
     * When successful the state will switch to [State.WAIT_FOR_APPROVAL] which
     * waits for a event of the backend that the payment has been confirmed or denied.
     *
     *
     * Possible states after receiving the event:
     *
     *
     * [State.PAYMENT_APPROVED]
     * [State.PAYMENT_ABORTED]
     * [State.DENIED_BY_PAYMENT_PROVIDER]
     * [State.DENIED_BY_SUPERVISOR]
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
        var allChecksOk = true
        for ((_, _, type, _, performedBy, state1) in checkoutProcessResponse.checks) {
            if (state1 !== CheckState.SUCCESSFUL) {
                return false
            }
            if (type == null) {
                continue
            }
            if (performedBy === Performer.APP) {
                if (type === CheckType.MIN_AGE) {
                    Logger.d("Verifying age...")
                    when (state1) {
                        CheckState.PENDING -> {
                            Logger.d("Age check pending...")
                            notifyStateChanged(CheckoutState.REQUEST_VERIFY_AGE)
                            abortSilently()
                            allChecksOk = false
                        }
                        CheckState.FAILED -> {
                            Logger.d("Age check failed...")
                            notifyStateChanged(CheckoutState.DENIED_TOO_YOUNG)
                            abortSilently()
                            allChecksOk = false
                        }
                        CheckState.SUCCESSFUL -> Logger.d("Age check successful")
                    }
                }
            }
        }
        return allChecksOk
    }

    private fun hasAnyCheckFailed(checkoutProcessResponse: CheckoutProcessResponse?): Boolean {
        for ((_, _, _, _, _, state1) in checkoutProcessResponse!!.checks) {
            if (state1 === CheckState.FAILED) {
                return true
            }
        }
        return false
    }

    private fun hasAnyFulfillmentAllocationFailed(): Boolean {
        for ((_, _, state1) in checkoutProcess!!.fulfillments) {
            if (state1 == FulfillmentState.ALLOCATION_FAILED ||
                state1 == FulfillmentState.ALLOCATION_TIMED_OUT
            ) {
                return true
            }
        }
        return false
    }

    private fun hasStillPendingChecks(checkoutProcessResponse: CheckoutProcessResponse): Boolean {
        for ((_, _, _, _, _, state1) in checkoutProcessResponse.checks) {
            if (state1 === CheckState.PENDING) {
                return true
            }
        }
        return false
    }

    private fun areAllFulfillmentsClosed(): Boolean {
        if (checkoutProcess == null) {
            return true
        }
        var ok = true
        for ((_, _, state1) in checkoutProcess!!.fulfillments) {
            if (state1 != null && state1.isOpen) {
                ok = false
                break
            }
        }
        return ok
    }

    private fun hasAnyFulfillmentFailed(): Boolean {
        if (checkoutProcess == null) {
            return false
        }
        var fail = false
        for ((_, _, state1) in checkoutProcess!!.fulfillments) {
            if (state1 != null && state1.isFailure) {
                fail = true
                break
            }
        }
        return fail
    }

    private val userAge: Int
        private get() {
            val date = instance.userPreferences.birthday ?: return -1
            val age = Age.calculateAge(date)
            return age.years
        }

    private fun scheduleNextPoll() {
        currentPollFuture = Dispatch.background({ pollForResult() }, 2000)
    }

    private fun stopPolling() {
        Logger.d("Stop polling")
        if (currentPollFuture != null) {
            currentPollFuture!!.cancel(true)
        }
    }

    @VisibleForTesting
    fun pollForResult() {
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
        if (state == CheckoutState.WAIT_FOR_APPROVAL || state == CheckoutState.WAIT_FOR_GATEKEEPER || state == CheckoutState.WAIT_FOR_SUPERVISOR || state == CheckoutState.VERIFYING_PAYMENT_METHOD || state == CheckoutState.REQUEST_PAYMENT_AUTHORIZATION_TOKEN || state == CheckoutState.PAYMENT_PROCESSING || state == CheckoutState.PAYMENT_APPROVED && !areAllFulfillmentsClosed()) {
            scheduleNextPoll()
        }
    }

    private fun handleProcessResponse(): Boolean {
        if (checkoutProcess!!.aborted) {
            Logger.d("Payment aborted")
            if (hasAnyFulfillmentFailed()) {
                notifyStateChanged(CheckoutState.PAYMENT_PROCESSING_ERROR)
            } else {
                notifyStateChanged(CheckoutState.PAYMENT_ABORTED)
            }
            return true
        }
        if (state == CheckoutState.VERIFYING_PAYMENT_METHOD) {
            if (checkoutProcess!!.routingTarget === RoutingTarget.SUPERVISOR) {
                notifyStateChanged(CheckoutState.WAIT_FOR_SUPERVISOR)
            } else if (checkoutProcess!!.routingTarget === RoutingTarget.GATEKEEPER) {
                notifyStateChanged(CheckoutState.WAIT_FOR_GATEKEEPER)
            } else {
                notifyStateChanged(CheckoutState.WAIT_FOR_APPROVAL)
            }
            return false
        }
        val authorizePaymentUrl = checkoutProcess!!.authorizePaymentLink
        if (authorizePaymentUrl != null) {
            if (authorizePaymentRequestFailed) {
                authorizePaymentRequestFailed = false
                authorizePayment(storedAuthorizePaymentRequest!!.encryptedOrigin)
            } else {
                if (storedAuthorizePaymentRequest != null) {
                    authorizePayment(storedAuthorizePaymentRequest!!.encryptedOrigin)
                } else {
                    notifyStateChanged(CheckoutState.REQUEST_PAYMENT_AUTHORIZATION_TOKEN)
                }
            }
            return false
        }
        if (checkoutProcess!!.paymentState === CheckState.SUCCESSFUL) {
            return if (checkoutProcess!!.exitToken != null
                && (checkoutProcess!!.exitToken!!.format == null || checkoutProcess!!.exitToken!!.format == "" || checkoutProcess!!.exitToken!!.value == null || checkoutProcess!!.exitToken!!.value == "")
            ) {
                false
            } else {
                approve()
                if (areAllFulfillmentsClosed()) {
                    notifyFulfillmentDone()
                    true
                } else {
                    notifyFulfillmentUpdate()
                    false
                }
            }
        } else if (checkoutProcess!!.paymentState === CheckState.PENDING
            || checkoutProcess!!.paymentState === CheckState.UNAUTHORIZED
        ) {
            if (hasAnyFulfillmentFailed()) {
                checkoutApi.abort(checkoutProcess, null)
                notifyStateChanged(CheckoutState.PAYMENT_PROCESSING)
                notifyFulfillmentDone()
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
        } else if (checkoutProcess!!.paymentState === CheckState.PROCESSING) {
            notifyStateChanged(CheckoutState.PAYMENT_PROCESSING)
        } else if (checkoutProcess!!.paymentState === CheckState.FAILED) {
            if (checkoutProcess!!.paymentResult != null && checkoutProcess!!.paymentResult!!.failureCause != null && checkoutProcess!!.paymentResult!!.failureCause == "terminalAbort") {
                Logger.d("Payment aborted by terminal")
                notifyStateChanged(CheckoutState.PAYMENT_ABORTED)
            } else {
                Logger.d("Payment denied by payment provider")
                notifyStateChanged(CheckoutState.DENIED_BY_PAYMENT_PROVIDER)
            }
            shoppingCart.generateNewUUID()
            return true
        }
        return false
    }

    private fun approve() {
        if (state != CheckoutState.PAYMENT_APPROVED) {
            Logger.d("Payment approved")
            if (selectedPaymentMethod != null && selectedPaymentMethod!!.isOfflineMethod) {
                shoppingCart.backup()
            }
            if (signedCheckoutInfo != null) {
                val coupons = project.coupons.get()
                redeemedCoupons = if (coupons != null) {
                    signedCheckoutInfo!!.getRedeemedCoupons(coupons)
                } else {
                    null
                }
            }
            shoppingCart.invalidate()
            clearCodes()
            notifyStateChanged(CheckoutState.PAYMENT_APPROVED)
            instance.users.update()
        }
    }

    fun getRedeemedCoupons(): List<Coupon> {
        return redeemedCoupons ?: ArrayList()
    }

    fun approveOfflineMethod() {
        if (selectedPaymentMethod != null && selectedPaymentMethod!!.isOfflineMethod
            || selectedPaymentMethod == PaymentMethod.CUSTOMERCARD_POS
        ) {
            shoppingCart.generateNewUUID()
            approve()
        }
    }

    val orderId: String?
        get() = if (checkoutProcess != null) checkoutProcess!!.orderId else null

    fun addCode(code: String) {
        codes.add(code)
    }

    fun removeCode(code: String) {
        codes.remove(code)
    }

    fun getCodes(): Collection<String> {
        return Collections.unmodifiableCollection(codes)
    }

    fun clearCodes() {
        codes.clear()
    }

    val paymentMethodForPayment: PaymentMethod?
        get() = if (checkoutProcess != null) {
            checkoutProcess!!.paymentMethod
        } else null
    val verifiedOnlinePrice: Int
        get() {
            try {
                if (checkoutProcess != null) {
                    return checkoutProcess!!.pricing!!.price!!.price
                }
            } catch (e: Exception) {
                return -1
            }
            return -1
        }
    val routingTarget: RoutingTarget?
        get() = if (checkoutProcess != null && checkoutProcess!!.routingTarget != null) {
            checkoutProcess!!.routingTarget
        } else RoutingTarget.NONE

    /**
     * Gets all available payment methods, callable after [State.REQUEST_PAYMENT_METHOD].
     */
    val availablePaymentMethods: Array<PaymentMethod?>
        get() {
            if (signedCheckoutInfo != null) {
                val paymentMethodInfos =
                    signedCheckoutInfo!!.getAvailablePaymentMethods(clientAcceptedPaymentMethods)
                val paymentMethods: MutableList<PaymentMethod> = ArrayList()
                for (info in paymentMethodInfos) {
                    val pm = PaymentMethod.fromString(
                        info!!.id
                    )
                    if (pm != null) {
                        paymentMethods.add(pm)
                    }
                }
                return paymentMethods.toTypedArray()
            }
            return arrayOfNulls(0)
        }

    /**
     * Gets the unique identifier of the checkout.
     *
     *
     * This id can be used for identification in the supervisor app.
     */
    val id: String?
        get() {
            if (checkoutProcess != null) {
                val selfLink = checkoutProcess!!.selfLink ?: return null
                return selfLink.substring(selfLink.lastIndexOf('/') + 1)
            }
            return null
        }

    /**
     * Gets the content of the qrcode that needs to be displayed,
     * or null if no qrcode needs to be displayed.
     */
    val qRCodePOSContent: String?
        get() {
            if (checkoutProcess != null) {
                if (checkoutProcess!!.paymentInformation != null) {
                    return checkoutProcess!!.paymentInformation!!.qrCodeContent
                }
            }
            return null
        }

    fun processPendingCheckouts() {
        checkoutRetryer.processPendingCheckouts()
    }

    interface OnCheckoutStateChangedListener {
        fun onStateChanged(state: CheckoutState)
    }

    fun getCheckoutState(): LiveData<CheckoutState> {
        return checkoutState
    }

    fun getFulfillmentState(): LiveData<List<Fulfillment>?> {
        return fulfillmentState
    }

    fun addOnCheckoutStateChangedListener(listener: OnCheckoutStateChangedListener) {
        if (!checkoutStateListeners.contains(listener)) {
            checkoutStateListeners.add(listener)
        }
    }

    fun removeOnCheckoutStateChangedListener(listener: OnCheckoutStateChangedListener) {
        checkoutStateListeners.remove(listener)
    }

    private fun notifyStateChanged(state: CheckoutState, repeat: Boolean = false) {
        synchronized(this@Checkout) {
            if (this.state != state || repeat) {
                lastState = this.state
                this.state = state
                Dispatch.mainThread {
                    checkoutState.value = state
                    for (checkoutStateListener in checkoutStateListeners) {
                        checkoutStateListener.onStateChanged(state)
                    }
                }
            }
        }
    }

    interface OnFulfillmentUpdateListener {
        fun onFulfillmentUpdated()
        fun onFulfillmentDone()
    }

    fun addOnFulfillmentListener(listener: OnFulfillmentUpdateListener) {
        if (!fulfillmentUpdateListeners.contains(listener)) {
            fulfillmentUpdateListeners.add(listener)
        }
    }

    fun removeOnFulfillmentListener(listener: OnFulfillmentUpdateListener) {
        fulfillmentUpdateListeners.remove(listener)
    }

    private fun notifyFulfillmentUpdate() {
        Dispatch.mainThread {
            for (checkoutStateListener in fulfillmentUpdateListeners) {
                checkoutStateListener.onFulfillmentUpdated()
            }
            if (checkoutProcess != null) {
                fulfillmentState.setValue(checkoutProcess!!.fulfillments)
            } else {
                fulfillmentState.setValue(null)
            }
        }
    }

    private fun notifyFulfillmentDone() {
        Dispatch.mainThread {
            for (checkoutStateListener in fulfillmentUpdateListeners) {
                checkoutStateListener.onFulfillmentDone()
            }
            if (checkoutProcess != null) {
                fulfillmentState.setValue(checkoutProcess!!.fulfillments)
            } else {
                fulfillmentState.setValue(null)
            }
        }
    }

    companion object {
        const val INVALID_PRICE = -1
    }

    init {
        checkoutRetryer = CheckoutRetryer(project, fallbackPaymentMethod)
    }
}