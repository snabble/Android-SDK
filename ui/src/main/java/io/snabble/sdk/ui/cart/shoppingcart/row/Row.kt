package io.snabble.sdk.ui.cart.shoppingcart.row

import androidx.annotation.DrawableRes
import io.snabble.sdk.PriceFormatter
import io.snabble.sdk.Unit
import io.snabble.sdk.shoppingcart.ShoppingCart

interface Row {

    val item: ShoppingCart.Item?
    val isDismissible: Boolean
}

data class SimpleRow(
    override val item: ShoppingCart.Item? = null,
    override val isDismissible: Boolean = false,
    val title: String? = null,
    val discount: String? = null,
    val name: String? = null,
    @DrawableRes val imageResId: Int = 0
) : Row

data class ProductRow(
    override val item: ShoppingCart.Item?,
    override val isDismissible: Boolean,
    val discounts: List<Discount> = mutableListOf(),
    val name: String? = null,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val encodingUnit: Unit? = null,
    val priceText: String? = null,
    val depositPrice: Int? = null,
    val depositPriceText: String? = null,
    val depositText: String? = null,
    val quantityText: String? = null,
    val quantity: Int = 0,
    val editable: Boolean = false,
    val manualDiscountApplied: Boolean = false,
) : Row {

    fun totalPrice(formatter: PriceFormatter): String? {
        val discountPrice = discounts.sumOf { it.discountValue }
        item?.totalPrice ?: return null
        return formatter.format(item.totalPrice + (depositPrice ?: 0) + discountPrice)
    }
}

data class Discount(
    val name: String,
    val discount: String,
    val discountValue: Int
)
