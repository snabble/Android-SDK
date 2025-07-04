package io.snabble.sdk.googlepay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkout.AuthorizePaymentRequest
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger

/**
 * Class for interfacing with google pay
 */
class GooglePayHelper(
    val project: Project,
    val context: Context,
) {
    var paymentDataLauncher: ActivityResultLauncher<Task<PaymentData>>? = null
    private var googlePayClient: PaymentsClient = createPaymentsClient(false)

    private fun createPaymentsClient(useTestEnvironment: Boolean): PaymentsClient {
        Logger.d("Create PaymentsClient for project: ${project.name}, ${if (useTestEnvironment) "TESTING" else ""}")

        val env = if (useTestEnvironment) {
            WalletConstants.ENVIRONMENT_TEST
        } else {
            WalletConstants.ENVIRONMENT_PRODUCTION
        }

        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(env)
            .build()

        return Wallet.getPaymentsClient(Snabble.application, walletOptions)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setUseTestEnvironment(boolean: Boolean) {
        val isUsingTestEnvironment = googlePayClient.apiOptions.environment == WalletConstants.ENVIRONMENT_TEST
        if (isUsingTestEnvironment != boolean) {
            googlePayClient = createPaymentsClient(boolean)
        }
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

    private fun baseRequest(): JsonObject {
        return JsonObject().apply {
            addProperty("apiVersion", 2)
            addProperty("apiVersionMinor", 0)
        }
    }

    private fun gatewayTokenizationSpecification(): JsonObject {
        return JsonObject().apply {
            addProperty("type", "PAYMENT_GATEWAY")
            add("parameters", JsonObject().apply {
                project.checkout.checkoutProcess?.paymentPreauthInformation?.let {
                    val gateway = it.get("gateway")?.asString
                    val gatewayMerchantId = it.get("gatewayMerchantId")?.asString
                    addProperty("gateway", gateway)
                    addProperty("gatewayMerchantId", gatewayMerchantId)
                }
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
            addProperty("countryCode", project.currencyLocale.country)
            addProperty("currencyCode", project.currency.currencyCode)
        }
    }

    private fun getMerchantInfo(): JsonObject {
        return JsonObject().apply {
            addProperty("merchantName", project.company?.name)
        }
    }

    private fun getPaymentDataRequest(price: String): JsonObject {
        return baseRequest().apply {
            add("allowedPaymentMethods", allowedPaymentMethods)
            add("transactionInfo", getTransactionInfo(price))
            add("merchantInfo", getMerchantInfo())
        }
    }

    private fun isReadyToPayRequest(): JsonObject {
        return baseRequest().apply {
            add("allowedPaymentMethods", allowedPaymentMethods)
            addProperty("existingPaymentMethodRequired", false)
        }
    }

    val allowedPaymentMethods; get() = JsonArray().apply {
        add(cardPaymentMethod())
    }

    /**
     * Checks if google pay is ready to make a payment
     */
    fun isReadyToPay(isReadyToPayListener: IsReadyToPayListener) {
        val request = IsReadyToPayRequest.fromJson(GsonHolder.get().toJson(isReadyToPayRequest()))

        val task = googlePayClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                completedTask.getResult(ApiException::class.java)?.let {
                    isReadyToPayListener.isReadyToPay(it)
                }
            } catch (exception: ApiException) {
                isReadyToPayListener.isReadyToPay(false)
            }
        }
    }

    /**
     * Checks if google pay is available on the snabble backend
     */
    fun isGooglePayAvailable(): Boolean {
        return project.availablePaymentMethods.contains(PaymentMethod.GOOGLE_PAY)
    }

    /**
     * Starts a payment request with google pay
     */
    fun requestPayment(priceToPay: Int): Boolean {
        val priceToPayDecimal = priceToPay.toBigDecimal().divide(100.toBigDecimal())
        val task = getLoadPaymentDataTask(priceToPayDecimal.toString())
        task.addOnCompleteListener {
            paymentDataLauncher?.launch(it)
        }

        return false
    }


    private fun getLoadPaymentDataTask(priceLabel: String): Task<PaymentData> {
        val paymentDataRequestJson = getPaymentDataRequest(priceLabel)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        return googlePayClient.loadPaymentData(request)
    }

    fun onResult(resultCode: Status, paymentData: PaymentData?) {
        when (resultCode) {
            Status.RESULT_SUCCESS -> {
                try {
                    val jsonPaymentData = GsonHolder.get().fromJson(paymentData?.toJson(), JsonObject::class.java)

                    val token = jsonPaymentData.get("paymentMethodData")
                        ?.asJsonObject?.get("tokenizationData")
                        ?.asJsonObject?.get("token")
                        ?.asString

                    if (token == null) {
                        project.checkout.abortError()
                    } else {
                        val base64Token = String(Base64.encode(token.toByteArray(), Base64.NO_WRAP))
                        val address: BillingAddress? = jsonPaymentData.get("paymentMethodData")
                            ?.asJsonObject?.get("info")
                            ?.asJsonObject?.get("billingAddress")
                            ?.let { GsonHolder.get().fromJson(it, BillingAddress::class.java) }
                        project.checkout.authorizePayment(
                            AuthorizePaymentRequest(
                                encryptedOrigin = base64Token,
                                name = address?.name,
                                countryCode = address?.countryCode,
                                state = address?.administrativeArea
                            )
                        )
                    }
                } catch (e: Exception) {
                    project.checkout.abortError()
                }
            }

            Status.RESULT_CANCELED -> {
                project.checkout.abort()
            }

            else -> {
                project.checkout.abortError()

                paymentData?.let {
                    Toast.makeText(context, "Google Pay Error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

private data class BillingAddress(val name: String?, val countryCode: String?, val administrativeArea: String?)

interface IsReadyToPayListener {

    fun isReadyToPay(isReadyToPay: Boolean)
}
