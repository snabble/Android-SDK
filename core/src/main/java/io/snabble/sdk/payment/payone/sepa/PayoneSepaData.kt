package io.snabble.sdk.payment.payone.sepa

import com.google.gson.annotations.SerializedName
import io.snabble.sdk.payment.IBAN

data class PayoneSepaData(
    @SerializedName("lastname") val name: String,
    @SerializedName("iban") val iban: String,
    @SerializedName("city") val city: String,
    @SerializedName("countryCode") val countryCode: String,
) {

    init {
        require(name.isNotBlank()) { "Name must not be blank" }
        require(IBAN.validate(iban)) { "IBAN is not valid" }
        require(city.isNotBlank()) { "City must not be blank" }
        require(countryCode.isNotBlank()) { "Country must not be" }
    }
}
