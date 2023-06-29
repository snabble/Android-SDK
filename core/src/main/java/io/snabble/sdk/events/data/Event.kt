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

/**
 * Checks if the Event is an analytics event for rating with a shopId.
 * If the shopId is missing false is returned, true otherwise.
 */
internal fun Event.isRatingAndValid(): Boolean =
    type == EventType.ANALYTICS &&
            payload?.asJsonObject?.get("key")?.asString == "rating" &&
            shopId != null
