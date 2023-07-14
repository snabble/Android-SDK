package io.snabble.sdk.shoppingcart

import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ShoppingCart.Item
import io.snabble.sdk.ViolationNotification

abstract class SimpleShoppingCartListener : ShoppingCartListener {

    abstract fun onChanged(list: ShoppingCart?)
    override fun onProductsUpdated(list: ShoppingCart?) = onChanged(list)
    override fun onItemAdded(list: ShoppingCart?, item: Item?) = onChanged(list)
    override fun onQuantityChanged(list: ShoppingCart?, item: Item?) = onChanged(list)
    override fun onCleared(list: ShoppingCart?) = onChanged(list)
    override fun onItemRemoved(list: ShoppingCart?, item: Item?, pos: Int) = onChanged(list)
    override fun onPricesUpdated(list: ShoppingCart?) = onChanged(list)
    override fun onTaxationChanged(list: ShoppingCart?, taxation: Taxation?) = onChanged(list)
    override fun onCheckoutLimitReached(list: ShoppingCart?) {
        // Do nothing
    }

    override fun onOnlinePaymentLimitReached(list: ShoppingCart?) {
        // Do nothing
    }

    override fun onViolationDetected(violations: List<ViolationNotification?>) {
        // Do nothing
    }

    override fun onCartDataChanged(list: ShoppingCart?) = onChanged(list)
}
