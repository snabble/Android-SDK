package io.snabble.sdk.ui.cart

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI

open class ShoppingCartFragment : BaseFragment(R.layout.snabble_fragment_shoppingcart) {
    var shoppingCartView: ShoppingCartView? = null
        private set

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        shoppingCartView = view.findViewById(R.id.shopping_cart_view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.snabble_menu_shopping_cart, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete) {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.Snabble_Shoppingcart_removeItems)
                .setPositiveButton(R.string.Snabble_Yes) { dialog, which ->
                    SnabbleUI.project.shoppingCart.clearBackup()
                    SnabbleUI.project.shoppingCart.clear()
                    onCartCleared()
                }
                .setNegativeButton(R.string.Snabble_No, null)
                .create()
                .show()
            return true
        }
        return false
    }

    open fun onCartCleared() {}
}