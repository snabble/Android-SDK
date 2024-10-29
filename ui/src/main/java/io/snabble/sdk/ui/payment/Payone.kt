package io.snabble.sdk.ui.payment

import android.os.Parcelable
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.data.FormPrefillData
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.payment.PayoneInputView.Companion.ARG_FORM_PREFILL_DATA
import io.snabble.sdk.ui.payment.PayoneInputView.Companion.ARG_PAYMENT_TYPE
import io.snabble.sdk.ui.payment.PayoneInputView.Companion.ARG_PROJECT_ID
import io.snabble.sdk.ui.payment.PayoneInputView.Companion.ARG_SAVE_PAYMENT_CREDENTIALS
import io.snabble.sdk.ui.payment.PayoneInputView.Companion.ARG_TOKEN_DATA
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.SimpleJsonCallback
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import okhttp3.Callback
import okhttp3.Request

object Payone {

    @Serializable
    @Parcelize
    data class PayoneTokenizationData(
        val merchantID: String,
        val isTesting: Boolean,
        val portalID: String,
        val accountID: String,
        val hash: String,
        val preAuthInfo: PreAuthInfo,
        val links: Map<String, Link>
    ) : Parcelable

    @Serializable
    @Parcelize
    data class PreAuthInfo(
        val amount: Int?,
        val currency: String?
    ) : Parcelable

    @Serializable
    @Parcelize
    data class Link(
        val href: String?
    ) : Parcelable

    data class PreAuthRequest(
        @SerializedName("pseudoCardPAN") val pseudoCardPan: String,
        @SerializedName("lastName") val name: String,
        @SerializedName("email") val email: String,
        @SerializedName("address") val address: Address
    ) {

        data class Address(
            @SerializedName("street") val street: String,
            @SerializedName("zip") val zip: String,
            @SerializedName("city") val city: String,
            @SerializedName("country") val country: String,
            @SerializedName("state") val state: String?,
        )
    }

    data class PreAuthResponse(
        val status: AuthStatus,
        val userID: String,
        val links: Map<String, Link>
    )

    enum class AuthStatus {
        pending, successful, failed
    }

    @JvmStatic
    fun registerCard(
        activity: FragmentActivity,
        project: Project,
        paymentMethod: PaymentMethod,
        saveCredentials: Boolean,
        formPrefillData: FormPrefillData?
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
            project.events.logError("Payone Error: No tokenization url")
            Logger.e("Payone error: No tokenization url")

            Dispatch.mainThread {
                showError(activity, paymentMethod)
            }
            return
        }

        val request: Request = Request.Builder()
            .url(Snabble.absoluteUrl(url.href))
            .build()

        project.okHttpClient.newCall(request)
            .enqueue(object : SimpleJsonCallback<PayoneTokenizationData>(PayoneTokenizationData::class.java), Callback {
                override fun success(response: PayoneTokenizationData) {
                    val args = bundleOf(
                        ARG_PROJECT_ID to project.id,
                        ARG_SAVE_PAYMENT_CREDENTIALS to saveCredentials,
                        ARG_PAYMENT_TYPE to paymentMethod,
                        ARG_TOKEN_DATA to response,
                        ARG_FORM_PREFILL_DATA to formPrefillData
                    )
                    Dispatch.mainThread {
                        SnabbleUI.executeAction(activity, SnabbleUI.Event.SHOW_PAYONE_INPUT, args)
                    }
                }

                override fun error(t: Throwable?) {
                    Dispatch.mainThread {
                        showError(activity, paymentMethod)
                    }

                    project.events.logError("Payone Tokenization Error: " + t?.message)
                    Logger.e("Payone Tokenization Error: ${t?.message}")
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
