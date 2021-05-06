package io.snabble.sdk.googlepay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger

class GooglePayHelper(
    val project: Project,
    val context: Context,
    val useTestEnvironment: Boolean = false,
) {
    private var googlePayClient: PaymentsClient

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
                addProperty("gateway", "firstdata")
                addProperty("gatewayMerchantId", "3176752955")
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

    fun requestPayment(priceToPay: Int): Boolean {
        val priceToPayDecimal = priceToPay.toBigDecimal().divide(100.toBigDecimal())
        val intent = Intent(context, GooglePayHelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(GooglePayHelperActivity.INTENT_EXTRA_PROJECT_ID, project.id)
        intent.putExtra(GooglePayHelperActivity.INTENT_EXTRA_PAYMENT_PRICE_TO_PAY, priceToPayDecimal.toString())
        context.startActivity(intent)

        return false
    }

    fun loadPaymentData(priceToPay: String, activity: Activity, requestCode: Int): Boolean {
        val paymentDataRequestJson = getPaymentDataRequest(priceToPay)
        if (paymentDataRequestJson == null) {
            Logger.e("Could not create google pay request")
            return false
        }

        val request = PaymentDataRequest.fromJson(GsonHolder.get().toJson(paymentDataRequestJson))
        if (request != null) {
            val task = googlePayClient.loadPaymentData(request)
            AutoResolveHelper.resolveTask(task, activity, requestCode)
            return true
        }

        return false
    }

    fun onActivityResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            AppCompatActivity.RESULT_OK -> {
                data?.let { intent ->
                    val paymentData = PaymentData.getFromIntent(intent)
                    Toast.makeText(context, "Google Pay Success: ${paymentData?.toJson()}", Toast.LENGTH_LONG).show()
                }
            }
            AppCompatActivity.RESULT_CANCELED -> {
                // The user cancelled the payment attempt
            }
            AutoResolveHelper.RESULT_ERROR -> {
                AutoResolveHelper.getStatusFromIntent(data)?.let {
                    Toast.makeText(context, "Google Pay Error ${it.statusCode}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}