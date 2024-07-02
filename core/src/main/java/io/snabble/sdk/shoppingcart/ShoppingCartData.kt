package io.snabble.sdk.shoppingcart

import io.snabble.sdk.Product
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.utils.GsonHolder
import java.util.*

data class ShoppingCartData @JvmOverloads constructor(
    @JvmField val id: String = UUID.randomUUID().toString(),
    @JvmField val uuid: String = UUID.randomUUID().toString(),
    @JvmField val lastModificationTime: Long = 0,
    @JvmSuppressWildcards @JvmField val items: MutableList<ShoppingCart.Item> = mutableListOf(),
    @JvmField val violationNotifications: MutableList<ViolationNotification> = mutableListOf(),
    @JvmField val modCount: Int = 0,
    @JvmField val addCount: Int = 0,
    @JvmField val onlineTotalPrice: Int? = null,
    @JvmSuppressWildcards @JvmField val invalidProducts: List<Product>? = null,
    @JvmField val taxation: Taxation = Taxation.UNDECIDED,
    @JvmField val hasRaisedMaxCheckoutLimit: Boolean = false,
    @JvmField val hasRaisedMaxOnlinePaymentLimit: Boolean = false,
    @JvmField val invalidDepositReturnVoucher: Boolean = false,
    @JvmField val backupTimestamp: Long = 0,
) {
    fun applyShoppingCart(shoppingCart: ShoppingCart) {
        items.forEach {
            it.cart = shoppingCart
        }
    }

    fun deepCopy() : ShoppingCartData {
        val json = GsonHolder.get().toJson(this)
        return GsonHolder.get().fromJson(json, ShoppingCartData::class.java)
    }
}
