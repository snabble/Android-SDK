package io.snabble.sdk.payment.data

import com.google.gson.annotations.SerializedName

data class GiropayData(
    @JvmField @SerializedName("clientID") val clientID: String?,
    @JvmField @SerializedName("customerAuthorizationURI") val customerAuthorizationURI: String,
    @JvmField @SerializedName("authorizationData") val authorizationData: GiropayAuthorizationData? = null,
)
