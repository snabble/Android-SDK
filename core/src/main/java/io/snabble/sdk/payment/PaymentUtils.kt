@file:JvmName("PaymentUtils")
package io.snabble.sdk.payment


fun List<PaymentCredentials>.filterValidTypes() =
    filter { it.type != null }
