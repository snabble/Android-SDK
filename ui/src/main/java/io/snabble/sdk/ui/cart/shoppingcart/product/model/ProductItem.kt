package io.snabble.sdk.ui.cart.shoppingcart.product.model

import io.snabble.sdk.Unit
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.CartItem

data class ProductItem(
    override val item: ShoppingCart.Item,
    val discounts: List<DiscountItem> = mutableListOf(),
    val deposit: DepositItem? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val encodingUnit: Unit? = null,
    val priceText: String? = null,
    val listPrice: String? = null,
    val quantityText: String? = null,
    val quantity: Int = 0,
    val editable: Boolean = false,
    val manualDiscountApplied: Boolean = false,
    val totalPrice: String? = null
) : CartItem {

    fun totalPrice(): Int {
        val discountPrice = discounts.sumOf { it.discountValue }
        return item.totalPrice + (deposit?.depositPrice ?: 0) + discountPrice
    }
}
