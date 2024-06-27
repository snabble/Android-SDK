package io.snabble.sdk.ui.cart.shoppingcart.giveaway.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.CartItem

data class GiveAwayItem(
    override val item: ShoppingCart.Item,
    val title: String,
    @DrawableRes val imageResId: Int = R.drawable.snabble_ic_gift,
    @StringRes val name: Int = R.string.Snabble_Shoppingcart_giveaway
) : CartItem
