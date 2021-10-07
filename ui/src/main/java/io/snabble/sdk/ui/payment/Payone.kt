package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.SimpleJsonCallback
import kotlinx.parcelize.Parcelize
import okhttp3.*

object Payone {

    @Parcelize
    data class PayoneTokenizationData(
        val merchantID: String,
        val isTesting: Boolean,
        val portalID: String,
        val accountID: String,
        val hash: String,
        val preauthURL: String,
        val pollingURL: String,
        val chargeAmount: String?,
        val currency: String?,
    ) : Parcelable

    @JvmStatic
    fun registerCard(
        activity: FragmentActivity,
        project: Project,
        paymentMethod: PaymentMethod,
        callback: SnabbleUI.Callback
    ) {
        val descriptor = project.paymentMethodDescriptors.find { it.paymentMethod == paymentMethod }
        if (descriptor == null) {
            project.events.logError("Payone error: No payment descriptor")
            Logger.e("Payone error: No payment method descriptor for $paymentMethod")

            Dispatch.mainThread {
                showError(activity, paymentMethod)
            }
            return
        }

        val url = descriptor.links?.get("tokenization")
        if (url == null) {
            project.events.logError("Datatrans Error: No tokenization url")
            Logger.e("Payone error: No tokenization url")

            Dispatch.mainThread {
                showError(activity, paymentMethod)
            }
            return
        }

        val request: Request = Request.Builder()
            .url(Snabble.getInstance().absoluteUrl(url.href))
            .build()

        project.okHttpClient.newCall(request).enqueue(object : SimpleJsonCallback<PayoneTokenizationData>(PayoneTokenizationData::class.java), Callback {
            override fun success(response: PayoneTokenizationData) {
                val args = Bundle()
                args.putString(PayoneInputView.ARG_PROJECT_ID, project.id)
                args.putSerializable(PayoneInputView.ARG_PAYMENT_TYPE, paymentMethod)
                args.putParcelable(PayoneInputView.ARG_TOKEN_DATA, response)
                Dispatch.mainThread {
                    callback.execute(SnabbleUI.Action.SHOW_PAYONE_INPUT, args)
                }
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
}