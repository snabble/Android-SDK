package io.snabble.sdk.ui.payment.creditcard.datatrans.data.dto

import com.google.gson.annotations.SerializedName

internal data class AuthDataDto(
    @SerializedName("mobileToken") val mobileToken: String,
    @SerializedName("isTesting") val isTesting: Boolean = false,
)
