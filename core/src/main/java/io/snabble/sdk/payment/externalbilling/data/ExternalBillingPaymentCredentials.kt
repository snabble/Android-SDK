package io.snabble.sdk.payment.externalbilling.data

import com.google.gson.annotations.SerializedName

data class ExternalBillingPaymentCredentials(
    @SerializedName("username") val username: String,
    @SerializedName("contactPersonID") val contactPersonID: String,
    @SerializedName("password") val password: String,
)
