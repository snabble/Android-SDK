package io.snabble.sdk.ui.payment.fiserv.data.country

import io.snabble.sdk.ui.payment.fiserv.domain.model.country.CountryItem

internal interface LocalCountryItemsDataSource {

    fun loadCountries(): List<CountryItem>
}
