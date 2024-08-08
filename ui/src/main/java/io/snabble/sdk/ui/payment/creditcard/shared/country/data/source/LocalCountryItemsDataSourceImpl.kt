package io.snabble.sdk.ui.payment.creditcard.shared.country.data.source

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem
import java.io.InputStreamReader
import java.lang.reflect.Type

internal class LocalCountryItemsDataSourceImpl<T>(
    private val assetManager: AssetManager,
    private val gson: Gson,
    private val clazz: Class<T>,
    private val mapFrom: (T) -> CountryItem
) : LocalCountryItemsDataSource {

    override fun loadCountries(): List<CountryItem> {
        val type: Type = TypeToken.getParameterized(List::class.java, clazz).type
        val jsonFileReader: InputStreamReader = assetManager.open(COUNTRIES_AND_STATES_FILE).reader()
        return gson
            .fromJson<List<T>>(jsonFileReader, type)
            .map(mapFrom)
            .sortedBy { it.displayName }
    }

    companion object {

        private const val COUNTRIES_AND_STATES_FILE = "countriesAndStates.json"
    }
}
