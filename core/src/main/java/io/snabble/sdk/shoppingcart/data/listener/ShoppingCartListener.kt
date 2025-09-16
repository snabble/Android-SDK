package io.snabble.sdk.shoppingcart.data.listener

import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.data.Taxation

/**
 * Shopping list listener that detects various changes to the shopping list.
 */
interface ShoppingCartListener {

    fun onItemAdded(cart: ShoppingCart, item: ShoppingCart.Item)
    fun onQuantityChanged(cart: ShoppingCart, item: ShoppingCart.Item)
    fun onCleared(cart: ShoppingCart)
    fun onItemRemoved(cart: ShoppingCart, item: ShoppingCart.Item, pos: Int)
    fun onProductsUpdated(cart: ShoppingCart)
    fun onPricesUpdated(cart: ShoppingCart)
    fun onOnlinePricesUpdated(cart: ShoppingCart)
    fun onCheckoutLimitReached(cart: ShoppingCart)
    fun onOnlinePaymentLimitReached(cart: ShoppingCart)
    fun onTaxationChanged(cart: ShoppingCart, taxation: Taxation)
    fun onViolationDetected(violations: List<ViolationNotification>)
    fun onCartDataChanged(cart: ShoppingCart)
}
