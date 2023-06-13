package io.snabble.sdk.events.data

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY)
enum class EventType {

    @SerializedName("sessionStart")
    SESSION_START,

    @SerializedName("sessionEnd")
    SESSION_END,

    @SerializedName("cart")
    CART,

    @SerializedName("error")
    ERROR,

    @SerializedName("log")
    LOG,

    @SerializedName("analytics")
    ANALYTICS,

    @SerializedName("productNotFound")
    PRODUCT_NOT_FOUND
}
