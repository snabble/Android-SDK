package io.snabble.sdk.checkout

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Product
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.events.Events
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import java.io.File

@Keep
data class PersistentState(
    @Transient
    var file: File,
    @SerializedName("cartId") var cartId: String? = null,
    @SerializedName("checkoutProcess") var checkoutProcess: CheckoutProcessResponse? = null,
    @SerializedName("selectedPaymentMethod") var selectedPaymentMethod: PaymentMethod? = null,
    @SerializedName("priceToPay") var priceToPay: Int = 0,
    @SerializedName("codes") var codes: List<String> = mutableListOf(),
    @SerializedName("invalidProducts") var invalidProducts: List<Product>? = null,
    @SerializedName("invalidItems") var invalidItems: List<ShoppingCart.Item>? = null,
    @SerializedName("redeemedCoupons") var redeemedCoupons: List<Coupon> = emptyList(),
    @SerializedName("state") var state: CheckoutState = CheckoutState.NONE,
    @SerializedName("fulfillmentState") var fulfillmentState: List<Fulfillment>? = null,
    @SerializedName("signedCheckoutInfo") var signedCheckoutInfo: SignedCheckoutInfo? = null
) {

    fun save() {
        val json = GsonHolder.get().toJson(this)

        Dispatch.io {
            try {
                file.writeText(json)
            } catch (e: Exception) {
                Logger.d("write exception [${file.parent}]: $e")
            }
        }
    }

    companion object {

        fun restore(file: File, cartId: String, projectId: String): PersistentState = try {
            val persistentState = GsonHolder.get()
                .fromJson(file.readText(), PersistentState::class.java)
            if (persistentState.cartId == cartId) {
                persistentState.file = file
                persistentState
            } else {
                Events.logErrorEvent(projectId, "Tried to restore a check process w/o a matching cart id.")
                PersistentState(file, cartId = cartId)
            }
        } catch (e: Exception) {
            Logger.d("read exception [${file.parent}]: $e")
            PersistentState(file, cartId = cartId)
        }
    }
}
