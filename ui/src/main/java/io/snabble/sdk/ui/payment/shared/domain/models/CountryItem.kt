package io.snabble.sdk.ui.payment.shared.domain.models

internal data class CountryItem(val displayName: String, val code: String, val stateItems: List<StateItem>?)
