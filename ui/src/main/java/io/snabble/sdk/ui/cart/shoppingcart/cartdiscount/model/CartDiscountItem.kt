package io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.CartItem

internal data class CartDiscountItem(
    override val item: ShoppingCart.Item,
    @param:StringRes val title: Int = R.string.Snabble_Shoppingcart_discounts,
    val discount: String,
    val name: String,
    @param:DrawableRes val imageResId: Int = R.drawable.snabble_ic_percent
) : CartItem
