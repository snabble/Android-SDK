package io.snabble.sdk.ui.scanner


import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI

class SelectReducedPriceDialogFragment(
    private val productConfirmationDialog: ProductConfirmationDialog,
    private val cartItem: ShoppingCart.Item
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val project = SnabbleUI.getProject()
        val discounts = project.manualCoupons

        val adapter = ArrayAdapter(requireContext(),
            R.layout.snabble_item_pricereduction_select,
            R.id.label,
            listOf(getString(R.string.Snabble_noDiscount)) + (discounts.map { it.name })
        )

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.Snabble_addDiscount)
            .setAdapter(adapter) { _, which ->
                if (which == 0) {
                    cartItem.manualCoupon = null
                } else {
                    cartItem.manualCoupon = discounts[which - 1]
                }
                productConfirmationDialog.updatePrice()
            }
            .create()
    }
}