package io.snabble.sdk.ui.cart.shoppingcart.product.model

internal data class DiscountItem(
    val id: String?,
    val name: String,
    val discount: String,
    val discountValue: Int,
    val isCoupon: Boolean = false,
    val useNegativeValue: Boolean = false
)
