package io.snabble.sdk.ui.payment.fiserv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CustomerInfoDto(
    @SerialName("name") val name: String,
    @SerialName("phoneNumber") val phoneNumber: String,
    @SerialName("email") val email: String,
    @SerialName("address") val address: AddressDto,
)

@Serializable
internal data class AddressDto(
    @SerialName("street") val street: String,
    @SerialName("zip") val zip: String,
    @SerialName("city") val city: String,
    @SerialName("state") val state: String?,
    @SerialName("country") val country: String
)