package io.snabble.sdk.ui.cart.adapter.models

import android.content.Context
import androidx.annotation.DrawableRes
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.emptyToNull

data class SimpleRow(
    override val item: ShoppingCart.Item? = null,
    override val isDismissible: Boolean = false,
    @JvmField val text: String? = null,
    @JvmField val title: String? = null,
    @JvmField @DrawableRes val imageResId: Int? = null
) : Row

internal fun ShoppingCart.Item.simpleRowFromLineItem(context: Context): SimpleRow? =
    if (isDiscount) {
        SimpleRow(
            item = this,
            text = priceText.emptyToNull(),
            title = context.getString(R.string.Snabble_Shoppingcart_discounts),
            imageResId = R.drawable.snabble_ic_percent
        )
    } else if (isGiveaway) {
        SimpleRow(
            item = this,
            text = context.getString(R.string.Snabble_Shoppingcart_giveaway),
            title = displayName,
            imageResId = R.drawable.snabble_ic_gift
        )
    } else {
        null
    }

internal fun ShoppingCart.depositItem(context: Context): SimpleRow? =
    if (totalDepositPrice > 0) {
        val priceFormatter = Snabble.instance.checkedInProject.getValue()?.priceFormatter
        SimpleRow(
            text = priceFormatter?.format(totalDepositPrice),
            title = context.getString(R.string.Snabble_Shoppingcart_deposit),
            imageResId = R.drawable.snabble_ic_deposit
        )
    } else {
        null
    }
