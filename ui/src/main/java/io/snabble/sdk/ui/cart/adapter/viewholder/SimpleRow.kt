package io.snabble.sdk.ui.cart.adapter.viewholder

import androidx.annotation.DrawableRes
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ui.cart.adapter.Row

data class SimpleRow(
    override val item: ShoppingCart.Item? = null,
    override val isDismissible: Boolean = false,
    @JvmField val text: String? = null,
    @JvmField val title: String? = null,
    @JvmField @DrawableRes val imageResId: Int? = null
) : Row
