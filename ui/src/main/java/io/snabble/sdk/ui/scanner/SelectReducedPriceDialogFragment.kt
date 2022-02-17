package io.snabble.sdk.ui.scanner


import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.snabble.sdk.CouponType
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI

class SelectReducedPriceDialogFragment(
    private val productConfirmationDialog: ProductConfirmationDialog?,
    private val cartItem: ShoppingCart.Item?,
    private val shoppingCart: ShoppingCart?
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (productConfirmationDialog == null || cartItem == null) {
            dismissAllowingStateLoss()
        }

        val project = SnabbleUI.project
        val discounts = project.coupons.filter(CouponType.MANUAL)
        val dialog = productConfirmationDialog
        val item = cartItem
        val cart = shoppingCart

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.snabble_item_pricereduction_select,
            R.id.label,
            listOf(getString(R.string.Snabble_noDiscount)) + (discounts.map { it.name })
        )

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.Snabble_addDiscount)
            .setAdapter(adapter) { _, which ->
                if (which == 0) {
                    item?.coupon = null
                } else {
                    item?.coupon = discounts[which - 1]
                }

                val existingItem = cart?.getExistingMergeableProduct(item?.product)
                if (existingItem?.isMergeable == true) {
                    dialog?.setQuantity(1)
                }

                dialog?.updatePrice()
                dialog?.updateQuantityText()
            }
            .create()
    }
}