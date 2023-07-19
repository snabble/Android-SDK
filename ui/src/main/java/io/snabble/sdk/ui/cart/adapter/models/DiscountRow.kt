package io.snabble.sdk.ui.cart.adapter.models

import io.snabble.sdk.ShoppingCart

data class DiscountRow(
    override val item: ShoppingCart.Item,
    override val isDismissible: Boolean = true,
    val text: String?,
    val title: String?,
) : Row

internal fun ShoppingCart.Item.couponRow(): DiscountRow = DiscountRow(
    item = this,
    text = priceText,
    title = displayName
)
