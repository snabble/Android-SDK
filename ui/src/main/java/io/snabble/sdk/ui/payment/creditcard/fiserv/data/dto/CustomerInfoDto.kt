package io.snabble.sdk.ui.payment.creditcard.fiserv.data.dto

import com.google.gson.annotations.SerializedName

internal data class CustomerInfoDto(
    @SerializedName("name") val name: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("address") val address: AddressDto,
)

internal data class AddressDto(
    @SerializedName("street") val street: String,
    @SerializedName("zip") val zip: String,
    @SerializedName("city") val city: String,
    @SerializedName("state") val state: String?,
    @SerializedName("country") val country: String
)
