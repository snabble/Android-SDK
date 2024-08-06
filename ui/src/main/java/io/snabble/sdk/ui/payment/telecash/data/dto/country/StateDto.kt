package io.snabble.sdk.ui.payment.telecash.data.dto.country

import com.google.gson.annotations.SerializedName

class StateDto(@SerializedName("name") val displayName: String, @SerializedName("code") val code: String)
