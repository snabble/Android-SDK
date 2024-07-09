package io.snabble.sdk.shoppingcart.data.cart

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.events.data.EventType
import io.snabble.sdk.events.data.payload.Payload
import io.snabble.sdk.shoppingcart.data.item.BackendCartItem

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BackendCart(
    @SerializedName("appUserID") val appUserId: String? = null,
    @SerializedName("clientID") val clientId: String? = null,
    val customer: BackendCartCustomer? = null,
    override val eventType: EventType = EventType.CART,
    @JvmField val items: List<BackendCartItem>,
    val requiredInformation: List<BackendCartRequiredInformation>,
    val session: String,
    @SerializedName("shopID") val shopId: String
) : Payload
