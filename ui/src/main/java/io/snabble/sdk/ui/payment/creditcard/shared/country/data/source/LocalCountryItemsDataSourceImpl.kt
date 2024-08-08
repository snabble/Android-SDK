package io.snabble.sdk.ui.payment.creditcard.shared.country.data.source

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.ui.payment.creditcard.shared.country.data.dto.CountryDto
import io.snabble.sdk.ui.payment.creditcard.shared.country.displayName
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.StateItem
import java.io.InputStreamReader

internal class LocalCountryItemsDataSourceImpl(
    private val assetManager: AssetManager,
    private val gson: Gson,
) : LocalCountryItemsDataSource {

    override fun loadCountries(): List<CountryItem> {
        val jsonFileReader: InputStreamReader =
            assetManager.open(COUNTRIES_AND_STATES_FILE).reader()
        val type = object : TypeToken<List<CountryDto>>() {}.type
        return gson
            .fromJson<List<CountryDto>>(jsonFileReader, type)
            .map {
                CountryItem(
                    displayName = it.countryCode.displayName,
                    code = it.countryCode,
                    numericCode = it.numericCode,
                    stateItems = it.states?.map(StateItem.Companion::from)
                )
            }
            .sortedBy { it.displayName }
    }

    companion object {

        private const val COUNTRIES_AND_STATES_FILE = "countriesAndStates.json"
    }
}
