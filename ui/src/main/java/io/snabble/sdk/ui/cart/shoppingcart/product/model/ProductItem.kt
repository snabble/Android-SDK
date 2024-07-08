package io.snabble.sdk.ui.cart.shoppingcart.product.model

import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.CartItem
import io.snabble.sdk.ui.cart.shoppingcart.convertPriceModifier

internal data class ProductItem(
    override val item: ShoppingCart.Item,
    val imageUrl: String? = null,
    val showPlaceHolder: Boolean = false,
    val name: String? = null,
    val discounts: List<DiscountItem> = mutableListOf(),
    val deposit: DepositItem? = null,
    val discountedPrice: String? = null,
    //Displays the price without any deposits, etc. applied
    val priceText: String? = null,
    //Displays the total price with deposits, etc. applied
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

    fun getTotalPrice(): Int {
        val priceModifiers = item.lineItem?.priceModifiers
        return when {
            !priceModifiers.isNullOrEmpty() -> {
                val totalModifiedPrices = priceModifiers.sumOf {
                    it.convertPriceModifier(quantity, unit, item.lineItem?.referenceUnit)
                }
                totalPrice - totalModifiedPrices.intValueExact()+ (deposit?.depositPrice ?: 0)
            }

            else -> {
                totalPrice + (deposit?.depositPrice ?: 0)
            }
        }
    }

    fun getPriceWithDiscountsApplied(): Int {
        val discountPrice = discounts.sumOf { it.discountValue }
        return (totalPrice + (deposit?.depositPrice ?: 0)) + discountPrice
    }
}
