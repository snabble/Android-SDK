package io.snabble.sdk.ui.payment.fiserv.data.dto.country

import com.google.gson.annotations.SerializedName

internal data class CountryDto(
    @SerializedName("code") val countryCode: String,
    @SerializedName("states") val states: List<StateDto>? = null,
)
