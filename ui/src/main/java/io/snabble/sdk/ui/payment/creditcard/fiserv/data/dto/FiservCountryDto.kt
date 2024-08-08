package io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto

import com.google.gson.annotations.SerializedName
import io.snabble.sdk.ui.payment.creditcard.shared.country.data.dto.CountryDto
import io.snabble.sdk.ui.payment.creditcard.shared.country.data.dto.StateDto

internal data class FiservCountryDto(
    @SerializedName("code") override val countryCode: String,
    @SerializedName("states") override val states: List<StateDto>? = null,
) : CountryDto
