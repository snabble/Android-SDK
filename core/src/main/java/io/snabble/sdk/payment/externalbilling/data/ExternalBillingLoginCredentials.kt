package io.snabble.sdk.payment.externalbilling.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExternalBillingLoginCredentials(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
)
