package io.snabble.sdk.payment.data

import com.google.gson.annotations.SerializedName

@Suppress(
    "LongParameterList",
    "unused", // Fields are used for serialization
)
internal class PayoneData internal constructor(
    @SerializedName("pseudoCardPAN") private val pseudoCardPan: String,
    @SerializedName("name") private val name: String,
    @SerializedName("email") private val email: String,
    street: String,
    zip: String,
    city: String,
    country: String,
    state: String?,
    @SerializedName("userID") private val userId: String?
) {

    @SerializedName("address")
    private val address: Address

    init {
        address = Address(street, zip, city, country, state)
    }

    private class Address(
        @SerializedName("street") val street: String,
        @SerializedName("zip") val zip: String,
        @SerializedName("city") val city: String,
        @SerializedName("country") val country: String,
        @SerializedName("state") val state: String?
    )
}
