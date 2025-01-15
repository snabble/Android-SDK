package io.snabble.sdk.shoppingcart

import io.snabble.sdk.Product
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.utils.GsonHolder
import java.util.UUID

data class ShoppingCartData @JvmOverloads constructor(
    val id: String = UUID.randomUUID().toString(),
    val uuid: String = UUID.randomUUID().toString(),
    val lastModificationTime: Long = 0,
    val items: MutableList<ShoppingCart.Item> = mutableListOf(),
    val violationNotifications: MutableList<ViolationNotification> = mutableListOf(),
    val modCount: Int = 0,
    val addCount: Int = 0,
    val onlineTotalPrice: Int? = null,
    val invalidProducts: List<Product>? = null,
    val invalidItemIds: List<String>? = null,
    val taxation: Taxation = Taxation.UNDECIDED,
    val hasReachedMaxCheckoutLimit: Boolean = false,
    val hasReachedMaxOnlinePaymentLimit: Boolean = false,
    val backupTimestamp: Long = 0,
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
