package io.snabble.sdk.checkout

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Product
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.events.Events
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import java.io.File

data class PersistentState(
    @Transient
    var file: File,
    var cartId: String? = null,
    var checkoutProcess: CheckoutProcessResponse? = null,
    var selectedPaymentMethod: PaymentMethod? = null,
    var priceToPay: Int = 0,
    var codes: List<String> = mutableListOf(),
    var invalidProducts: List<Product>? = null,
    var redeemedCoupons: List<Coupon> = emptyList(),
    var state: CheckoutState = CheckoutState.NONE,
    var fulfillmentState: List<Fulfillment>? = null,
    var signedCheckoutInfo: SignedCheckoutInfo? = null
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
