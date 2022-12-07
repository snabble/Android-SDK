package io.snabble.sdk.payment.payone.sepa

import io.snabble.sdk.payment.IBAN

data class PayoneSepaData(
    val name: String,
    val iban: String,
    val city: String,
    val country: String,
) {

    init {
        if (name.isBlank()) throw IllegalArgumentException("Name must not be blank")
        if (!IBAN.validate(iban)) throw IllegalArgumentException("IBAN must not be blank")
        if (city.isBlank()) throw IllegalArgumentException("City must not be blank")
        if (country.isBlank()) throw IllegalArgumentException("Country must not be blank")
    }
}
