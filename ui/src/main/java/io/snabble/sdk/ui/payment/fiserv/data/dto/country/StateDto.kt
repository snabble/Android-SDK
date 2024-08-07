package io.snabble.sdk.ui.payment.fiserv.data.dto.country

import com.google.gson.annotations.SerializedName

internal class StateDto(@SerializedName("name") val displayName: String, @SerializedName("code") val code: String)
