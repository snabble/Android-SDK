package io.snabble.sdk.ui.cart

import android.os.Bundle
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.UIUtils
import android.content.res.ColorStateList
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.SnabbleUI

open class ShoppingCartFragment : Fragment() {
    var shoppingCartView: ShoppingCartView? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.snabble_fragment_shoppingcart, container, false)
        shoppingCartView = v.findViewById(R.id.shopping_cart_view)
        return v
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