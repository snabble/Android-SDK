package io.snabble.sdk.ui.payment.creditcard.shared.data.country

import io.snabble.sdk.ui.payment.creditcard.shared.domain.models.CountryItem

internal interface LocalCountryItemsDataSource {

    fun loadCountries(): List<CountryItem>
}
