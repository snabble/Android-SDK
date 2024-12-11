package io.snabble.sdk.ui.cart.shoppingcart.product.model

import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.CartItem

data class DepositReturnItem(
    override val item: ShoppingCart.Item? = null,
    val totalDeposit: String
) : CartItem
