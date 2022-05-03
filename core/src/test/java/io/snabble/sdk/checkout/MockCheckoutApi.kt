package io.snabble.sdk.checkout

import com.google.gson.JsonObject
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.merge
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.utils.GsonHolder
import java.util.*

class MockCheckoutApi(
    val project: Project
) : CheckoutApi {
    var mockResponse: CheckoutProcessResponse? = CheckoutProcessResponse(
        paymentState = CheckState.SUCCESSFUL,
        routingTarget = RoutingTarget.NONE
    )
    var forceError = false

    override fun cancel() {
        TODO("Not yet implemented")
    }

    override fun abort(
        checkoutProcessResponse: CheckoutProcessResponse?,
        paymentAbortResult: PaymentAbortResult?
    ) {
        TODO("Not yet implemented")
    }

    override fun createCheckoutInfo(
        backendCart: ShoppingCart.BackendCart,
        checkoutInfoResult: CheckoutInfoResult?,
        timeout: Long
    ) {
        if (forceError) {
            checkoutInfoResult?.connectionError()
            return
        }

        val signedCheckoutInfo = SignedCheckoutInfo(
            checkoutInfo = GsonHolder.get().fromJson("", JsonObject::class.java)
        )

        checkoutInfoResult?.success(
            signedCheckoutInfo = signedCheckoutInfo,
            onlinePrice = project.shoppingCart.totalPrice,
            availablePaymentMethods = listOf(PaymentMethodInfo(
                id = "deDirectDebit",
                acceptedOriginTypes = listOf("iban"),
            )
        ))
    }

    override fun updatePaymentProcess(url: String?, paymentProcessResult: PaymentProcessResult?) {
        if (forceError) {
            paymentProcessResult?.error()
            return
        }

        paymentProcessResult?.success(mockResponse, GsonHolder.get().toJson(mockResponse))
    }

    override fun updatePaymentProcess(
        checkoutProcessResponse: CheckoutProcessResponse?,
        paymentProcessResult: PaymentProcessResult?
    ) {
        if (forceError) {
            paymentProcessResult?.error()
            return
        }

        paymentProcessResult?.success(mockResponse, GsonHolder.get().toJson(mockResponse))
    }

    override fun createPaymentProcess(
        id: String?,
        signedCheckoutInfo: SignedCheckoutInfo?,
        paymentMethod: PaymentMethod?,
        paymentCredentials: PaymentCredentials?,
        processedOffline: Boolean,
        finalizedAt: Date?,
        paymentProcessResult: PaymentProcessResult?
    ) {
        if (forceError) {
            paymentProcessResult?.error()
            return
        }

        paymentProcessResult?.success(mockResponse, GsonHolder.get().toJson(mockResponse))
    }

    override fun authorizePayment(
        checkoutProcessResponse: CheckoutProcessResponse?,
        authorizePaymentRequest: AuthorizePaymentRequest?,
        authorizePaymentResult: AuthorizePaymentResult?
    ) {
        TODO("Not yet implemented")
    }

    fun modifyMockResponse(merger: CheckoutProcessResponse) {
        mockResponse = mockResponse?.merge(merger)
    }
}