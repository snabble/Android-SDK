package io.snabble.sdk.ui.payment.telecash.domain

import io.snabble.sdk.ui.payment.telecash.domain.model.country.CountryItem

internal interface CountryItemsRepository {

    fun loadCountryItems(): List<CountryItem>
}
