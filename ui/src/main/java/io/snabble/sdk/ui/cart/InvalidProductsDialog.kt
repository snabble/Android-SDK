@file:JvmName("InvalidProductsHelper")

package io.snabble.sdk.ui.cart

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AlertDialog
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.ui.R

fun Context.showInvalidProductsDialog(
    invalidItems: List<ShoppingCart.Item>,
    onRemove: () -> Unit
) {
    AlertDialog.Builder(this)
        .setCancelable(false)
        .setTitle(getString(R.string.Snabble_ShoppingCart_Product_Invalid_title))
        .setMessage(resources.createInvalidItemsMessage(invalidItems))
        .setPositiveButton(R.string.Snabble_ShoppingCart_Product_Invalid_button) { dialog, _ ->
            onRemove()
            dialog.dismiss()
        }
        .show()
}

private fun Resources.createInvalidItemsMessage(
    items: List<ShoppingCart.Item>
): String {
    val invalidItems = items.joinToString(separator = "\n") { item ->
        if (item.type == ItemType.DEPOSIT_RETURN_VOUCHER) {
            getString(R.string.Snabble_ShoppingCart_DepositReturn_title)
        } else {
            getString(R.string.Snabble_ShoppingCart_product, item.displayName)
        }
    }

    return getQuantityString(
        R.plurals.Snabble_ShoppingCart_Product_Invalid_message,
        items.size,
        invalidItems
    )
}
