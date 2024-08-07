package io.snabble.sdk.ui.payment.datatrans.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CustomerInfoDto(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("address") val address: AddressDto,
    @SerialName("phoneNumber") val phoneNumber: PhoneNumberDto,
)

@Serializable
internal data class AddressDto(
    @SerialName("street") val street: String,
    @SerialName("zip") val zip: String,
    @SerialName("city") val city: String,
    @SerialName("state") val state: String?,
    @SerialName("country") val country: String,
)

@Serializable
internal data class PhoneNumberDto(
    @SerialName("subscriber") val number: String,
    @SerialName("countryCode") val intCallingCode: String,
)
