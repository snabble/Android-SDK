package io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto

import com.google.gson.annotations.SerializedName

internal data class AuthDataDto(
    @SerializedName("links") val links: LinksDto
)

internal data class LinksDto(
    @SerializedName("self") val deleteUrl: LinkDto,
    @SerializedName("tokenizationForm") val formUrl: LinkDto
)

internal data class LinkDto(
    @SerializedName("href") val href: String
)
