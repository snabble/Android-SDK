package io.snabble.sdk.ui.payment.creditcard.shared.country.domain

import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem

internal interface CountryItemsRepository {

    fun loadCountryItems(): List<CountryItem>
}
