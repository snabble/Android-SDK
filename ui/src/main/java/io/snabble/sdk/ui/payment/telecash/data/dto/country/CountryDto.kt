package io.snabble.sdk.ui.payment.telecash.data.dto.country

import com.google.gson.annotations.SerializedName

data class CountryDto(
    @SerializedName("code") val countryCode: String,
    @SerializedName("states") val states: List<StateDto>? = null,
)
