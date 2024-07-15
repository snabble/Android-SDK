package io.snabble.sdk.ui.cart.deprecated

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

@Deprecated("Integrate [io.snabble.sdk.ui.cart.shoppingcart.ShoppingCartScreen] instead.")
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
                .setPositiveButton(R.string.Snabble_yes) { _, _ ->
                    requireNotNull(Snabble.checkedInProject.value).shoppingCart.clearBackup()
                    requireNotNull(Snabble.checkedInProject.value).shoppingCart.clear()
                    onCartCleared()
                }
                .setNegativeButton(R.string.Snabble_no, null)
                .create()
                .show()
            return true
        }
        return false
    }

    open fun onCartCleared() {}
}
