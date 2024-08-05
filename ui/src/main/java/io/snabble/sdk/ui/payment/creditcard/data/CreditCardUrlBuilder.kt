package io.snabble.sdk.ui.payment.creditcard.data

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble

class CreditCardUrlBuilder {

    fun createUrlFor(paymentType: PaymentMethod, path: String): String {
        val paymentMethod = when (paymentType) {
            PaymentMethod.MASTERCARD -> "mastercard"
            PaymentMethod.AMEX -> "amex"
            PaymentMethod.VISA -> "visa"
            else -> "visa"
        }
        val appUserId = Snabble.userPreferences.appUser?.id

        return "$path&platform=android&appUserID=$appUserId&paymentMethod=$paymentMethod"
    }
}
