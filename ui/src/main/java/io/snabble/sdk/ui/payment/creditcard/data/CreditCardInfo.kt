package io.snabble.sdk.ui.payment.creditcard.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class CreditCardInfo(
    @SerialName("bname") val cardHolder: String,
    @SerialName("cardnumber") val obfuscatedCardNumber: String,
    @SerialName("ccbrand") val brand: String,
    @SerialName("expyear") val expirationYear: String,
    @SerialName("expmonth") val expirationMonth: String,
    @SerialName("hosteddataid") val hostedDataId: String,
    @SerialName("schemeTransactionId") val schemeTransactionId: String,
    @SerialName("oid") val transactionId: String,
    @SerialName("storeId") val storeId: String
){
    companion object {
        @JvmStatic
        fun String.toCreditCardInfo(): CreditCardInfo{
            val format = Json { ignoreUnknownKeys = true }
            return format.decodeFromString<CreditCardInfo>(this)
        }
    }
}
