package io.snabble.sdk.ui.payment.creditcard.shared.country.data

import io.snabble.sdk.ui.payment.creditcard.shared.country.data.source.LocalCountryItemsDataSource
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem

internal class CountryItemsRepositoryImpl(
    private val localCountryItemsDataSource: LocalCountryItemsDataSource,
) : CountryItemsRepository {

    override fun loadCountryItems(): List<CountryItem> = localCountryItemsDataSource.loadCountries()
}
