package io.snabble.sdk.checkout

import com.google.gson.JsonObject
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.utils.GsonHolder
import java.util.*
import java.util.List
import kotlin.math.roundToInt

class MockCheckoutApi(
    val project: Project
) : CheckoutApi {

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
        clientAcceptedPaymentMethods: List<PaymentMethod>?,
        checkoutInfoResult: CheckoutInfoResult?,
        timeout: Long
    ) {
        val signedCheckoutInfo = SignedCheckoutInfo(
            checkoutInfo = GsonHolder.get().fromJson(
                """
                    {
                      "checkoutInfo": {
                        "project": "test-ieme8a",
                        "availableMethods": [
                          "deDirectDebit",
                        ],
                        "price": {
                          "subTotal": ${project.shoppingCart.totalPrice},
                          "price": ${project.shoppingCart.totalPrice},
                          "netPrice": ${(project.shoppingCart.totalPrice - (project.shoppingCart.totalPrice * 0.19)).roundToInt()},
                          "tax": {
                            "19": ${(project.shoppingCart.totalPrice * 0.19).roundToInt()}
                          },
                          "taxPre": {
                            "19": ${project.shoppingCart.totalPrice}
                          },
                          "taxNet": {
                            "19": ${(project.shoppingCart.totalPrice - (project.shoppingCart.totalPrice * 0.19)).roundToInt()}
                          }
                        },
                        "paymentMethods": [
                          {
                            "id": "deDirectDebit",
                            "acceptedOriginTypes": [
                              "iban"
                            ],
                            "providerName": "test"
                          },
                        ]
                      },
                      "links": {
                        "checkoutProcess": {
                          "href": "/test-ieme8a/checkout/process"
                        }
                      }
                    }
                """.trimIndent(),
                JsonObject::class.java)
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
        TODO("Not yet implemented")
    }

    override fun updatePaymentProcess(
        checkoutProcessResponse: CheckoutProcessResponse?,
        paymentProcessResult: PaymentProcessResult?
    ) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun authorizePayment(
        checkoutProcessResponse: CheckoutProcessResponse?,
        authorizePaymentRequest: AuthorizePaymentRequest?,
        authorizePaymentResult: AuthorizePaymentResult?
    ) {
        TODO("Not yet implemented")
    }

}