package io.snabble.sdk.ui.cart.adapter

import io.snabble.sdk.ShoppingCart

interface Row {

    val item: ShoppingCart.Item?
    val isDismissible: Boolean
}
