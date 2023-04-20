package io.snabble.sdk.ui.payment.creditcard.data

import android.net.Uri
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble

class CreditCardUrlBuilder {

    fun createUrlFor(projectId: String, paymentType: PaymentMethod): String {
        val builder = Uri.Builder()
        val paymentMethod = when (paymentType) {
            PaymentMethod.MASTERCARD -> "mastercard"
            PaymentMethod.AMEX -> "amex"
            PaymentMethod.VISA -> "visa"
            else -> "visa"
        }
        val appUserId = Snabble.userPreferences.appUser?.id
        val authority = Snabble.endpointBaseUrl.substringAfter("https://")

        return builder.scheme("https")
            .authority(authority)
            .appendPath(projectId)
            .appendPath("telecash")
            .appendPath("form")
            .appendQueryParameter("platform", "android")
            .appendQueryParameter("appUserID", appUserId)
            .appendQueryParameter("paymentMethod", paymentMethod)
            .build().toString()
    }
}
