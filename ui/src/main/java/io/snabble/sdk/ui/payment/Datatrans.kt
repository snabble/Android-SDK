package io.snabble.sdk.ui.payment

import android.util.Base64
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import ch.datatrans.payment.api.Transaction
import ch.datatrans.payment.api.TransactionListener
import ch.datatrans.payment.api.TransactionRegistry
import ch.datatrans.payment.api.TransactionSuccess
import ch.datatrans.payment.exception.TransactionException
import ch.datatrans.payment.paymentmethods.PaymentMethodToken
import com.google.gson.JsonObject
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.SimpleJsonCallback
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

class Datatrans {
    companion object {
        data class DatatransTokenizationRequest(
            val paymentMethod: PaymentMethod,
            val language: String = Locale.getDefault().language,
        )

        data class DatatransTokenizationResponse(
            val mobileToken: String,
        )

        val datatransPaymentMethods = mapOf(
            PaymentMethod.MASTERCARD to "ECA",
            PaymentMethod.VISA to "VIS",
            PaymentMethod.AMEX to "AMX",
            PaymentMethod.TWINT to "TWI",
            PaymentMethod.POST_FINANCE_CARD to "PFC"
        )

        @JvmStatic
        fun registerCard(activity: FragmentActivity, project: Project, paymentMethod: PaymentMethod) {
            val request: Request = Request.Builder()
                .url(project.datatransTokenizationUrl)
                .post(GsonHolder.get().toJson(
                    DatatransTokenizationRequest(paymentMethod)
                ).toRequestBody("application/json".toMediaType()))
                .build()

            val okClient = project.okHttpClient
            okClient.newCall(request).enqueue(object : SimpleJsonCallback<DatatransTokenizationResponse>(DatatransTokenizationResponse::class.java), Callback {
                override fun success(response: DatatransTokenizationResponse) {
                    startDatatransTransaction(activity, response, paymentMethod)
                }

                override fun error(t: Throwable?) {
                    Dispatch.mainThread {
                        if (!activity.isDestroyed) {
                            Toast.makeText(activity, R.string.Snabble_Payment_CreditCard_error, Toast.LENGTH_LONG).show()
                        }
                    }

                    Logger.e("Datatrans error: ${t?.message}")
                }
            })
        }

        private fun startDatatransTransaction(activity: FragmentActivity, tokenizationResponse: DatatransTokenizationResponse, paymentMethod: PaymentMethod) {
            val transaction = Transaction(tokenizationResponse.mobileToken)
            transaction.listener = object : TransactionListener {
                override fun onTransactionSuccess(result: TransactionSuccess) {
                    val token = result.paymentMethodToken
                    if (token != null) {
                        Keyguard.unlock(activity, object :  Keyguard.Callback {
                            override fun success() {
                                val store = Snabble.getInstance().paymentCredentialsStore
                                val credentials = PaymentCredentials.fromDatatrans(
                                    token.toJson(),
                                    PaymentCredentials.Brand.fromPaymentMethod(paymentMethod),
                                    result.paymentMethodToken?.getDisplayTitle(activity)
                                )
                                store.add(credentials)
                            }

                            override fun error() {
                                Toast.makeText(activity, R.string.Snabble_SEPA_encryptionError, Toast.LENGTH_LONG).show()
                            }
                        })
                    } else {
                        Toast.makeText(activity, R.string.Snabble_SEPA_encryptionError, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onTransactionError(exception: TransactionException) {
                    Logger.e("Datatrans error: ${exception.message}")
                }
            }
            transaction.options.appCallbackScheme = "snabble"
            transaction.options.isTesting = true
            transaction.options.useCertificatePinning = true
            TransactionRegistry.startTransaction(activity, transaction)
        }
    }
}