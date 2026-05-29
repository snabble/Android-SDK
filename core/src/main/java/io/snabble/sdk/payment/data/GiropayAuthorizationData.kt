package io.snabble.sdk.payment.data

import com.google.gson.annotations.SerializedName

data class GiropayAuthorizationData(
    @JvmField @SerializedName("id") val id: String,
    @JvmField @SerializedName("name") val name: String,
    @JvmField @SerializedName("ipAddress") val ipAddress: String,
    @JvmField @SerializedName("fingerprint") val fingerprint: String,
    @JvmField @SerializedName("redirectUrlAfterSuccess") val redirectUrlAfterSuccess: String,
    @JvmField @SerializedName("redirectUrlAfterCancellation") val redirectUrlAfterCancellation: String,
    @JvmField @SerializedName("redirectUrlAfterFailure") val redirectUrlAfterFailure: String
)
