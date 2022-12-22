package io.snabble.sdk.customization

import io.snabble.sdk.ShoppingCart

/**
 * Implement the interface and set [Snabble.isMergeable] to make use of the
 * custom ShoppingCart.Item.isMergeable implementation
 */
fun interface IsMergeable {

    fun isMergeable(item: ShoppingCart.Item, isMergeable: Boolean): Boolean
}
