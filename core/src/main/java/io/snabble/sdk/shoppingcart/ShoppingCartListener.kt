package io.snabble.sdk.shoppingcart

import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ShoppingCart.Item
import io.snabble.sdk.ViolationNotification

/**
 * Shopping list listener that detects various changes to the shopping list.
 */
interface ShoppingCartListener {

    fun onItemAdded(list: ShoppingCart?, item: Item?){}
    fun onQuantityChanged(list: ShoppingCart?, item: Item?)
    fun onCleared(list: ShoppingCart?)
    fun onItemRemoved(list: ShoppingCart?, item: Item?, pos: Int)
    fun onProductsUpdated(list: ShoppingCart?)
    fun onPricesUpdated(list: ShoppingCart?)
    fun onCheckoutLimitReached(list: ShoppingCart?)
    fun onOnlinePaymentLimitReached(list: ShoppingCart?)
    fun onTaxationChanged(list: ShoppingCart?, taxation: Taxation?)
    fun onViolationDetected(violations: List<ViolationNotification?>)
    fun onCartDataChanged(list: ShoppingCart?)
}


interface ShoppingCartListiner2{

    fun onChanged(event: Event)
}

sealed interface Event
data class ItemAdded(
    val list: ShoppingCart?,
    val item: Item?
):Event
