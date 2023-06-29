package io.snabble.sdk.payment.externalbilling.data

import com.google.gson.annotations.SerializedName

data class ExternalBillingLoginResponse(
    @SerializedName("username") val username: String,
    @SerializedName("contactPersonID") val contactPersonID: String,
)
