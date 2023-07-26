package io.snabble.sdk.shoppingcart.data.listener

import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.data.Taxation

abstract class SimpleShoppingCartListener : ShoppingCartListener {

    abstract fun onChanged(list: ShoppingCart?)

    override fun onProductsUpdated(list: ShoppingCart?) = onChanged(list)

    override fun onItemAdded(list: ShoppingCart?, item: ShoppingCart.Item?) = onChanged(list)

    override fun onQuantityChanged(list: ShoppingCart?, item: ShoppingCart.Item?) = onChanged(list)

    override fun onCleared(list: ShoppingCart?) = onChanged(list)

    override fun onItemRemoved(list: ShoppingCart?, item: ShoppingCart.Item?, pos: Int) = onChanged(list)

    override fun onPricesUpdated(list: ShoppingCart?) = onChanged(list)

    override fun onTaxationChanged(list: ShoppingCart?, taxation: Taxation?) = onChanged(list)

    override fun onCheckoutLimitReached(list: ShoppingCart?) {}

    override fun onOnlinePaymentLimitReached(list: ShoppingCart?) {}

    override fun onViolationDetected(violations: List<ViolationNotification?>) {}

    override fun onCartDataChanged(list: ShoppingCart?) = onChanged(list)
}
