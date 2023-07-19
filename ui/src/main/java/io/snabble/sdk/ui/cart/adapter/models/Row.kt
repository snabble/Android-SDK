package io.snabble.sdk.ui.cart.adapter.models

import io.snabble.sdk.ShoppingCart

interface Row {

    val item: ShoppingCart.Item?
    val isDismissible: Boolean
}
