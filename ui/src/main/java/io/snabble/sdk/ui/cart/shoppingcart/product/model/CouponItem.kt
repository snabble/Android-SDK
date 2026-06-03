package io.snabble.sdk.ui.cart.shoppingcart.product.model

import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.CartItem

internal data class CouponItem (
    override val item: ShoppingCart.Item,
    val couponId: String?,
    val name: String? = null,
    val areRequirementsMet: Boolean = false,
) : CartItem
