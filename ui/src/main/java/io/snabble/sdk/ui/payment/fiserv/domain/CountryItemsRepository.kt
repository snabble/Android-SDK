package io.snabble.sdk.ui.payment.fiserv.domain

import io.snabble.sdk.ui.payment.fiserv.domain.model.country.CountryItem

internal interface CountryItemsRepository {

    fun loadCountryItems(): List<CountryItem>
}
