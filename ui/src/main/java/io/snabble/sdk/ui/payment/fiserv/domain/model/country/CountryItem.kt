package io.snabble.sdk.ui.payment.fiserv.domain.model.country

internal data class CountryItem(val displayName: String, val code: String, val stateItems: List<StateItem>?)