package io.snabble.sdk.checkout

import android.util.Log
import io.snabble.sdk.Coupon
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Product
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import java.io.File
import java.lang.Exception

data class PersistentState (
    var file: File,
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

        Log.d("CheckoutPersistence", "trywrite [${file.parent}]: $json")

        Dispatch.io {
            try {
                Log.d("CheckoutPersistence", "write [${file.parent}]: $json")
                file.writeText(json)
            } catch (e: Exception) {
                Log.d("CheckoutPersistence", "write exception [${file.parent}]: $e")
            }
        }
    }

    companion object {
        fun restore(file: File): PersistentState {
            Log.d("CheckoutPersistence", "restore [${file.parent}]")

            return try {
                val text = file.readText()
                Log.d("CheckoutPersistence", "read [${file.parent}]: $text")
                val persistentState = GsonHolder.get().fromJson(text, PersistentState::class.java)
                persistentState.file = file
                persistentState
            } catch (e: Exception) {
                Log.d("CheckoutPersistence", "read exception [${file.parent}]: $e")
                PersistentState(file)
            }
        }
    }
}