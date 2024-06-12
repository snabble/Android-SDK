package io.snabble.sdk.ui.cart.shoppingcart.row.new

import androidx.annotation.DrawableRes
import io.snabble.sdk.Unit
import io.snabble.sdk.shoppingcart.ShoppingCart

interface Row {

    val item: ShoppingCart.Item?
    val isDismissible: Boolean
}

data class SimpleRow(
    override val item: ShoppingCart.Item? = null,
    override val isDismissible: Boolean = false,
    val text: String? = null,
    val title: String? = null,
    @DrawableRes val imageResId: Int = 0
) : Row

data class ProductRow(
    override val item: ShoppingCart.Item?,
    override val isDismissible: Boolean,
    val name: String? = null,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val encodingUnit: Unit? = null,
    val priceText: String? = null,
    val quantityText: String? = null,
    val quantity: Int = 0,
    val editable: Boolean = false,
    val manualDiscountApplied: Boolean = false,

    ) : Row
