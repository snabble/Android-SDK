package io.snabble.sdk.ui.payment.telecash.domain.model.country

import com.tegut.tbox.account.details.domain.model.country.StateItem

data class CountryItem(val displayName: String, val code: String, val stateItems: List<StateItem>?)
