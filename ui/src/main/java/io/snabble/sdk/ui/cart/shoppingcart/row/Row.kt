package io.snabble.sdk.ui.cart.shoppingcart.row

import io.snabble.sdk.shoppingcart.ShoppingCart

abstract class Row {

    var item: ShoppingCart.Item? = null
    var isDismissible: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val row = other as Row

        if (isDismissible != row.isDismissible) return false
        return if (item != null) item == row.item else row.item == null
    }

    override fun hashCode(): Int {
        var result = if (item != null) item.hashCode() else 0
        result = 31 * result + (if (isDismissible) 1 else 0)
        return result
    }
}
