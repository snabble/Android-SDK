package io.snabble.sdk.ui.payment.externalbilling.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillingCredentials(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
)

@Serializable
data class BillingCredentialsResponse(
    @SerialName("username") val username: String,
    @SerialName("contactPersonID") val password: String,
)
