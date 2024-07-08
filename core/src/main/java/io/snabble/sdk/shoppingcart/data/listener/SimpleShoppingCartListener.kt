package io.snabble.sdk.shoppingcart.data.listener

import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.Taxation

abstract class SimpleShoppingCartListener : ShoppingCartListener {

    abstract fun onChanged(cart: ShoppingCart)

    override fun onProductsUpdated(cart: ShoppingCart) = onChanged(cart)

    override fun onItemAdded(cart: ShoppingCart, item: ShoppingCart.Item) = onChanged(cart)

    override fun onQuantityChanged(cart: ShoppingCart, item: ShoppingCart.Item) = onChanged(cart)

    override fun onCleared(cart: ShoppingCart) = onChanged(cart)

    override fun onItemRemoved(cart: ShoppingCart, item: ShoppingCart.Item, pos: Int) = onChanged(cart)

    override fun onPricesUpdated(cart: ShoppingCart) = onChanged(cart)

    override fun onTaxationChanged(cart: ShoppingCart, taxation: Taxation) = onChanged(cart)

    override fun onCheckoutLimitReached(cart: ShoppingCart) {}

    override fun onOnlinePaymentLimitReached(cart: ShoppingCart) {}

    override fun onViolationDetected(violations: List<ViolationNotification>) {}

    override fun onCartDataChanged(cart: ShoppingCart) = onChanged(cart)
}
