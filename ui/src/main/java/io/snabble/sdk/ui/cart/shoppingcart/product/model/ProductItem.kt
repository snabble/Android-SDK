package io.snabble.sdk.ui.cart.shoppingcart.product.model

import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.CartItem

data class ProductItem(
    override val item: ShoppingCart.Item,
    val imageUrl: String? = null,
    val showPlaceHolder: Boolean = false,
    val name: String? = null,
    val discounts: List<DiscountItem> = mutableListOf(),
    val deposit: DepositItem? = null,
    val discountPrice: String? = null,
    val priceText: String? = null,
    val totalPrice: String? = null,
    val isAgeRestricted: Boolean = false,
    val manualDiscountApplied: Boolean = false,
    val editable: Boolean = false,
    val quantityText: String,
    val unit: String,
    val quantity: Int = 0,
    val minimumAge: Int = 0
) : CartItem {

    fun getTotalPrice(): Int {
        return item.totalPrice + (deposit?.depositPrice ?: 0)
    }

    fun getDiscountPrice(): Int {
        val discountPrice = discounts.sumOf { it.discountValue }
        return item.totalPrice + discountPrice
    }
}
