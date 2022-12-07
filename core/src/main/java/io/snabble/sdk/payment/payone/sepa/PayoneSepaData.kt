package io.snabble.sdk.payment.payone.sepa

import io.snabble.sdk.payment.IBAN

data class PayoneSepaData(
    val name: String,
    val iban: String,
    val city: String,
    val country: String,
) {

    init {
        require(name.isNotBlank()) { "Name must not be blank" }
        require(IBAN.validate(iban)) { "IBAN is not valid" }
        require(city.isBlank()) { "City must not be blank" }
        require(country.isNotBlank()) { "Country must not be" }
    }
}
