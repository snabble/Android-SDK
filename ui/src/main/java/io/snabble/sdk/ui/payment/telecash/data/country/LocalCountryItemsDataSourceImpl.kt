package io.snabble.sdk.ui.payment.telecash.data.country

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.ui.payment.telecash.domain.model.country.StateItem
import io.snabble.sdk.ui.payment.telecash.data.dto.country.CountryDto
import io.snabble.sdk.ui.payment.telecash.domain.model.country.CountryItem
import java.util.Locale

internal class LocalCountryItemsDataSourceImpl(
    private val assetManager: AssetManager,
    private val gson: Gson,
) : LocalCountryItemsDataSource {

    override fun loadCountries(): List<CountryItem> {
        val typeToken = object : TypeToken<List<CountryDto>>() {}.type
        return gson.fromJson<List<CountryDto>>(
            assetManager.open(COUNTRIES_AND_STATES_FILE).reader(),
            typeToken
        )
            .map { (countryCode, states) ->
                CountryItem(
                    displayName = countryCode.displayName,
                    code = countryCode,
                    stateItems = states?.map { StateItem.from(it) }
                )
            }
            .sortedBy { it.displayName }
    }

    companion object {

        private const val COUNTRIES_AND_STATES_FILE = "countriesAndStates.json"
    }
}

val String.displayName: String
    get() = Locale("", this).displayName
