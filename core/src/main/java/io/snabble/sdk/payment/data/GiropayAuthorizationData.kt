package io.snabble.sdk.payment.data

data class GiropayAuthorizationData(
    @JvmField val id: String,
    @JvmField val name: String,
    @JvmField val ipAddress: String,
    @JvmField val fingerprint: String,
    @JvmField val redirectUrlAfterSuccess: String,
    @JvmField val redirectUrlAfterCancellation: String,
    @JvmField val redirectUrlAfterFailure: String
)
