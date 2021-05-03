package io.snabble.sdk.ui.payment

import android.app.Activity
import android.util.Base64
import ch.datatrans.payment.api.Transaction
import ch.datatrans.payment.api.TransactionListener
import ch.datatrans.payment.api.TransactionRegistry
import ch.datatrans.payment.api.TransactionSuccess
import ch.datatrans.payment.exception.TransactionException
import ch.datatrans.payment.paymentmethods.PaymentMethodToken
import com.google.gson.JsonObject
import io.snabble.sdk.CheckoutApi
import io.snabble.sdk.CheckoutApi.SignedCheckoutInfo
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.payment.PaymentCredentialsStore
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.SimpleJsonCallback
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

// --- server to server requests - those will get removed when backend support is ready ---
data class DatatransMobileTokenRequest(
    val refno : String,
    val currency: String,
    val language: String? = null,
    val paymentMethods: List<String>,
    val amount : Int?,
    val option : DatatransMobileTokenRequestOptions = DatatransMobileTokenRequestOptions()
)

data class DatatransMobileTokenRequestOptions (
    val returnMobileToken: Boolean = true
)

data class DatatransMobileTokenResponse (
    val mobileToken: String
)

class Datatrans {
    companion object {
        val paymentMethods = listOf("ECA", "VIS", "AMX", "TWI", "PFC")

        @JvmStatic
        fun registerPaymentInfo(activity: Activity) {
            val data = DatatransMobileTokenRequest(
                refno = UUID.randomUUID().toString().replace("-", "").take(20),
                currency = "CHF",
                amount = 0,
                paymentMethods = paymentMethods,
            )

            makeRequest(activity, data)
        }

        @JvmStatic
        fun pay(activity: Activity, amount: Int) {
            val data = DatatransMobileTokenRequest(
                refno = UUID.randomUUID().toString().replace("-", "").take(20),
                currency = "CHF",
                amount = amount,
                paymentMethods = paymentMethods,
            )

            makeRequest(activity, data)
        }

        private fun makeRequest(activity: Activity, data: DatatransMobileTokenRequest) {
            val request: Request = Request.Builder()
                .url("https://api.sandbox.datatrans.com/v1/transactions")
                .header("Authorization", "Basic ${Base64.encodeToString("1100029137:btcgYLp7UgeK9URR".toByteArray(), Base64.NO_WRAP)}")
                .post(GsonHolder.get().toJson(data).toRequestBody("application/json".toMediaType()))
                .build()

            val okClient = Snabble.getInstance().okHttpClient
            okClient.newCall(request).enqueue(object : SimpleJsonCallback<DatatransMobileTokenResponse>(DatatransMobileTokenResponse::class.java), Callback {
                override fun success(response: DatatransMobileTokenResponse) {
                    startDatatransTransaction(
                        activity = activity,
                        mobileToken = response.mobileToken,
                        saveCredentials = data.amount == 0
                    )
                    Logger.e("Datatrans transaction: ${response.mobileToken}")
                }

                override fun error(t: Throwable?) {
                    Logger.e("Datatrans error: ${t?.message}")
                }
            })
        }

        private fun startDatatransTransaction(activity: Activity, mobileToken: String, saveCredentials: Boolean = false) {
            val transaction = if (saveCredentials) {
                Transaction(mobileToken)
            } else {
                val paymentMethodTokens = Snabble.getInstance().paymentCredentialsStore.all
                    .filter { it.type == PaymentCredentials.Type.DATATRANS }
                    .map { PaymentMethodToken.create(it.encryptedData) }
                Transaction(mobileToken, paymentMethodTokens)
            }

            transaction.listener = object : TransactionListener {
                override fun onTransactionSuccess(result: TransactionSuccess) {
                    if (saveCredentials) {
                        result.paymentMethodToken?.let { token ->
                            PaymentCredentials.fromDatatrans(token.toJson(), result.paymentMethodType.identifier)
                        }
                    } else {
                        // TODO
                    }
                }

                override fun onTransactionError(exception: TransactionException) {
                    Logger.e("Datatrans error: ${exception?.message}")
                }
            }
            transaction.options.appCallbackScheme = "snabble"
            transaction.options.isTesting = true
            transaction.options.useCertificatePinning = true
            TransactionRegistry.startTransaction(activity, transaction)
        }
    }
}