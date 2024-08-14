package io.snabble.sdk.ui.cart.shoppingcart.product.model

import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.CartItem
import io.snabble.sdk.ui.cart.shoppingcart.convertPriceModifier
import kotlin.math.abs

internal data class ProductItem(
    override val item: ShoppingCart.Item,
    val imageUrl: String? = null,
    val showPlaceHolder: Boolean = false,
    val name: String? = null,
    val discounts: List<DiscountItem> = mutableListOf(),
    val deposit: DepositItem? = null,
    val discountedPrice: String? = null,
    // Displays the price without any deposits, etc. applied
    val priceText: String? = null,
    // Displays the total price with deposits, etc. applied
    val totalPriceText: String? = null,
    val totalPrice: Int = 0,
    val isAgeRestricted: Boolean = false,
    val isManualDiscountApplied: Boolean = false,
    val editable: Boolean = false,
    val quantityText: String,
    val unit: String,
    val quantity: Int = 0,
    val minimumAge: Int = 0
) : CartItem {

    fun calculateTotalPrice(): Int {
        // If price modifiers are present the total price is already adjusted. Thus we need to re-add the
        // price modifiers on top of the total price to get the unmodified total price.
        // In the other case we can directly work with the total price.
        val depositPrice = deposit?.depositPrice ?: 0
        val weightUnit = item.lineItem?.weightUnit
        val referenceUnit = item.lineItem?.referenceUnit
        val sumOfModifierPriceDiscounts = if (weightUnit != null && referenceUnit != null) {
            item.lineItem?.priceModifiers.orEmpty()
                .sumOf { it.convertPriceModifier(quantity, weightUnit, referenceUnit) }
                .let(::abs)
        } else {
            item.lineItem?.priceModifiers.orEmpty()
                .sumOf { it.price * quantity }
                .let(::abs)
        }
        return totalPrice + depositPrice + sumOfModifierPriceDiscounts
    }

    fun getPriceWithDiscountsApplied(): Int {
        val discountPrice = discounts.sumOf { it.discountValue }
        return (totalPrice + (deposit?.depositPrice ?: 0)) + discountPrice
    }
}
