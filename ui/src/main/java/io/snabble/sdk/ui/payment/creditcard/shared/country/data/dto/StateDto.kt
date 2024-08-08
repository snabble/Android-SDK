package io.snabble.sdk.ui.payment.creditcard.shared.country.data.dto

import com.google.gson.annotations.SerializedName

internal class StateDto(@SerializedName("name") val displayName: String, @SerializedName("code") val code: String)
