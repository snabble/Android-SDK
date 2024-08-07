package io.snabble.sdk.ui.payment.creditcard.fiserv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CreditCardInfo(
    @SerialName("cardHolder") val cardHolder: String,
    @SerialName("cardNumber") val obfuscatedCardNumber: String,
    @SerialName("ccBrand") val brand: String,
    @SerialName("expYear") val expirationYear: String,
    @SerialName("expMonth") val expirationMonth: String,
    @SerialName("hostedDataId") val hostedDataId: String,
    @SerialName("schemeTransactionId") val schemeTransactionId: String,
    @SerialName("transactionId") val transactionId: String,
    @SerialName("storeId") val storeId: String
) {

    companion object {

        @JvmStatic
        fun String.toCreditCardInfo(): CreditCardInfo {
            val format = Json { ignoreUnknownKeys = true }
            return format.decodeFromString<CreditCardInfo>(this)
        }
    }
}
