package io.snabble.sdk.ui.payment.creditcard.shared.country.data.dto

internal interface CountryDto {

    val countryCode: String

    val states: List<StateDto>?
}
