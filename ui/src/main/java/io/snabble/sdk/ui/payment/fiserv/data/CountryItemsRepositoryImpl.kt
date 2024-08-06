package io.snabble.sdk.ui.payment.fiserv.data

import io.snabble.sdk.ui.payment.fiserv.data.country.LocalCountryItemsDataSource
import io.snabble.sdk.ui.payment.fiserv.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.fiserv.domain.model.country.CountryItem

internal class CountryItemsRepositoryImpl(
    private val localCountryItemsDataSource: LocalCountryItemsDataSource,
) : CountryItemsRepository {

    override fun loadCountryItems(): List<CountryItem> = localCountryItemsDataSource.loadCountries()
}
