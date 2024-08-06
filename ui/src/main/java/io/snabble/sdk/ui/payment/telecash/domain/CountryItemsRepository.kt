package io.snabble.sdk.ui.payment.telecash.domain

import io.snabble.sdk.ui.payment.telecash.domain.model.country.CountryItem

interface CountryItemsRepository {

    fun loadCountryItems(): List<CountryItem>
}
