package io.snabble.sdk.ui.payment.creditcard.shared.data

import io.snabble.sdk.ui.payment.creditcard.shared.data.country.LocalCountryItemsDataSource
import io.snabble.sdk.ui.payment.creditcard.shared.domain.CountryItemsRepository
import io.snabble.sdk.ui.payment.creditcard.shared.domain.models.CountryItem

internal class CountryItemsRepositoryImpl(
    private val localCountryItemsDataSource: LocalCountryItemsDataSource,
) : CountryItemsRepository {

    override fun loadCountryItems(): List<CountryItem> = localCountryItemsDataSource.loadCountries()
}
