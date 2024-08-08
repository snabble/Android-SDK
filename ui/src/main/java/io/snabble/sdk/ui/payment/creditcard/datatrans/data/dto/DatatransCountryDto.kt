package io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto

import com.google.gson.annotations.SerializedName
import io.snabble.sdk.ui.payment.creditcard.shared.country.data.dto.CountryDto
import io.snabble.sdk.ui.payment.creditcard.shared.country.data.dto.StateDto

internal data class DatatransCountryDto(
    @SerializedName("code") override val countryCode: String,
    @SerializedName("states") override val states: List<StateDto>? = null,
    @SerializedName("numeric") val numericCode: String,
) : CountryDto
