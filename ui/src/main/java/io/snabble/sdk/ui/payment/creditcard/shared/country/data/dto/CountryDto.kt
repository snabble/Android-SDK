package io.snabble.sdk.ui.payment.creditcard.shared.country.data.dto

import com.google.gson.annotations.SerializedName

internal data class CountryDto(
    @SerializedName("code") val countryCode: String,
    @SerializedName("states") val states: List<StateDto>?,
    @SerializedName("numeric") val numericCode: String,
)
