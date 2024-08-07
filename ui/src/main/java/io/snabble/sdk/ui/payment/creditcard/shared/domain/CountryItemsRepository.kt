package io.snabble.sdk.ui.payment.creditcard.shared.domain

import io.snabble.sdk.ui.payment.creditcard.shared.domain.models.CountryItem

internal interface CountryItemsRepository {

    fun loadCountryItems(): List<CountryItem>
}
