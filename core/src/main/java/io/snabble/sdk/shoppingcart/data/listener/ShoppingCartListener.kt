package io.snabble.sdk.shoppingcart.data.listener

import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.data.Taxation

/**
 * Shopping list listener that detects various changes to the shopping list.
 */
interface ShoppingCartListener {

    fun onItemAdded(list: ShoppingCart, item: ShoppingCart.Item)
    fun onQuantityChanged(list: ShoppingCart, item: ShoppingCart.Item)
    fun onCleared(list: ShoppingCart)
    fun onItemRemoved(list: ShoppingCart, item: ShoppingCart.Item, pos: Int)
    fun onProductsUpdated(list: ShoppingCart)
    fun onPricesUpdated(list: ShoppingCart)
    fun onCheckoutLimitReached(list: ShoppingCart)
    fun onOnlinePaymentLimitReached(list: ShoppingCart)
    fun onTaxationChanged(list: ShoppingCart, taxation: Taxation)
    fun onViolationDetected(violations: List<ViolationNotification>)
    fun onCartDataChanged(list: ShoppingCart)
}
