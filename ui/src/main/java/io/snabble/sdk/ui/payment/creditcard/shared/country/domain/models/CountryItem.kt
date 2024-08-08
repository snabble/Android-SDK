package io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models

internal data class CountryItem(
    val displayName: String,
    val code: String,
    val numericCode: String,
    val stateItems: List<StateItem>?
)
