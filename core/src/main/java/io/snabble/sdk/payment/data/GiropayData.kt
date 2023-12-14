package io.snabble.sdk.payment.data

data class GiropayData(
    @JvmField val clientID: String?,
    @JvmField val customerAuthorizationURI: String,
    @JvmField val authorizationData: GiropayAuthorizationData? = null,
)

