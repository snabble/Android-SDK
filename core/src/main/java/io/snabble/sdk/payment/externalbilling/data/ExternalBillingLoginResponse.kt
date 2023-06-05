package io.snabble.sdk.payment.externalbilling.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExternalBillingLoginResponse(
    @SerialName("username") val username: String,
    @SerialName("contactPersonID") val contactPersonID: String,
)
