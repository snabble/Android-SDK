package io.snabble.sdk.ui.payment.shared.data.country

internal interface CountryDto {

    val countryCode: String

    val states: List<StateDto>?
}
