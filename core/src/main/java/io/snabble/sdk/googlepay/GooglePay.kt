package io.snabble.sdk.googlepay

import android.app.Activity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.GsonHolder

class GooglePay(
    val useTestEnvironment: Boolean = false,
    private var googlePayClient: PaymentsClient
) {
    init {
        googlePayClient = createPaymentsClient(true)
    }

    private fun createPaymentsClient(useTestEnvironment: Boolean = false): PaymentsClient {
        val env = if (useTestEnvironment) {
            WalletConstants.ENVIRONMENT_TEST
        } else {
            WalletConstants.ENVIRONMENT_PRODUCTION
        }

        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(env)
            .build()

        return Wallet.getPaymentsClient(Snabble.getInstance().application, walletOptions)
    }

    private val allowedCardNetworks = JsonArray().apply {
        add("AMEX")
        add("DISCOVER")
        add("INTERAC")
        add("JCB")
        add("MASTERCARD")
        add("VISA")
    }

    private val allowedCardAuthMethods = JsonArray().apply {
        add("PAN_ONLY")
        add("CRYPTOGRAM_3DS")
    }

    private val baseRequest = JsonObject().apply {
        addProperty("apiVersion", 2)
        addProperty("apiVersionMinor", 0)
    }

    private fun gatewayTokenizationSpecification(): JsonObject {
        return JsonObject().apply {
            addProperty("type", "PAYMENT_GATEWAY")
            add("parameters", JsonObject().apply {
                addProperty("gateway", "example")
                addProperty("gatewayMerchantId", "exampleGatewayMerchantId")
            })
        }
    }

    private fun baseCardPaymentMethod(): JsonObject {
        return JsonObject().apply {
            addProperty("type", "CARD")
            add("parameters", JsonObject().apply {
                add("allowedAuthMethods", allowedCardAuthMethods)
                add("allowedCardNetworks", allowedCardNetworks)
                addProperty("billingAddressRequired", true)
                add("billingAddressParameters", JsonObject().apply {
                    addProperty("format", "FULL")
                })
            })
        }
    }

    private fun cardPaymentMethod(): JsonObject {
        val cardPaymentMethod = baseCardPaymentMethod()
        cardPaymentMethod.add("tokenizationSpecification", gatewayTokenizationSpecification())

        return cardPaymentMethod
    }

    private fun getTransactionInfo(price: String): JsonObject {
        return JsonObject().apply {
            addProperty("totalPrice", price)
            addProperty("totalPriceStatus", "FINAL")
            addProperty("countryCode", "DE")
            addProperty("currencyCode", "EUR")
        }
    }

    private fun getMerchantInfo(): JsonObject {
        return JsonObject().apply {
            addProperty("merchantName", "snabble Google Pay Test")
        }
    }

    fun getPaymentDataRequest(price: String): JsonObject? {
        return baseRequest.apply {
            add("allowedPaymentMethods", JsonArray().apply {
                add(cardPaymentMethod())
            })
            add("transactionInfo", getTransactionInfo(price))
            add("merchantInfo", getMerchantInfo())
//                add("shippingAddressParameters", JsonObject().apply {
//                    addProperty("phoneNumberRequired", false)
//                    add("allowedCountryCodes", JsonArray().apply {
//                        add("DE")
//                    })
//                })
//                addProperty("shippingAddressRequired", true)
        }
    }

    private fun isReadyToPayRequest(): JsonObject? {
        return baseRequest.apply {
            add("allowedPaymentMethods", baseCardPaymentMethod())
        }
    }

    fun isReadyToPay(onComplete: (Boolean) -> Unit) {
        val request = IsReadyToPayRequest.fromJson(GsonHolder.get().toJson(isReadyToPayRequest()))

        val task = googlePayClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                completedTask.getResult(ApiException::class.java)?.let { onComplete(it) }
            } catch (exception: ApiException) {
                onComplete(false)
            }
        }
    }
}