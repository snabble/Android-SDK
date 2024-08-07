package io.snabble.sdk.ui.payment.shared.domain

import io.snabble.sdk.ui.payment.shared.domain.models.CountryItem

internal interface CountryItemsRepository {

    fun loadCountryItems(): List<CountryItem>
}
