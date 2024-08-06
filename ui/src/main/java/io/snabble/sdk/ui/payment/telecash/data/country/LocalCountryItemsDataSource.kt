package io.snabble.sdk.ui.payment.telecash.data.country

import io.snabble.sdk.ui.payment.telecash.domain.model.country.CountryItem

interface LocalCountryItemsDataSource {

    fun loadCountries(): List<CountryItem>
}
