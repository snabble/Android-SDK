package io.snabble.sdk.ui.cart.shoppingcart.product.model

internal data class DiscountItem(
    val name: String,
    val discount: String,
    val discountValue: Int,
    val useNegativeValue: Boolean = false
)
