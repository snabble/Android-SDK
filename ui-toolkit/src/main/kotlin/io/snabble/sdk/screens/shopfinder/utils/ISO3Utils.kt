package io.snabble.sdk.screens.shopfinder.utils

import java.util.*

object ISO3Utils {
    private var localeMap: MutableMap<String, Locale>? = null

    private fun initCountryCodeMapping() {
        val countries = Locale.getISOCountries()
        val map = HashMap<String, Locale>(countries.size)
        for (country in countries) {
            val locale = Locale("", country)
            map[locale.isO3Country.uppercase(Locale.getDefault())] = locale
        }
        localeMap = map
    }

    fun iso3CountryCodeToIso2CountryCode(iso3CountryCode: String): String? {
        if (localeMap == null) {
            initCountryCodeMapping()
        }

        localeMap?.let { map ->
            val locale = map[iso3CountryCode.uppercase(Locale.getDefault())]
            return locale?.country
        }

        return null
    }

    @JvmStatic
    fun getDisplayNameByIso3Code(iso3Country: String): String? {
        val iso2 = iso3CountryCodeToIso2CountryCode(iso3Country)
        return if (iso2 != null) {
            getDisplayNameByIso2Code(iso2)
        } else null
    }

    private fun getDisplayNameByIso2Code(iso2Country: String): String {
        return Locale("", iso2Country).getDisplayCountry(Locale.getDefault())
    }
}