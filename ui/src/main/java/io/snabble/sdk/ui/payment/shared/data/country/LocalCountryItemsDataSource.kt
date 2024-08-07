package io.snabble.sdk.ui.payment.shared.data.country

import io.snabble.sdk.ui.payment.shared.domain.models.CountryItem

internal interface LocalCountryItemsDataSource {

    fun loadCountries(): List<CountryItem>
}
