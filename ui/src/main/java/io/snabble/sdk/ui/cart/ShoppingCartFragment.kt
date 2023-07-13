package io.snabble.sdk.ui.cart

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Deprecated(
    message = "The solo shopping cart view is end-of-life.",
    ReplaceWith(
        "Use the CombinedScannerFragment instead.",
        "io.snabble.sdk.ui.scanner.CombinedScannerFragment"
    )
)
open class ShoppingCartFragment : BaseFragment(R.layout.snabble_fragment_shoppingcart), MenuProvider {

    var shoppingCartView: ShoppingCartView? = null
        private set

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)

        shoppingCartView = view.findViewById(R.id.shopping_cart_view)

        setupMenu()
    }

    private fun setupMenu() {
        (activity as? MenuHost)?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.snabble_menu_shopping_cart, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            R.id.action_delete -> {
                startCartDeletionFlow()
                true
            }

            else -> false
        }

    private fun startCartDeletionFlow() {
        lifecycleScope.launch {
            val shouldDelete = askForDeletion()
            if (shouldDelete) {
                val project: Project = Snabble.checkedInProject.value ?: return@launch
                with(project.shoppingCart) {
                    clearBackup()
                    clear()
                }
                onCartCleared()
            }
        }
    }

    private suspend fun askForDeletion(): Boolean = suspendCancellableCoroutine { continuation ->
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.Snabble_Shoppingcart_removeItems)
            .setPositiveButton(R.string.Snabble_yes) { _, _ ->
                continuation.resume(true)
            }
            .setNegativeButton(R.string.Snabble_no) { _, _ ->
                continuation.resume(false)
            }
            .setOnDismissListener { continuation.cancel() }
            .create()
            .show()
    }

    open fun onCartCleared() {}
}
