package io.snabble.sdk.ui.payment.creditcard.shared.data.country

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.ui.payment.creditcard.shared.domain.models.CountryItem
import java.lang.reflect.Type

internal class LocalCountryItemsDataSourceImpl<T>(
    private val assetManager: AssetManager,
    private val gson: Gson,
    private val clazz: Class<T>,
    private val mapFrom: (T) -> CountryItem
) : LocalCountryItemsDataSource {

    override fun loadCountries(): List<CountryItem> {
        val type: Type = TypeToken.getParameterized(List::class.java, clazz).type
        return gson
            .fromJson<List<T>>(assetManager.open(COUNTRIES_AND_STATES_FILE).reader(), type)
            .map(mapFrom)
            .sortedBy { it.displayName }
    }

    companion object {

        private const val COUNTRIES_AND_STATES_FILE = "countriesAndStates.json"
    }
}
