package io.snabble.sdk.ui.payment

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import ch.datatrans.payment.api.Transaction
import ch.datatrans.payment.api.TransactionListener
import ch.datatrans.payment.api.TransactionRegistry
import ch.datatrans.payment.api.TransactionSuccess
import ch.datatrans.payment.exception.TransactionException
import ch.datatrans.payment.paymentmethods.SavedCard
import ch.datatrans.payment.paymentmethods.SavedPostFinanceCard
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
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

object Datatrans {
    data class DatatransTokenizationRequest(
        val paymentMethod: PaymentMethod,
        val language: String = Locale.getDefault().language,
    )

    data class DatatransTokenizationResponse(
        val mobileToken: String,
        val isTesting: Boolean?,
    )

    @JvmStatic
    fun registerCard(activity: FragmentActivity, project: Project, paymentMethod: PaymentMethod) {
        val descriptor = project.paymentMethodDescriptors.find { it.paymentMethod == paymentMethod }
        if (descriptor == null) {
            project.events.logError("Datatrans Error: No payment descriptor")
            Logger.e("Datatrans error: No payment method descriptor for $paymentMethod")

            Dispatch.mainThread {
                showError(activity, paymentMethod)
            }
            return
        }

        val url = descriptor.links?.get("tokenization")
        if (url == null) {
            project.events.logError("Datatrans Error: No tokenization url")
            Logger.e("Datatrans error: No tokenization url")

            Dispatch.mainThread {
                showError(activity, paymentMethod)
            }
            return
        }

        val request: Request = Request.Builder()
            .url(Snabble.absoluteUrl(url.href))
            .post(
                GsonHolder.get().toJson(
                    DatatransTokenizationRequest(paymentMethod)
                ).toRequestBody("application/json".toMediaType())
            )
            .build()

        project.okHttpClient.newCall(request).enqueue(object :
            SimpleJsonCallback<DatatransTokenizationResponse>(DatatransTokenizationResponse::class.java), Callback {
            override fun success(response: DatatransTokenizationResponse) {
                startDatatransTransaction(activity, response, paymentMethod, project)
            }

            override fun error(t: Throwable?) {
                Dispatch.mainThread {
                    showError(activity, paymentMethod)
                }

                project.events.logError("Datatrans Tokenization Error: " + t?.message)
                Logger.e("Datatrans Tokenization Error: ${t?.message}")
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

    private fun startDatatransTransaction(
        activity: FragmentActivity,
        tokenizationResponse: DatatransTokenizationResponse,
        paymentMethod: PaymentMethod,
        project: Project
    ) {
        val transaction = Transaction(tokenizationResponse.mobileToken)
        transaction.listener = object : TransactionListener {
            override fun onTransactionSuccess(result: TransactionSuccess) {
                activity.runOnUiThreadWhenResumed {
                    val token = result.savedPaymentMethod
                    var month = ""
                    var year = ""

                    when (token) {
                        is SavedPostFinanceCard -> {
                            token.cardExpiryDate?.let {
                                month = it.formattedMonth
                                year = it.formattedYear
                            }
                        }
                        is SavedCard -> {
                            token.cardExpiryDate?.let {
                                month = it.formattedMonth
                                year = it.formattedYear
                            }
                        }
                    }

                    if (token != null) {
                        Keyguard.unlock(activity, object : Keyguard.Callback {
                            override fun success() {
                                val store = Snabble.paymentCredentialsStore
                                val credentials = PaymentCredentials.fromDatatrans(
                                    token.alias,
                                    PaymentCredentials.Brand.fromPaymentMethod(paymentMethod),
                                    result.savedPaymentMethod?.getDisplayTitle(activity),
                                    month,
                                    year,
                                    project.id,
                                )
                                store.add(credentials)
                            }

                            override fun error() {
                                Toast.makeText(activity, R.string.Snabble_SEPA_encryptionError, Toast.LENGTH_LONG)
                                    .show()
                            }
                        })
                    } else {
                        Toast.makeText(activity, R.string.Snabble_SEPA_encryptionError, Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onTransactionError(exception: TransactionException) {
                activity.runOnUiThreadWhenResumed {
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

fun FragmentActivity.runOnUiThreadWhenResumed(task: () -> Unit) {
    Dispatch.mainThread {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            Dispatch.mainThread {
                task()
            }
        } else {
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    Dispatch.mainThread {
                        task()
                    }
                    lifecycle.removeObserver(this)
                }
            })
        }
    }
}
