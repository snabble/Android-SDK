@file:JvmName("SnabbleCreditCardUrlCreator")

package io.snabble.sdk.ui.payment.creditcard.fiserv.data

import android.net.Uri
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble

fun createCreditCardUrlFor(paymentType: PaymentMethod, url: String): String {
    val paymentMethod = when (paymentType) {
        PaymentMethod.MASTERCARD -> "mastercard"
        PaymentMethod.AMEX -> "amex"
        PaymentMethod.VISA -> "visa"
        else -> "visa"
    }
    val appUserId = Snabble.userPreferences.appUser?.id
    return Uri.parse(url)
        .buildUpon()
        .appendQueryParameter(PARAM_KEY_PLATFORM, "android")
        .appendQueryParameter(PARAM_KEY_ADD_USER_ID, appUserId)
        .appendQueryParameter(PARAM_KEY_PAYMENT_METHOD, paymentMethod)
        .build()
        .toString()
}

private const val PARAM_KEY_PLATFORM = "platform"
private const val PARAM_KEY_ADD_USER_ID = "appUserID"
private const val PARAM_KEY_PAYMENT_METHOD = "paymentMethod"
