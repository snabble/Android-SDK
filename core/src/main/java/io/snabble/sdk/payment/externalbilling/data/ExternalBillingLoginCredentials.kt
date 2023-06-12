package io.snabble.sdk.payment.externalbilling.data

import com.google.gson.annotations.SerializedName

data class ExternalBillingLoginCredentials(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
)
