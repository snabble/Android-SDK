package io.snabble.sdk.ui.payment.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreditCardInfo(
    @SerialName("bname") val cardHolder: String,
    @SerialName("cardnumber") val obfuscatedCardNumber: String,
    @SerialName("ccbrand") val brand: String,
    @SerialName("expyear") val expirationYear: String,
    @SerialName("expmonth") val expirationMonth: String,
    @SerialName("hosteddataid") val hostedDataId: String,
    @SerialName("schemeTransactionId") val schemeTransactionId: String,
    @SerialName("oid") val transactionId: String
)
