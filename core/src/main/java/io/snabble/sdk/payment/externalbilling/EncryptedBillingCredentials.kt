package io.snabble.sdk.payment.externalbilling

import com.google.gson.annotations.SerializedName

data class EncryptedBillingCredentials(
    @SerializedName("username") val username: String,
    @SerializedName("contactPersonID") val contactPersonID: String,
    @SerializedName("password") val password: String,
)
