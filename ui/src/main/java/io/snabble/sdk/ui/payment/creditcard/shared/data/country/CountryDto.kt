package io.snabble.sdk.ui.payment.creditcard.shared.data.country

internal interface CountryDto {

    val countryCode: String

    val states: List<StateDto>?
}
