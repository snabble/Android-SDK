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
        val discounts = project.manualDiscounts

        val adapter = ArrayAdapter(requireContext(),
            R.layout.snabble_item_pricereduction_select,
            R.id.label,
            discounts.map { it.label }
        )

        return AlertDialog.Builder(requireContext())
            .setTitle("Select price reduction")
            .setAdapter(adapter) { _, which ->
                cartItem.setManualDiscount(discounts[which])
                productConfirmationDialog.updatePrice()
            }
            .create()
    }
}