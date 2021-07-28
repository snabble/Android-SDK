package io.snabble.sdk.ui.payment

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import ch.datatrans.payment.api.Transaction
import ch.datatrans.payment.api.TransactionListener
import ch.datatrans.payment.api.TransactionRegistry
import ch.datatrans.payment.api.TransactionSuccess
import ch.datatrans.payment.exception.TransactionException
import ch.datatrans.payment.paymentmethods.CardToken
import ch.datatrans.payment.paymentmethods.PostFinanceCardToken
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
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
            val isTesting: Boolean?,
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
            val descriptor = project.paymentMethodDescriptors.find { it.paymentMethod == paymentMethod }
            if (descriptor == null) {
                Logger.e("Datatrans error: No payment method descriptor for $paymentMethod")

                Dispatch.mainThread {
                    showError(activity, paymentMethod)
                }
                return
            }

            val url = descriptor.links?.get("tokenization")
            if (url == null) {
                Logger.e("Datatrans error: No tokenization url")

                Dispatch.mainThread {
                    showError(activity, paymentMethod)
                }
                return
            }

            val request: Request = Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url.href))
                .post(GsonHolder.get().toJson(
                    DatatransTokenizationRequest(paymentMethod)
                ).toRequestBody("application/json".toMediaType()))
                .build()

            val okClient = project.okHttpClient
            okClient.newCall(request).enqueue(object : SimpleJsonCallback<DatatransTokenizationResponse>(DatatransTokenizationResponse::class.java), Callback {
                override fun success(response: DatatransTokenizationResponse) {
                    startDatatransTransaction(activity, response, paymentMethod, project)
                }

                override fun error(t: Throwable?) {
                    Dispatch.mainThread {
                        showError(activity, paymentMethod)
                    }

                    Logger.e("Datatrans error: ${t?.message}")
                }
            })
        }

        private fun showError(activity: FragmentActivity, paymentMethod: PaymentMethod) {
            if (!activity.isDestroyed) {
                val err = when (paymentMethod) {
                    PaymentMethod.TWINT -> {
                        R.string.Snabble_Payment_Twint_error
                    }
                    PaymentMethod.POST_FINANCE_CARD -> {
                        R.string.Snabble_Payment_PostFinanceCard_error
                    }
                    else -> {
                        R.string.Snabble_Payment_CreditCard_error
                    }
                }

                Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
            }
        }

        private fun startDatatransTransaction(activity: FragmentActivity, tokenizationResponse: DatatransTokenizationResponse, paymentMethod: PaymentMethod, project: Project) {
            val transaction = Transaction(tokenizationResponse.mobileToken)
            transaction.listener = object : TransactionListener {
                override fun onTransactionSuccess(result: TransactionSuccess) {
                    val token = result.paymentMethodToken
                    var month = ""
                    var year = ""

                    when (token) {
                        is PostFinanceCardToken -> {
                            token.cardExpiryDate?.let {
                                month = it.formattedMonth
                                year = it.formattedYear
                            }
                        }
                        is CardToken -> {
                            token.cardExpiryDate?.let {
                                month = it.formattedMonth
                                year = it.formattedYear
                            }
                        }
                    }

                    if (token != null) {
                        Keyguard.unlock(activity, object :  Keyguard.Callback {
                            override fun success() {
                                val store = Snabble.getInstance().paymentCredentialsStore
                                val credentials = PaymentCredentials.fromDatatrans(
                                    token.token,
                                    PaymentCredentials.Brand.fromPaymentMethod(paymentMethod),
                                    result.paymentMethodToken?.getDisplayTitle(activity),
                                    month,
                                    year,
                                    project.id,
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
                    Dispatch.mainThread {
                        project.events.logError("Datatrans TransactionException: " + exception.message)
                        showError(activity, paymentMethod)
                    }
                }
            }
            transaction.options.appCallbackScheme = "snabble"
            transaction.options.isTesting = tokenizationResponse.isTesting ?: false
            transaction.options.useCertificatePinning = true
            TransactionRegistry.startTransaction(activity, transaction)
        }
    }
}