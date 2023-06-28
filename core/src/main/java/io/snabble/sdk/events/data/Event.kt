package io.snabble.sdk.events.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

internal data class Event(
    val type: EventType? = null,
    val appId: String? = null,
    @SerializedName("shopID") val shopId: String? = null,
    val project: String? = null,
    val timestamp: String? = null,
    val payload: JsonElement? = null
)
