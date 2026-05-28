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
    @SerializedName("customer") val customer: BackendCartCustomer? = null,
    @SerializedName("eventType") override val eventType: EventType = EventType.CART,
    @JvmField @SerializedName("items") val items: List<BackendCartItem>,
    @SerializedName("requiredInformation") val requiredInformation: List<BackendCartRequiredInformation>,
    @SerializedName("session") val session: String,
    @SerializedName("shopID") val shopId: String
) : Payload
