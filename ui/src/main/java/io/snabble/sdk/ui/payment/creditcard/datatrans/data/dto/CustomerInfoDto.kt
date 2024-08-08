package io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto

import com.google.gson.annotations.SerializedName

internal data class CustomerInfoDto(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("address") val address: AddressDto,
    @SerializedName("phoneNumber") val phoneNumber: PhoneNumberDto,
)

internal data class AddressDto(
    @SerializedName("street") val street: String,
    @SerializedName("zip") val zip: String,
    @SerializedName("city") val city: String,
    @SerializedName("state") val state: String?,
    @SerializedName("country") val country: String,
)

internal data class PhoneNumberDto(
    @SerializedName("subscriber") val number: String,
    @SerializedName("countryCode") val intCallingCode: String,
)
