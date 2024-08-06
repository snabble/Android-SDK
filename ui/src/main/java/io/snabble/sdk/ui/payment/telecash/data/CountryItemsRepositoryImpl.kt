package io.snabble.sdk.ui.payment.telecash.data

import io.snabble.sdk.ui.payment.telecash.data.country.LocalCountryItemsDataSource
import io.snabble.sdk.ui.payment.telecash.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.telecash.domain.model.country.CountryItem

class CountryItemsRepositoryImpl(
    private val localCountryItemsDataSource: LocalCountryItemsDataSource,
) : CountryItemsRepository {

    override fun loadCountryItems(): List<CountryItem> = localCountryItemsDataSource.loadCountries()
}
