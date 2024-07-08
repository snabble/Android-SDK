package io.snabble.sdk.ui.cart.shoppingcart.product.model

import io.snabble.sdk.Product
import io.snabble.sdk.extensions.xx
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
    val discountPrice: String? = null,
    val priceText: String? = null,
    val listPrice: Int = 0,
    val totalPrice: String? = null,
    val finalPrice: Int = 0,
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
                finalPrice - totalModifiedPrices.intValueExact()
            }

            else -> {
                if (item.product?.type == Product.Type.UserWeighed) {
                    finalPrice
                } else {
                    (listPrice * quantity) + (deposit?.depositPrice ?: 0)
                }
            }
        }
    }

    fun getDiscountedPrice(): Int {
        val priceModifiers = item.lineItem?.priceModifiers
        val discountPrice = discounts.sumOf { it.discountValue }
        return when {

            !priceModifiers.isNullOrEmpty() -> {
                (finalPrice + (deposit?.depositPrice ?: 0)) + discountPrice
            }

            else -> {
                (listPrice * quantity) + (deposit?.depositPrice ?: 0) + discountPrice
            }
        }
    }
}
