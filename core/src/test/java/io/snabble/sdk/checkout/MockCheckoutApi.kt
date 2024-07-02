package io.snabble.sdk.checkout

import com.google.gson.JsonObject
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.merge
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.shoppingcart.data.cart.BackendCart
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

    }

    override fun abort(
        checkoutProcessResponse: CheckoutProcessResponse,
        paymentAbortResult: PaymentAbortResult?
    ) {

    }

    override fun createCheckoutInfo(
        backendCart: BackendCart,
        checkoutInfoResult: CheckoutInfoResult?,
        timeout: Long
    ) {
        if (forceError) {
            checkoutInfoResult?.onConnectionError()
            return
        }

        val signedCheckoutInfo = SignedCheckoutInfo(
            checkoutInfo = GsonHolder.get().fromJson("", JsonObject::class.java)
        )

        checkoutInfoResult?.onSuccess(
            signedCheckoutInfo = signedCheckoutInfo,
            onlinePrice = project.shoppingCart.totalPrice,
            availablePaymentMethods = listOf(PaymentMethodInfo(
                id = "deDirectDebit",
                acceptedOriginTypes = listOf("iban"),
            )
        ))
    }

    override fun updatePaymentProcess(
        checkoutProcessResponse: CheckoutProcessResponse,
        paymentProcessResult: PaymentProcessResult?
    ) {
        if (forceError) {
            paymentProcessResult?.onError()
            return
        }

        paymentProcessResult?.onSuccess(mockResponse, GsonHolder.get().toJson(mockResponse))
    }

    override fun createPaymentProcess(
        id: String,
        signedCheckoutInfo: SignedCheckoutInfo,
        paymentMethod: PaymentMethod,
        processedOffline: Boolean,
        paymentCredentials: PaymentCredentials?,
        finalizedAt: Date?,
        paymentProcessResult: PaymentProcessResult?
    ) {
        if (forceError) {
            paymentProcessResult?.onError()
            return
        }

        paymentProcessResult?.onSuccess(mockResponse, GsonHolder.get().toJson(mockResponse))
    }

    override fun authorizePayment(
        checkoutProcessResponse: CheckoutProcessResponse,
        authorizePaymentRequest: AuthorizePaymentRequest,
        authorizePaymentResult: AuthorizePaymentResult?
    ) {

    }

    fun modifyMockResponse(merger: CheckoutProcessResponse) {
        mockResponse = mockResponse?.merge(merger)
    }
}
