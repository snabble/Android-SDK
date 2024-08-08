package io.snabble.sdk.ui.payment.creditcard.shared

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.PaymentMethodDescriptor
import io.snabble.sdk.Snabble

fun List<PaymentMethodDescriptor>.getTokenizationUrlFor(paymentMethod: PaymentMethod): String? =
    firstOrNull { it.paymentMethod == paymentMethod }
        ?.links
        ?.get("tokenization")
        ?.href
        ?.let(Snabble::absoluteUrl)
