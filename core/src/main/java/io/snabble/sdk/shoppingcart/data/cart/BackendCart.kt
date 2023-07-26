package io.snabble.sdk.shoppingcart.data.cart

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.events.data.EventType
import io.snabble.sdk.events.data.payload.Payload

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class BackendCart(
    val session: String? = null,
    @SerializedName("shopID") val shopId: String? = null,
    @SerializedName("clientID") val clientId: String? = null,
    @SerializedName("appUserID") val appUserId: String? = null,
    val customer: BackendCartCustomer? = null,
    val items: List<ShoppingCart.BackendCartItem>,
    val requiredInformation: MutableList<BackendCartRequiredInformation>? = null,
    override val eventType: EventType = EventType.CART
) : Payload
