package io.snabble.sdk.ui.payment.datatrans.data.dto

import com.google.gson.annotations.SerializedName
import io.snabble.sdk.ui.payment.shared.data.country.CountryDto
import io.snabble.sdk.ui.payment.shared.data.country.StateDto

internal data class DatatransCountryDto(
    @SerializedName("code") override val countryCode: String,
    @SerializedName("states") override val states: List<StateDto>? = null,
    @SerializedName("numeric") val numericCode: String,
) : CountryDto
