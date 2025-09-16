package io.snabble.sdk.shoppingcart.data.listener

import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.Taxation

interface DefaultShoppingCartListener : ShoppingCartListener {

    override fun onItemAdded(cart: ShoppingCart, item: ShoppingCart.Item) {
    }

    override fun onQuantityChanged(cart: ShoppingCart, item: ShoppingCart.Item) {
    }

    override fun onCleared(cart: ShoppingCart) {
    }

    override fun onItemRemoved(cart: ShoppingCart, item: ShoppingCart.Item, pos: Int) {
    }

    override fun onProductsUpdated(cart: ShoppingCart) {
    }

    override fun onPricesUpdated(cart: ShoppingCart) {
    }

    override fun onOnlinePricesUpdated(cart: ShoppingCart) {}

    override fun onCheckoutLimitReached(cart: ShoppingCart) {
    }

    override fun onOnlinePaymentLimitReached(cart: ShoppingCart) {
    }

    override fun onTaxationChanged(cart: ShoppingCart, taxation: Taxation) {
    }

    override fun onViolationDetected(violations: List<ViolationNotification>) {
    }

    override fun onCartDataChanged(cart: ShoppingCart) {
    }
}
