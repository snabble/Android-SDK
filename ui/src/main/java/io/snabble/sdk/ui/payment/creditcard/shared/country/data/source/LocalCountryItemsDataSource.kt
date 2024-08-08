package io.snabble.sdk.ui.payment.creditcard.shared.country.data.source

import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem

internal interface LocalCountryItemsDataSource {

    fun loadCountries(): List<CountryItem>
}
